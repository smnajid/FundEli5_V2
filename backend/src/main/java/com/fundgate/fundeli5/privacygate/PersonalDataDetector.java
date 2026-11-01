package com.fundgate.fundeli5.privacygate;

import java.util.List;

/** Out port of the Privacy Gate (ADR 0009): finds personal data, a person's name first of all, in free text. */
public interface PersonalDataDetector {

    /**
     * @throws PersonalDataDetectorUnavailable when the detector cannot answer; the Gate then fails closed
     */
    List<DetectedSpan> detect(String text);
}
