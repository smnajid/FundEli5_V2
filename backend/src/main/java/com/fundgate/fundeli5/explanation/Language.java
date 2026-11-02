package com.fundgate.fundeli5.explanation;

/** The languages the desk has worded templates for; picked by the request's language, never translated by a model. */
public enum Language {
    EN, FR;

    public String code() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
