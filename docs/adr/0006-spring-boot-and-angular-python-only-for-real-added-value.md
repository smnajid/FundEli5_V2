# Spring Boot 4 and Angular for the POC; Python only where it adds real value

The hackathon builds a throwaway proof of concept. If it shows value, the bank rebuilds it in its own stack. So the POC stack is chosen for how many features we can explore in 36 hours, not for production. We still build it in the bank's standard stack: Spring Boot 4 (Java) for the backend and the Rule Card pipeline, and Angular for the dealer screen and the Rule Card validation screen. With coding agents writing most of the code, the slow step is checking it: reviewing generated code, getting an agent unstuck, steering the design, and fixing things on demo day. The driver and the likely teammates are fluent in Java and less so in TypeScript, so a Java backend is where we check code fastest and catch the most bugs. The Rule Engine and the Privacy Gate need the most careful checking, and they stay in Java. The Angular screens are TypeScript, but UI bugs show on screen, so they are easy to verify.

To recover most of the speed a single-language stack would give, we use three practices:
- **Generated client types.** The backend publishes an OpenAPI spec (springdoc), and the Angular types and HTTP client are generated from it, so nobody hand-copies DTOs.
- **One codebase.** The Rule Card pipeline is a command-line mode of the same Spring Boot app, which gives one build, one image and shared domain types. It runs as its own process with no access to client data, so the two runtimes of ADR 0004 stay separate.
- **Tests catch what review misses.** Spring Boot DevTools keeps restarts to seconds, and the Rule Engine is built test-first against the coverage set, so a wrong Verdict fails a test even when a review misses it.

Python is used only when a module needs something the JVM cannot do well, because the IT department is still young on Python. Today no module does. The extraction spike was written in Python, but everything it relied on has a Java equivalent. The official Anthropic Java SDK supports PDF document blocks, Citations with page locations and structured outputs, and has a Microsoft Foundry backend (`anthropic-java-foundry`). Apache PDFBox replaces pypdf for page selection and the quote-on-page check. The spike's code could not be reused anyway, because the organisers forbid pre-built product code. The egress client calls the official SDK directly, so Citations reach the Rule Card unchanged.

If the team, once recruited, turns out to be stronger in TypeScript than in Java, revisit this decision at team formation. The language the reviewers know best wins.

## Considered Options

- **TypeScript end to end (Angular + NestJS)**: one language, shared schema types, sub-second reloads and less boilerplate per feature. But it costs review speed and debugging fluency for a Java-first team, and in 36 hours that loss is larger than the gain.
- **Python for the offline Rule Card pipeline**: closest to the spike and quick for prompt iteration, but it adds a second build, a second container and a stack the team is new to, for no capability Java lacks.
- **Python with Streamlit for the screens**: fastest to start, but it looks like a prototype in front of the business jury.
- **Spring AI as the LLM layer**: portable across providers, but we have not checked that it exposes page-level Citations and Foundry hosting options, and it adds an abstraction between the Privacy Gate and the one egress client.
- **React instead of Angular**: the stack AI tools know best, but the bank runs Angular, and Angular's own agent skill and MCP server close most of that gap.
