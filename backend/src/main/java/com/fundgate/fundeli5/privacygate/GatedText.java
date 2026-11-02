package com.fundgate.fundeli5.privacygate;

/** Text the Privacy Gate has passed: the proof a model call may carry it (ADR 0004). Only the Gate builds one. */
public sealed interface GatedText permits GatedEmail, GatedPrompt {

    String text();

    /** The client fields this text carries to a model, for the Check Record. */
    java.util.List<String> fieldsSent();
}
