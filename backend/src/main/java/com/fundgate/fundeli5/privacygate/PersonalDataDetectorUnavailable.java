package com.fundgate.fundeli5.privacygate;

/** The detector is unreachable or answered with an error. The Gate treats it like a detected name. */
public class PersonalDataDetectorUnavailable extends RuntimeException {

    public PersonalDataDetectorUnavailable(String message) {
        super(message);
    }

    public PersonalDataDetectorUnavailable(String message, Throwable cause) {
        super(message, cause);
    }
}
