package com.fundgate.fundeli5.llm;

/**
 * Replay mode holds no recording for this request; record it with the recording egress. The
 * message names the model and sizes only, never the prompt, so it is safe to log.
 */
public class UnrecordedLlmRequest extends RuntimeException {

    public UnrecordedLlmRequest(LlmRequest request) {
        super("No recorded answer for model " + request.model() + " (system " + request.system().length()
                + " chars, prompt " + request.prompt().length() + " chars, hash "
                + Integer.toHexString(request.hashCode()) + ")");
    }
}
