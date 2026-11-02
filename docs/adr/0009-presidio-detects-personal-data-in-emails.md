# Presidio detects personal data left in an email; the fixture detector stands in for tests

The Privacy Gate no longer looks client names up in a list. After the Portfolio References are swapped for Portfolio Ids, it scans the pseudonymised text with a `PersonalDataDetector` out port. In production that is Microsoft Presidio's analyzer service (a Python service, allowed by [ADR 0006](0006-spring-boot-and-angular-python-only-for-real-added-value.md) because it adds real value: no list of names is complete). Any PERSON found, an unknown reference, or a detector that is unreachable or errors, means no LLM call and the dealer fills the request form by hand. This refines [ADR 0004](0004-two-runtimes-one-llm-egress.md), which set NER aside as "unnecessary once Requesters send references only": the Gate must still catch the Requester who names the client anyway, and a bank cannot list every client name.

Presidio is probabilistic, so the guard is asymmetric: the Gate never downgrades a detection, and it treats "detector down" exactly like a detected name. A missed name is still possible; the Gate narrows the risk, it does not remove it, and the allow-list and Check Record stay as the other defences.

## Adapters

Chosen by `fundeli5.privacygate.detector`, as for every port ([ADR 0008](0008-bank-data-enters-through-canonical-contracts.md)):

- `fixture` (tests only, set in the test config): deterministic detection of the named demo clients (full name or surname) from demo-dataset.md. Used by tests and the specification; no Docker.
- `presidio` (default, so a forgotten property fails closed when the service is down): `POST <fundeli5.privacygate.presidio.url>/analyze` with `{text, language, entities: ["PERSON"]}`, once per language in `fundeli5.privacygate.presidio.languages`, results united. The analyzer image is `mcr.microsoft.com/presidio-analyzer` (container port 3000; the default URL is `http://localhost:5002`, i.e. `docker run -p 5002:3000 mcr.microsoft.com/presidio-analyzer`). Tested against a stubbed HTTP server.

## Languages

Emails are EN, FR or DE. The stock image ships an English NLP model only, so **only `en` works out of the box**. FR and DE need the analyzer started with an NLP configuration that loads spaCy `fr_core_news_lg` and `de_core_news_lg` (Presidio's NLP engine configuration file); until then set `languages=en` and a request for `fr` or `de` is answered 400, which the Gate treats as unavailable and refuses. A consequence to know: Presidio also flags the Requester's own name (e.g. a signature). Every email with a PERSON in it goes to the dealer by hand under this rule; tuning (ignoring the sender's header and signature) is a later decision for the business.

## Run order

1. Swap each `[A-Z]{3}\d{8}` for its Portfolio Id through `ClientProfiles`; an unknown reference refuses.
2. Detect on the pseudonymised text; any PERSON refuses, the detector failing refuses.

Names stay out of the Client Profile (client-profile.md, "Not in the contract").

## Considered Options

- **A list of known client names from the CRM**: needs a new port returning client names, which the contract deliberately leaves out, and misses every name not in the list.
- **Client name in `ClientProfiles`**: changes a port and the contract.
- **Regex heuristics for capitalised words**: no Python service, but far noisier than a trained recogniser.

## Boundaries of this decision

- The Gate looks for **names** (PERSON). Other free-text personal data (email address, phone, street address, birth date) is not detected; Requesters send references only, and widening the entities is a later decision.
- The Presidio service receives pseudonymised text that may still hold a name, so it must run inside the bank's boundary (local container or in-bank host), never a public endpoint. The adapter accepts http(s) URLs only and follows no redirects.
- After the swap the Gate also refuses anything shaped like a reference that it could not swap (other case, a separator, extra digits).

## Known limits

Recorded at the Integrate checkpoint (#27); mohamed decides on each later.

- Detection is PERSON only; the "never sent" list is wider (see Boundaries above).
- A failed Request Parser call writes no Check Record.
- Presidio languages default to `en` only (`fundeli5.privacygate.presidio.languages`).
- There is no deterministic rescan of the CRM client names after the Gate's swap.
