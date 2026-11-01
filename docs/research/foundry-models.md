# Which Microsoft Foundry models and features can run the cited extraction pipeline?

Research for issue #13 (wayfinder map #1). It builds on #4 ([`llm-rule-extraction.md`](https://github.com/smnajid/fund-gate/blob/research/llm-rule-extraction/docs/research/llm-rule-extraction.md)) and ADR 0001 (`docs/adr/0001-llm-extracts-rules-engine-decides.md`). Researched **2026-10-03** against primary docs: Microsoft Learn, Anthropic and OpenAI. Each Microsoft Learn page is cited with its `ms.date`.

The pipeline being checked comes from #4:

- **Pass 1:** the whole prospectus PDF goes in as native PDF input with Citations turned on, and Claude drafts rules with verbatim passages and page numbers.
- **Pass 2:** structured output turns the draft into Rule Card JSON.
- **Pass 0, only for scanned pages:** OCR with Azure Document Intelligence.

The constraint: Fund Documentation may go to an external LLM, but client-identifying data may not.

## TL;DR

- **Yes, the #4 pipeline runs on Foundry unchanged.** Claude Opus 5.5 and Sonnet 5.5 are in the Foundry catalogue as GA, in both hosting options. Through Foundry you get PDF input, Citations, prompt caching (5-minute, 1-hour and automatic), structured outputs and strict tool use, token counting, and the 1M context window. You call them with Anthropic's own SDK (`AnthropicFoundry`), so the code is the same as for the direct API except for how the client is built.
- **Catch 1: the Files API is missing on "Hosted on Azure" deployments** (they return a 400). Send the PDF inline as base64. The request is still capped at 32 MB, and base64 adds about 33%, so the raw PDF must be under about 24 MB. Prompt caching still means the PDF is only paid for in full once per 5-minute or 1-hour window.
- **Catch 2: default rate limits depend on the subscription type, and this could block the spike.**
  - Pay-as-you-go subscriptions default to **40K input tokens per minute (ITPM) and 8K output tokens per minute (OTPM)** for Opus 5.5 and Sonnet 5.5. A single 300K–800K-token prospectus request is far over that.
  - Enterprise and MCA-E subscriptions default to **10M ITPM and 2M OTPM**.
  - Free-trial, student, sponsored-credit and **CSP subscriptions cannot deploy Claude at all.**
  - **Ask the organisers what subscription type the event uses.** If it is pay-as-you-go or CSP, use our own Anthropic API key or our own Enterprise Azure subscription.
- **Catch 3: there is no EU or Swiss residency for Claude on Foundry.**
  - "Hosted on Azure" offers Global Standard everywhere, plus a **US-only** Data Zone. There is no EU Data Zone.
  - The only European region for deploying Claude is **Sweden Central**, and that is still a Global deployment, so processing can happen anywhere.
  - "Hosted on Anthropic" may process data outside Azure entirely.
  - Fund Documentation is public, so this is acceptable for our use. It would not be acceptable for client data, which already stays out of every LLM.
- **The OpenAI-in-Foundry alternative works only partly.** GPT-5.5, GPT-5.6 and GPT-6 accept PDFs (extracted text plus page images, 50 MB), support structured outputs, and have a 1.05M context (about 922K usable input). **But no OpenAI path returns page-anchored citations.** PDF input has no citation feature. `file_search` annotations carry only `file_id`, `filename` and `index`, with no page and no quote. So we would have to build page anchoring ourselves: the model returns `{quote, page}` and we verify it against the text of each page. That is the "Plan B" from #4. GPT models do run in the EU Data Zone, and in-region Swiss options exist.
- **Azure Document Intelligence Layout** handles up to 2,000 pages and 500 MB per document. It returns paragraphs and tables, each with `boundingRegions[].pageNumber` and a polygon, plus Markdown output. It processes data in the region where the resource lives and deletes inputs and results after 24 hours. It is the right page-anchored text layer: as an OCR fallback for Claude, and as the verification layer for the GPT path.
- **Recommendation:** run the spike on **Claude Opus 5.5 in Foundry, Hosted on Azure, Global Standard**, with the PDF inline and Citations and caching turned on, and Sonnet 5.5 for pass 2.
  - **Fallback 1:** the same code against the **direct Anthropic API** with our own key. Use it if the event subscription can't deploy Claude or its limits are too low.
  - **Fallback 2:** **GPT-5.5 or GPT-6 in Foundry**, with structured output of `{rule, quote, page}`, verified against Document Intelligence Layout text.

## 1. Claude in the Foundry catalogue

### Models (Anthropic "Claude in Microsoft Foundry", fetched 2026-10-03; MS Learn "Claude models", ms.date 2026-09-21)

| Model | Hosted on Azure | Hosted on Anthropic | Context / max output |
|---|---|---|---|
| Claude Opus 5.5 (`claude-opus-5-5`) | GA | GA | 1M / 128K |
| Claude Sonnet 5.5 (`claude-sonnet-5-5`) | GA (Global Standard only) | GA | 1M / 128K |
| Claude Opus 5, Opus 4.8, Sonnet 5 | GA | GA | 1M / 128K |
| Claude Haiku 4.5 | GA | GA | 200K / 64K |
| Claude Fable 5.1, Fable 5 | no | Preview | 1M / 128K |
| Claude Opus 4.7, 4.6, 4.5; Sonnet 4.6, 4.5 | no | GA | 1M (4.5 models: 200K) |
| Mythos 5.1, Mythos 5, Mythos Preview | no | gated research preview | 1M / 128K |

[Anthropic Foundry] [MS Claude models]

### How it is deployed

- **Not classic serverless MaaS.** Claude is a third-party **Azure Marketplace** offer ("Claude Platform on Foundry"). The flow:
  1. Create a Foundry resource or project.
  2. Find the model in the catalogue and choose **Deploy → Custom settings**.
  3. On your first Claude deployment, accept the Marketplace terms.
  4. Pick the hosting option (shown as **version 1 = Hosted on Anthropic, version 2 = Hosted on Azure**) and the region scope (**Global**, or **Data Zone** for US only).

  [Anthropic Foundry] [MS deploy Claude, ms.date 2026-09-21]
- **Endpoint:** `https://{resource}.services.ai.azure.com/anthropic/v1/messages`.
  - Auth uses an Azure API key (`api-key` or `x-api-key` header) or a Microsoft Entra ID token.
  - The deployment name goes in `model`.
  - SDKs: the Python `AnthropicFoundry` client, `@anthropic-ai/foundry-sdk`, and Java `FoundryBackend` (`com.anthropic:anthropic-java-foundry`), plus C#, Go and PHP. Ruby is not supported. [Anthropic Foundry]
- **Two hosting options:**
  - **Hosted on Azure:** "Anthropic-operated service running on Azure infrastructure". Global Standard or US Data Zone Standard.
  - **Hosted on Anthropic:** runs on Anthropic infrastructure, Global Standard only.
  - Anthropic recommends Hosted on Azure "for most workloads". [Anthropic Foundry]
- **Billing:**
  - Charged as Claude Consumption Units (CCU) on the Azure invoice, metered hourly.
  - Pay-as-you-go, with no prepaid credits.
  - **Counts toward a Microsoft Azure Consumption Commitment (MACC)** for both hosting options. [MS hosting comparison, ms.date 2026-09-21]
  - Per-token rates are the same as Anthropic's first-party API, for example Opus 5.5 at $4 in / $20 out per million tokens. [claude-api skill pricing note, cached 2026-09-25; not re-fetched from the pricing page]
- **Regions for Global Standard deployments:**
  - Americas: centralus, eastus, **eastus2**, northcentralus, southcentralus, westcentralus, westus, westus3.
  - Europe: **swedencentral only**.
  - Fable 5 and 5.1 are only in eastus2 and swedencentral. [MS partners region table, ms.date 2026-09-21]
- **Data Zone Standard:** US only. It is available for Hosted-on-Azure Opus 5.5, Opus 5, Opus 4.8, Sonnet 5 and Sonnet 5.5. The Europe tab says "Not available". [MS partners region table]
  - *Inconsistency, not resolved:* the MS Global Standard region table lists only version 1 (Hosted on Anthropic) for `claude-sonnet-5-5`. The other MS and Anthropic pages say Sonnet 5.5 is GA Hosted on Azure. Check in the portal.

### Subscriptions and agreements required (MS deploy Claude, ms.date 2026-09-21)

- You need a **paid Azure subscription with a billing account in a country where Anthropic sells the offer**. The offer region is "Microsoft Managed Countries/Regions (except Belarus and Russia)". *I did not check whether CH and LU are on that list. It is very likely, but confirm in Partner Center.*
- **Not supported:**
  - Enterprise Accounts in South Korea
  - **Cloud Solution Provider (CSP) subscriptions**
  - subscriptions without an active pay-as-you-go payment method (student, **free trial**, **startup-credit**)
  - **sponsored subscriptions that only use Azure credits**
- **Permissions:**
  - Owner or Contributor on the subscription, for the Marketplace SaaS actions `Microsoft.MarketplaceOrdering/*`, `Microsoft.SaaS/register/action` and `Microsoft.SaaS/resources/*`.
  - To call the model: Foundry User or Cognitive Services User. [MS partners]
- **Terms:** Claude is a "Non-Microsoft Product under the Product Terms". **Anthropic is the seller, operator and an independent data processor** for both hosting options, and Anthropic's Commercial Terms and Data Processing Addendum (DPA) apply. [MS Claude data privacy, ms.date 2026-06-23] [MS hosting comparison]
- **Covered Models:** Fable 5, Fable 5.1 and the Mythos models require 30-day retention. A subscription with zero data retention (ZDR) gets a 400 when calling them. This does not affect Opus 5.5 or Sonnet 5.5. [MS deploy Claude troubleshooting] [Anthropic data retention]

## 2. Feature support: Foundry vs the direct Anthropic API

| Feature the pipeline needs | Direct Anthropic API | Foundry, Hosted on Azure | Foundry, Hosted on Anthropic | Source |
|---|---|---|---|---|
| PDF document input (text + page images) | Yes | **Yes** (base64 or URL) | Yes | [MS Claude models] [Anthropic PDF] |
| Citations (`page_location`, verbatim `cited_text`) | Yes | **Yes** | Yes | [MS Claude models] [claude-api skill platform table] |
| Prompt caching (5 min, 1 h, automatic) | Yes | **Yes** | Yes | [MS Claude models] |
| Structured outputs (`output_config.format`) and strict tool use | Yes | **Yes** (also accepts the legacy `output_format`) | Yes | [MS Claude models] |
| Tool use (client tools) | Yes | Yes | Yes | [MS Claude models] |
| Token counting (`/v1/messages/count_tokens`) | Yes | **Yes** | Yes | [MS Claude models] [MS hosting comparison] |
| Files API (`/v1/files`) | Yes | **No (400)** | Yes (beta) | [Anthropic Foundry] [MS Claude models] |
| Message Batches API | Yes | **No** | **No** | [Anthropic Foundry] |
| Models API, Admin API, server-side `fallbacks`, fast mode | Yes | No | No | [Anthropic Foundry] [claude-api skill] |
| Code execution, Agent Skills, programmatic tool calling, newer web search/fetch | Yes | No (400) | Yes | [Anthropic Foundry] |
| `inference_geo` (EU/US pinning per request) | `us` / `global` only | n/a (use the US Data Zone) | n/a | [Anthropic data residency] |
| 1M context (Opus 5.5 / Sonnet 5.5) | Yes | Yes | Yes | [Anthropic Foundry] |

Notes:

- **Citations and structured outputs are still mutually exclusive** (a 400 if both are set). This is an API rule, not a platform one, so the two-pass design from #4 stays. [#4, Citations]
- **PDF limits are the same as on the direct API:** 32 MB per request, and 600 pages on 1M-context models. With no Files API on Hosted on Azure, the PDF has to go inline as base64 in every pass-1 call. With `cache_control` on the document block, repeat calls in the same window (one per Share Class) read it from the cache. *I did not test whether the "URL" PDF source avoids the 32 MB payload limit.*
- **Rate-limit headers:** Foundry does **not** return the `anthropic-ratelimit-*` headers. Monitor through Azure instead. [Anthropic Foundry]
- **Opus 5.5 specifics on Foundry:**
  - Thinking is always on, and effort defaults to `medium`. Set `high` for extraction.
  - `temperature` and `top_k` are not supported.
  - `top_p` must be 0.99.

  [MS partners table] [MS Claude models]

### Rate limits (MS "Claude model quotas and rate limits", ms.date 2026-09-21)

Quota is **per subscription, shared across all regions**: one pool per model and version for Global Standard. Cache reads don't count toward ITPM.

| Subscription type | Opus 5.5 / Sonnet 5.5 (Global or US Data Zone) | Haiku 4.5 |
|---|---|---|
| **Pay-as-you-go** | **40 RPM / 40,000 ITPM / 8,000 OTPM** | 80 / 80,000 / 16,000 |
| **Enterprise and MCA-E** | **10,000 RPM / 10,000,000 ITPM / 2,000,000 OTPM** | same |
| Free Trial | 0 (can't use) | 0 |

- **Why this matters:** one uncached pass-1 call on a whole prospectus is about 300K–800K input tokens (#4). On a pay-as-you-go default of 40K ITPM, that **will not fit in a minute's budget**.
  - *I did not verify how Foundry treats a single request larger than the per-minute ITPM. Expect a 429.*
  - You can request more quota (aka.ms/oai/stuquotarequest), but approval is "evaluated individually and aren't guaranteed".
- **Direct Anthropic API, for comparison:** Opus 5.5 gets **2M ITPM / 400K OTPM even at the Start tier** (#4, [Rate limits]).

## 3. The OpenAI-in-Foundry alternative (GPT)

### Models and limits (MS "Foundry Models sold by Azure", ms.date 2026-09-21; region page ms.date 2026-09-03)

- **Current models:** `gpt-6.1-sol` (2026-09-29), `gpt-6-sol` / `gpt-6-luna` / `gpt-6-astra`, `gpt-5.6-sol` / `terra` / `luna`, and `gpt-5.5`.
- **Context:** all have a **1,050,000-token context window, with about 922,000 tokens of input and 128,000 of output.** For example, for GPT-5.5: "You can't combine a 922,000-token prompt with 128,000 generated tokens in one request."
- **Structured outputs:** all list "Structured outputs", meaning a JSON schema in `text.format` on the Responses API, or `response_format` on Chat Completions. [MS Responses, ms.date 2026-08-18]
- **Input:** all take text and images and return text. GPT-6 and GPT-5.6 price short-context and long-context requests differently.
- **Regions:**
  - Global Standard covers essentially every region, including switzerlandnorth, switzerlandwest and swedencentral.
  - **Data Zone Standard (EU)** is available for gpt-5.5, gpt-5.6 and gpt-6.x from resources in francecentral, germanywestcentral, italynorth, norwayeast, polandcentral, spaincentral, swedencentral, **switzerlandnorth** and westeurope.
  - **Regional Standard in switzerlandnorth** (processing stays in that region) is only available for older models: gpt-4.1, gpt-4.1-mini and gpt-4o (2024-11-20). gpt-4.1 has a 1M context, but its provisioned deployments are capped below 128K. [MS region availability]
  - *Note:* an EU Data Zone deployment processes "within any EU member nation". A Swiss resource on the EU Data Zone does not keep processing inside Switzerland.

### PDF input

- The Responses API takes `input_file`, either as a base64 data URI or a `file_id` (upload with `purpose="assistants"`; `user_data` isn't supported on Azure).
- Limits: **under 50 MB per file and per request.**
- "Both extracted text and rendered images of each page are included in the model's context."
- Only models that take both text and images accept PDFs.

[MS Responses] [OpenAI PDF files]

### Page-anchored citations: **not available**

- **OpenAI PDF input** has no citation feature at all. [OpenAI PDF files]
- **`file_search`** (vector stores):
  - Annotations are `file_citation` with **`file_id`, `filename` and `index`**. They carry **no page number and no quoted text**. [OpenAI file search]
  - On Foundry, documents are chunked into 800-token chunks with 400-token overlap, up to 20 chunks go into context, and files can be up to 512 MB. The C# sample only shows `FileId`. [MS Foundry file search, ms.date 2026-09-30]
  - A community Q&A reports that Azure's Responses `file_search` "searches only one attached vector store". This is secondary and I did not verify it.
  - Retrieval is also the wrong shape for us. We want the model to read the whole prospectus for one Share Class, not the top 20 chunks.
- **So the GPT path needs the "Plan B" from #4:**
  1. Give each page's text a page marker. Either put DI Layout or PyMuPDF text into the prompt as `[[PAGE 37]] …`, or send the PDF.
  2. Have structured outputs return `{rule, quote, page}`.
  3. Fuzzy-match each `quote` against the text of that page, deterministically.
  4. Mark any rule whose quote fails the check as Needs Review.

  This works, but the guarantee that citations are valid now comes from our verifier, not from the API.

## 4. Azure Document Intelligence: Layout as preprocessor or fallback

From MS Learn "Document layout analysis" (v4.0 `2024-11-30` GA; ms.date 2026-05-01):

- **Limits:** PDFs and TIFFs **up to 2,000 pages**, and **500 MB on the paid S0 tier**. The free F0 tier processes only the first 2 pages and 4 MB. The `pages` query parameter analyses chosen page ranges.
- **Output:**
  - `pages[]` with `pageNumber`, lines and words (each with a polygon and a confidence).
  - `paragraphs[]` with `content`, `boundingRegions` (page number plus polygon), `spans`, and a `role`: `title`, `sectionHeading`, `pageHeader`, `pageFooter`, `pageNumber` or `footnote`.
  - Tables with cells and spans.
  - Figures and sections.
  - The whole result can also come back as **Markdown**.
  - OCR covers scanned pages and handwriting.
- **Data handling:** data "is processed in the same region where the Document Intelligence resource was created". Inputs and results are stored for **24 hours** and then deleted automatically, or earlier through `Delete Analyze Result`. [MS DI data privacy, ms.date 2026-07-22]
- **Regions:** Document Intelligence is generally available in Switzerland North and Switzerland West. *That comes from a Microsoft Q&A answer. I did not confirm it on the product region page.*
- **Where it fits:**
  - **With Claude:** turn DI paragraphs and table rows into a **custom content document** (one block per paragraph or row). Citations then come back as `content_block_location`, which we map to DI's `pageNumber` and polygon so the UI can highlight the passage. Use this **only for scanned or low-quality pages**. Native PDF input is better when the text layer is good (#4).
  - **With GPT:** DI page text is the reference the quote verifier matches against, and it supplies the `[[PAGE n]]` markers.

## 5. Data residency and data handling (Swiss and Luxembourg bank view)

What goes to the LLM is **public Fund Documentation only** (prospectus, KID, EMT). No Client Profile data goes to any LLM (constraint and ADR 0001). So residency here is something to tell the jury about, not a blocker. It would only become a blocker if client data were ever sent, and it won't be.

| Option | Where prompts are processed | Retention | Training on our data | Processor and terms |
|---|---|---|---|---|
| **Claude on Foundry, Hosted on Azure** | Azure infrastructure. "Data at rest is stored in the selected Azure geography". Processing follows the Global scope (any region) or the US Data Zone. **No EU or CH Data Zone.** Only content flagged by safety systems, plus usage metadata, goes to Anthropic. | Under Anthropic's DPA and Commercial Terms | No (Anthropic: "never used for model training without your express permission") | Anthropic is an independent processor. Microsoft bills. [MS Claude data privacy] [Anthropic Foundry] |
| **Claude on Foundry, Hosted on Anthropic** | Anthropic infrastructure. "Data might be processed outside of Azure including outside of your selected Azure region." | Same | No | Anthropic. Microsoft shares contact and usage details with Anthropic. [MS Claude data privacy] |
| **Direct Anthropic API** | `inference_geo` is `global` (default) or `us` only (US costs 1.1×). Workspace geo (data at rest) is **US only**. No EU option. | API inputs and outputs deleted **within 30 days**. Usage-policy violations kept up to 2 years. ZDR available by contract. Files API files kept until deleted. | No | Anthropic Commercial Terms and DPA. [Anthropic data residency] [Anthropic data retention] [Anthropic privacy: commercial retention, updated 2026-07-01] |
| **Azure OpenAI (GPT) in Foundry** | Regional Standard: the deployment's region (switzerlandnorth possible for gpt-4.1 / 4o). Data Zone EU: any EU member state. Global: anywhere. Data at rest stays in the resource's geography. | Abuse-monitoring store in the resource's geography (reviewed by people located in the EEA for EEA deployments). You can apply for modified abuse monitoring, which removes storage and human review. Responses API, Files and vector stores persist data until you delete it. | No. Prompts and outputs are "NOT available to OpenAI" and "NOT used… to train" | Microsoft, under the Microsoft Products and Services DPA. [MS Azure OpenAI data privacy, ms.date 2026-05-18] |
| **Azure Document Intelligence** | The resource's region (for example switzerlandnorth) | 24 hours, then deleted | Not stated on that page | Microsoft DPA. [MS DI data privacy] |

- **The best residency story is Azure OpenAI + Document Intelligence in a Swiss or EU Data Zone.** Microsoft is the sole processor, there is EEA-located human review, and nothing goes to OpenAI.
- **For Claude on Foundry**, the honest pitch is: "public documents only, processed on Azure (Hosted on Azure), Anthropic as processor under a DPA, no training". Foundry offers no in-EU guarantee for Claude today.
- **Not verified:** whether the event's Foundry tenant has any policy that restricts Marketplace (third-party) models. Ask the organisers (#5).
- **Not verified:** whether `store: false` on Azure's Responses API avoids persisting the data, as it does on OpenAI. The Azure privacy page only says the Responses API "stores message history".

## 6. Recommendation

**Run the extraction spike on Claude Opus 5.5 in Microsoft Foundry: Hosted on Azure, Global Standard deployment, resource in Sweden Central or East US 2.**

- **Why:** every feature the #4 pipeline relies on is supported on Hosted on Azure: native PDF, Citations with guaranteed-valid page pointers, prompt caching, structured outputs and `count_tokens`. It runs on the event's own infrastructure, which matches the organisers' expectation, and prompts stay on Azure.
- **Changes from #4:**
  - Send the PDF **inline as base64**, not by Files API `file_id`, with `cache_control` on the document block.
  - Keep the raw PDF under about 24 MB, or split it and keep a page map.
  - Use `AnthropicFoundry` (Python) or `FoundryBackend` (Java) as the client, with the deployment name as `model`.
  - Use Sonnet 5.5 for pass 2.
- **Day-1 checks, before building on it:**
  1. What **subscription type** does the event's Foundry use? Pay-as-you-go means a 40K ITPM default, which is unusable for whole-prospectus calls. CSP or sponsored-credit subscriptions can't deploy Claude at all.
  2. Is the Claude Marketplace offer allowed in the tenant, and do we have Contributor/Owner to accept the terms?
  3. Make a smoke-test call: one 200-page PDF with `citations.enabled` plus `count_tokens`, against our deployment.

**Fallback 1 (same code, different client): the direct Anthropic API with our own key.**
- Use it if checks 1–3 fail.
- It has higher default limits (2M ITPM at the Start tier), the Files API, Batches, and fast mode.
- It is the same request shape, so switching is one line.
- Trade-off: it isn't the event infrastructure, and residency is global or US only.

**Fallback 2 (Foundry-native, no Claude): GPT-5.5 or gpt-6-sol on Azure OpenAI, plus Document Intelligence Layout.**
- Use it if Claude is unavailable and we must stay on event infra, or if the jury pushes hard on EU/CH residency. GPT runs in the EU Data Zone from a switzerlandnorth resource, and DI runs in the Swiss region.
- The cost is building the citation layer ourselves: DI page text, a structured `{rule, quote, page}` output, and a deterministic quote verifier. Plan that as about half a day of extra work. That is my estimate.

**Azure Document Intelligence Layout is needed in every option**, but only for scanned or table-heavy pages (Claude: a custom content document) or as the verification text layer (GPT).

## Not verified / open

- Whether Switzerland and Luxembourg billing accounts are on Anthropic's Marketplace offer list ("Microsoft Managed Countries").
- How Foundry handles a single request whose input tokens exceed the per-minute ITPM quota.
- Whether a URL-sourced PDF avoids the 32 MB base64 payload limit on Hosted on Azure.
- Whether `claude-sonnet-5-5` is actually deployable Hosted on Azure in Global Standard. The MS region table and the model tables disagree.
- The event subscription's type and tenant policies. This is a question for the organisers (#5).
- Whether `store: false` is honoured on the Azure Responses API.
- Document Intelligence availability in Switzerland North (secondary source only).
- No latency or accuracy was measured. Everything here comes from the docs, not from a test call.

## Sources

Microsoft Learn (all fetched 2026-10-03):

- [MS Claude models] Claude models in Microsoft Foundry, ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/claude-models
- [MS deploy Claude] Deploy and use Claude models in Microsoft Foundry, ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/how-to/use-foundry-models-claude
- [MS Claude quotas] Claude model quotas and rate limits, ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/claude-models-quotas-limits
- [MS hosting comparison] Compare hosting options for Claude models, ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/claude-models-hosting-comparison
- [MS partners] Foundry Models from partners and community (offer regions, region tables), ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-from-partners
- [MS Claude data privacy] Data, privacy, and security for Claude models in Foundry, ms.date 2026-06-23: https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/claude-models/data-privacy
- [MS models sold by Azure] GPT model capabilities and context windows, ms.date 2026-09-21: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure
- [MS region availability] Region availability for Foundry Models sold by Azure, ms.date 2026-09-03: https://learn.microsoft.com/en-us/azure/foundry/foundry-models/concepts/models-sold-directly-by-azure-region-availability
- [MS Responses] Azure OpenAI Responses API (PDF input, structured outputs, regions), ms.date 2026-08-18: https://learn.microsoft.com/en-us/azure/foundry/openai/how-to/responses
- [MS Foundry file search] File search tool for Foundry agents, ms.date 2026-09-30: https://learn.microsoft.com/en-us/azure/foundry/agents/how-to/tools/file-search
- [MS Azure OpenAI data privacy] Data, privacy, and security for Foundry Models sold by Azure, ms.date 2026-05-18: https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/openai/data-privacy
- [MS DI Layout] Document layout analysis (v4.0 2024-11-30), ms.date 2026-05-01: https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence/prebuilt/layout
- [MS DI data privacy] Data, privacy, and security for Document Intelligence, ms.date 2026-07-22: https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/document-intelligence/data-privacy-security
- Secondary (Microsoft Q&A, not authoritative): Claude on Azure EU infrastructure timeline (https://learn.microsoft.com/en-us/answers/questions/5867930/), which predates the "Hosted on Azure" option and is superseded; Document Intelligence in Switzerland North (https://learn.microsoft.com/en-us/answers/questions/2156548/).

Anthropic (fetched 2026-10-03):

- [Anthropic Foundry] Claude in Microsoft Foundry: https://platform.claude.com/docs/en/build-with-claude/claude-in-microsoft-foundry
- [Anthropic data residency] Data residency (`inference_geo`, workspace geo): https://platform.claude.com/docs/en/manage-claude/data-residency
- [Anthropic data retention] API and data retention (covers Foundry, ZDR, Covered Models): https://platform.claude.com/docs/en/manage-claude/api-and-data-retention
- [Anthropic privacy] How long do you store my organization's data? (updated 2026-07-01): https://privacy.claude.com/en/articles/7996866-how-long-do-you-store-my-organization-s-data
- [Anthropic PDF], [Citations], [Rate limits]: as cited in #4 (https://platform.claude.com/docs/en/build-with-claude/pdf-support, …/citations, https://platform.claude.com/docs/en/api/rate-limits)
- [claude-api skill] Bundled Claude API reference, platform-availability table and pricing note, cached 2026-09-25

OpenAI (fetched 2026-10-03; the pages show no dates):

- [OpenAI PDF files] https://developers.openai.com/api/docs/guides/pdf-files
- [OpenAI file search] https://developers.openai.com/api/docs/guides/tools-file-search
