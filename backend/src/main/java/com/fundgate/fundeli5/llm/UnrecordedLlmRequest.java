package com.fundgate.fundeli5.llm;

/** Replay mode holds no recording for this request; record it with the recording egress. */
public class UnrecordedLlmRequest extends RuntimeException {

    public UnrecordedLlmRequest(LlmRequest request) {
        super("No recorded answer for model " + request.model() + ", prompt starting \""
                + abbreviate(request.prompt()) + "\"");
    }

    private static String abbreviate(String text) {
        String flat = text.replaceAll("\\s+", " ");
        return flat.length() <= 80 ? flat : flat.substring(0, 80) + "...";
    }
}
