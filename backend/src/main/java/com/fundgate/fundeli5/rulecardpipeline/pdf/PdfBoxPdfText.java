package com.fundgate.fundeli5.rulecardpipeline.pdf;

import com.fundgate.fundeli5.rulecardpipeline.PdfText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** PDF to page text with PDFBox. */
public class PdfBoxPdfText implements PdfText {

    @Override
    public List<String> pages(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return List.copyOf(pages);
        } catch (IOException e) {
            throw new IllegalArgumentException("Not a readable PDF: " + e.getMessage(), e);
        }
    }
}
