package com.fundgate.fundeli5.replydrafter;

import java.util.List;

/** A drafted reply, and the model call that wrote it (null when the template did), for the Check Record. */
public record Reply(String text, ModelCall call) {

    public record ModelCall(String model, String gatedPrompt, List<String> fieldsSent, String response) {
        public ModelCall {
            fieldsSent = List.copyOf(fieldsSent);
        }
    }

    public static Reply fromTemplate(String text) {
        return new Reply(text, null);
    }
}
