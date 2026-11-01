# How well can an LLM extract a cited Rule Card from a 200-page prospectus?

Research for issue #4 (wayfinder map #1). Researched 2026-10-03 against primary vendor docs. Context: ADR 0001 (`docs/adr/0001-llm-extracts-rules-engine-decides.md`). The LLM drafts a **Rule Card** (Hard Restrictions and Target Market criteria, each with a **Citation**) once per **Share Class**, a human validates it, and a deterministic engine decides.

## TL;DR

- **It's feasible, and the platform features fit this task well.** Claude takes a whole PDF natively (text plus a rendered image of every page, so tables and charts are visible) up to **600 pages / 32 MB per request** on 1M-context models. The **Citations** feature returns verbatim `cited_text` plus **1-indexed page numbers**, and the API guarantees those are valid pointers into the document. That is what we need for passage-level Citations.
- **The catch: Citations and structured outputs are mutually exclusive** (a request that sets both returns a 400). That forces a **two-pass pipeline**. Pass 1 extracts with Citations on. Pass 2 normalises the result into the Rule Card JSON schema with structured outputs and no documents attached.
- **Watch the context limit.** A dense 200-page prospectus sent as a native PDF can come to roughly **0.5M–1.5M tokens**, which may not fit in 1M. Run `count_tokens` first. Then either **scope the input** (general section plus the relevant sub-fund appendix) or fall back to text-only input.
- **Cost is a few dollars per prospectus** on Claude Opus 5.5 (about $1.50–$4 for the first Share Class). Each further Share Class from the same cached prospectus costs about **$0.50**. **Latency** is likely **2–6 minutes per document** (my estimate, not measured). Both are fine for a "once per Share Class, offline" step. Precompute Rule Cards before the demo.
- **The EMT probably needs no LLM.** The European MiFID Template is a standardised FinDatEx data file (spreadsheet/CSV) with Target Market fields. Parse it deterministically and save the LLM for the prospectus and KID.
- **Accuracy:** I found **no public benchmark for this exact task**. Expect strong results on explicitly stated restrictions. The risky cases are cross-references, defined terms, exceptions, umbrella vs sub-fund overrides, and share-class tables that span pages. The spike should hand-label a gold Rule Card for 2–3 prospectuses and measure against it (see [Evaluation](#evaluation-for-the-spike)).

## 1. Capabilities (Claude, primary docs)

### PDF input

- Any standard PDF (no passwords or encryption). Limits: **32 MB request size; 600 pages per request (100 when the context window is under 1M tokens)**. Both limits cover the whole request payload. Upload large PDFs with the Files API and reference them by `file_id`. [PDF support]
- How the PDF is processed: "The system converts each page of the document into an image. The text from each page is extracted and provided alongside each page's image." So Claude reasons over table layout and charts, not just the text layer. [PDF support]
- Token cost: "Each page typically uses **1,500–3,000 tokens** per page depending on content density" for text, **plus** image tokens for each page image. [PDF support]
- Image tokens: an image costs `⌈w/28⌉ × ⌈h/28⌉` visual tokens. Claude 4.7 and later (including Opus 5.5 and Sonnet 5.5) use the high-resolution tier, capped at **2576 px long edge / 4,784 visual tokens** per image. [Vision]
- The docs warn about density: "Dense PDFs (many small-font pages, complex tables, or heavy graphics) can fill the context window before reaching the page limit… Try splitting the document into sections." [PDF support]
- PDF input is available on every platform: Claude API, Claude Platform on AWS, Bedrock, Google Cloud, Microsoft Foundry. **Bedrock gotcha:** in the Converse API, the visual (page-image) mode is used only when citations are enabled. Without citations it falls back to text extraction only. [PDF support]

### Citations

- Turn it on per document block with `citations: {enabled: true}`. PDF text is **chunked into sentences**. Claude can cite a single sentence or chain consecutive sentences into a passage. A PDF citation has type `page_location` with `start_page_number` / `end_page_number` (1-indexed) and the verbatim `cited_text`. [Citations]
- Reliability: "citations are guaranteed to contain valid pointers to the provided documents." `cited_text` **does not count toward output tokens**. [Citations]
- Limits:
  - Only text can be cited, not images.
  - **Scanned PDFs with no extractable text cannot be cited.** Run OCR first, for example with Azure Document Intelligence (see §4).
  - Citations and structured outputs **cannot be used together**: "If you enable citations on any user-provided document… and also include the `output_config.format` parameter… the API returns a 400 error." [Citations]
- **Custom content documents** let you supply your own chunks (for example, one paragraph or table row per block). Citations then come back as `content_block_location` block indices. This is the hook for joining Claude citations to an OCR layer that carries page numbers and bounding boxes. [Citations]
- Citations work together with prompt caching, token counting, and batch processing. [Citations]

### Structured outputs / strict tool use

- `output_config.format` (JSON Schema) or `strict: true` on a tool guarantees schema-valid output. GA on Opus 5.5, Sonnet 5.5, and Haiku 4.5. **Not available on "Claude in Amazon Bedrock"** (the legacy integration only, up to Opus 4.6). [Structured outputs]
- Schema limits: no recursive schemas, and no `minimum`/`maximum`/`minLength` constraints (SDK helpers move these into descriptions and validate client-side). `additionalProperties` must be `false`. A request may have at most 20 strict tools, **24 optional parameters**, and 16 union-typed parameters in total. Compiled grammars are cached for 24 hours. [Structured outputs]
- On Opus 5.5 and Sonnet 5.5, forced `tool_choice` (`any` / `tool`) **returns a 400**. Use `output_config.format` instead. [claude-api skill, model notes]

### Models, context, pricing (Claude API, first-party)

| Model | ID | Context | Input $/MTok | Output $/MTok | Cache read | Batch (in/out) |
|---|---|---|---|---|---|---|
| Claude Opus 5.5 | `claude-opus-5-5` | 1M | 4 | 20 | 0.20 | 2 / 10 |
| Claude Sonnet 5.5 | `claude-sonnet-5-5` | 1M | 2 | 10 | 0.20 | 1 / 5 |
| Claude Haiku 4.5 | `claude-haiku-4-5` | 200K (so 100-page PDF cap) | 1 | 5 | 0.10 | 0.5 / 2.5 |

- There is **no long-context surcharge**: "A 900k-token request is billed at the same per-token rate as a 9k-token request." [Pricing]
- Cache writes cost 1.25× the input price (5-minute TTL) or 2× (1-hour TTL). Batch is −50%, and the discounts stack. [Pricing]
- The 4.7+ tokenizer produces "approximately 30% more tokens for the same text". [Pricing]
- Rate limits are not a blocker even at the Start tier (Opus 5.5: **2M ITPM, 400K OTPM**), and cached reads don't count toward ITPM. [Rate limits]
- Opus 5.5 thinking is always on and is controlled by `output_config.effort`. The default is `medium`; use `high` for extraction. [claude-api skill]

## 2. Limits and risks for this use case

| Risk | Why it matters | Mitigation |
|---|---|---|
| Context overflow on dense PDFs | 200 pages × (1.5–3K text + up to ~4.8K image) can reach ≈0.5M–1.5M tokens, over the 1M window | `count_tokens` first. Scope pages: general section plus the sub-fund appendix. Or send text only. |
| Scanned / image-only pages | Not citable (no extractable text) | OCR (Azure DI Layout) → custom content document |
| Citations ⟂ structured outputs | Can't get schema JSON and native citations in one call | Two-pass pipeline (§5) |
| Page-number offsets | If you split the PDF, cited pages are relative to the sub-document | Keep a `subdoc_page → original_page` map |
| Cross-references and defined terms ("US Person as defined in Section 12") | Rule text and definition live on different pages | Prompt Claude to cite both the rule and the definition. Allow multiple Citations per rule. |
| Umbrella vs sub-fund overrides, exceptions ("except for Class I…") | Easy to pull the general rule and miss the override | Ask Claude to extract per Share Class. In the gold set, specifically check overrides. |
| Share-class tables spanning pages | Minimum subscription and eligible investors often sit in a table | Native PDF (sees page images) does better than text-only. Azure DI handles cross-page tables. |
| Hallucinated or loose rules | A Rule Card feeds a deterministic gate | Citation required for every rule, a deterministic quote check, and human validation (ADR 0001) |

## 3. Cost and latency estimate for a ~200-page prospectus

Assumptions:

- Output is about 20K tokens per extraction (thinking plus rules).
- The normalise pass takes about 10K tokens in and 5K out.
- Token counts are estimates. **Measure them with `count_tokens` on real documents.**

| Scenario | Input tokens | Opus 5.5 | Sonnet 5.5 |
|---|---|---|---|
| A. Whole prospectus, native PDF | ~800K | $3.20 + $0.40 = **~$3.60** | **~$1.80** |
| B. Whole prospectus, text only | ~400K | $1.60 + $0.40 = **~$2.00** | **~$1.00** |
| C. Scoped: ~60 pp general + ~10 pp sub-fund, native | ~280K | $1.12 + $0.40 = **~$1.50** | **~$0.75** |
| Extra Share Class reusing cached A | 800K cache read | $0.16 + $0.40 = **~$0.56** | **~$0.36** |
| Normalise pass (pass 2) | ~10K | **~$0.14** | **~$0.07** |

- Caching the prospectus for a 5-minute TTL raises the first call by 0.25× input (A: $3.20 → $4.00). It pays off from the second Share Class onwards.
- The Batch API halves everything, but results can take up to 24 hours. Use it to pre-compute a corpus, not live in the demo.
- A hackathon budget of a few dozen prospectuses comes to **well under $100**.

Latency is an **estimate, not a vendor figure**. Prefilling several hundred thousand tokens plus generating 10–20K tokens with thinking at high effort comes to roughly **2–6 minutes** per uncached extraction. Cached follow-up calls and pass 2 take seconds to about a minute. Measure this in the spike. If it matters, Opus 5.5 fast mode (Claude API only, $8/$40) speeds up output generation.

## 4. Alternatives (brief)

| Option | What it gives | Limits / price | Fit for us |
|---|---|---|---|
| **Azure AI Document Intelligence: Layout** | OCR, paragraphs with roles (title, sectionHeading…), tables with cells and spans, **Markdown output**, figures, sections. Bounding `polygon` and character `spans` for every element. [Azure DI Layout] | Up to **2,000 pages**, 500 MB (S0). Price: see the Azure calculator (not retrievable from the pricing page). | Best preprocessor for scanned docs and tricky tables. Feed paragraphs and table rows to Claude as a **custom content document**. The `content_block_location` citations then map to page and polygon, so the UI can highlight the passage. |
| **Google Document AI: Layout Parser** | Hierarchical layout, tables, figure descriptions, context-aware chunks with **page spans**. [Google Layout Parser] | Online: 15 pages / 20 MB. Batch: **500 pages** / 1 GB. Pricing on a separate page. | Comparable to Azure DI. Batch-only at 200 pages, which adds friction. |
| **LlamaParse (LlamaCloud)** | PDF → Markdown/JSON with layout-aware parsing, tables, charts, custom schemas. [LlamaParse] | Credits at $1.25/1K. Per page: Fast 1, Cost-effective 3, Agentic 10, Agentic Plus 45. **200 pages ≈ $0.25 / $0.75 / $2.50 / $11.25.** [LlamaParse pricing] | Good Markdown quality, but it adds a vendor and doesn't give us citation semantics by itself. |
| **OpenAI file inputs (Responses API)** | Extracts text **and page images** and sends both to vision models, with a `detail` control. [OpenAI PDF files] | 50 MB per file and per request | Functionally similar to Claude's native PDF input. That page documents no page-level citation feature, so we would need our own quote-verification. |

## 5. Recommended extraction pipeline for the spike

```
            ┌──────────────┐  EMT (xlsx/csv) ──► deterministic parser ──► Target Market fields (cite: EMT field id)
            │ Fund Docs    │
            └──────┬───────┘
                   │ prospectus + KID (PDF)
   0. Prep  ───────┼─► pypdf: locate general section + sub-fund appendix (TOC / bookmarks / keyword scan)
                   │   keep page map; count_tokens; if >~700K or scanned → Azure DI Layout → custom content doc
                   ▼
   1. Draft  Claude Opus 5.5, effort=high, Files API file_id, citations:{enabled:true}, cache_control on doc
             Prompt: "For Share Class <X> list every Hard Restriction and Target Market criterion,
                      one per paragraph, prefixed [RULE:<type>]; cite rule text AND any definition it relies on."
             → text blocks, each with citations[] (page_location, cited_text)  ← guaranteed-valid pointers
                   ▼
   2. Normalise  Claude (Sonnet 5.5 is enough), NO documents, output_config.format = RuleCard JSON schema
             Input: pass-1 rules with citation ids C1..Cn → output typed rules referencing citation ids
             (e.g. {type:"US_PERSON_EXCLUDED", value:true, citations:["C3","C7"]},
                   {type:"MIN_INITIAL_SUBSCRIPTION", amount:1000000, currency:"EUR", citations:["C12"]})
                   ▼
   3. Verify (deterministic)  every rule has ≥1 citation; citation ids resolve; values (amounts, ISO country
             codes, investor classes) parse into engine enums; map sub-doc pages → original pages
                   ▼
   4. Human validation UI  show each rule beside cited_text + page link; approve / edit / reject
                   ▼
   5. Rule Card (validated, versioned per Share Class + doc hash) ──► deterministic Eligibility Check engine
```

Why this shape:

- **Pass 1 keeps the API's guarantee that citations are valid.** The Citation on every rule is a real passage on a real page, with no fuzzy matching to write. This directly answers "what if the AI is wrong?" (ADR 0001).
- **Pass 2 gets schema-valid JSON** for the engine. Its input is small (no documents), so it is cheap and fast. Citation ids carry the link from each rule back to its passage.
- **One cached prospectus serves many Share Classes.** Umbrella prospectuses cover many sub-funds, so caching the document and querying per Share Class costs about $0.50 per extra class.
- **Plan B if Citations prove awkward**: run a single pass with structured outputs, where each rule carries a verbatim `quote` and `page`. Then verify deterministically by fuzzy-matching the quote against that page's text layer (PyMuPDF/pdfplumber) and flag any rule that fails as Needs Review. It is simpler to build, but citation validity is no longer guaranteed by the API.
- **Hackathon shortcuts**:
  - Pre-compute Rule Cards for the demo funds.
  - Use the Claude API directly. Bedrock lacks structured outputs on the current integration and needs citations on for visual PDF mode in Converse.
  - Skip Azure DI unless a demo document is scanned.

### Evaluation for the spike

1. Hand-label a gold Rule Card for **2–3 real prospectuses** (one umbrella with several share classes, one with a scanned or table-heavy appendix).
2. Metrics:
   - **Rule recall** (did it find every Hard Restriction?) and **precision**
   - **Value accuracy** (amounts, currencies, jurisdictions)
   - **Citation support rate** (does the cited passage actually support the rule? Judge by hand or with an LLM judge)
   - Tokens, $, and latency per document
3. Compare: native PDF vs text-only, Opus 5.5 vs Sonnet 5.5, whole doc vs scoped pages.

## Sources

- [PDF support] https://platform.claude.com/docs/en/build-with-claude/pdf-support
- [Citations] https://platform.claude.com/docs/en/build-with-claude/citations
- [Structured outputs] https://platform.claude.com/docs/en/build-with-claude/structured-outputs
- [Vision] https://platform.claude.com/docs/en/build-with-claude/vision
- [Pricing] https://platform.claude.com/docs/en/about-claude/pricing
- [Rate limits] https://platform.claude.com/docs/en/api/rate-limits
- [claude-api skill] Bundled Claude API reference (model ids, Opus 5.5 thinking/effort and `tool_choice` notes), cached 2026-09-25. Also see https://platform.claude.com/docs/en/about-claude/models/overview
- [Azure DI Layout] https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/prebuilt/layout
- [Google Layout Parser] https://docs.cloud.google.com/document-ai/docs/layout-parse-chunk
- [LlamaParse] https://developers.llamaindex.ai/python/cloud/llamaparse/overview
- [LlamaParse pricing] https://developers.llamaindex.ai/python/cloud/general/pricing
- [OpenAI PDF files] https://developers.openai.com/api/docs/guides/pdf-files
- EMT background: FinDatEx (https://findatex.eu). Not fetched in this research. Confirm the current EMT version and field list before building the parser.
