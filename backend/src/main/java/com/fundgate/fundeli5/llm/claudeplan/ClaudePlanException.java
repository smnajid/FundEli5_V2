package com.fundgate.fundeli5.llm.claudeplan;

/** The {@code claude} subprocess failed. The message never carries the prompt or the model's output. */
public class ClaudePlanException extends RuntimeException {

    public ClaudePlanException(String message) {
        super(message);
    }

    public ClaudePlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
