package com.fundgate.fundeli5.rulecardpipeline;

import java.util.List;

/** Turns PDF bytes into the text of each page. */
public interface PdfText {

    /** The text of each page, in order: element 0 is page 1. */
    List<String> pages(byte[] pdf);
}
