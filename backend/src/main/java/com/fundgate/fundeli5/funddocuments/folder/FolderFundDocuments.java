package com.fundgate.fundeli5.funddocuments.folder;

import static com.fundgate.fundeli5.bankdata.json.Json.editionDate;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredString;
import static com.fundgate.fundeli5.bankdata.json.Json.string;
import static com.fundgate.fundeli5.bankdata.json.Json.strings;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.DocumentUnavailable;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Reads the manifest {@code fund-documents.json} under the fixtures directory and the PDFs from the
 * documents folder. Refusing a document with a missing or wrong hash, or not public, is the
 * pipeline's job: this adapter reports what the manifest says.
 */
public class FolderFundDocuments implements FundDocuments {

    private final Path manifest;
    private final Path pdfFolder;

    public FolderFundDocuments(Path fixturesDir, Path documentsFolder) {
        this.manifest = fixturesDir.resolve("fund-documents.json");
        this.pdfFolder = documentsFolder;
    }

    @Override
    public List<FundDocument> documents(Isin anyShareClassOfTheFund) {
        List<FundDocument> all = all();
        // A fund's documents: those naming the class, plus any document sharing a class with one of them
        // (the prospectus names every class of the fund, a KID only its own).
        List<Isin> fundClasses = all.stream()
                .filter(d -> d.isins().contains(anyShareClassOfTheFund))
                .flatMap(d -> d.isins().stream())
                .distinct()
                .toList();
        return all.stream().filter(d -> d.isins().stream().anyMatch(fundClasses::contains)).toList();
    }

    @Override
    public byte[] content(DocumentId id) {
        FundDocument document = all().stream()
                .filter(d -> d.documentId().equals(id))
                .findFirst()
                .orElseThrow(() -> new DocumentUnavailable(id, "not in the manifest", null));
        if (document.file() == null) {
            throw new DocumentUnavailable(id, "the manifest names no file", null);
        }
        Path pdf = pdfFolder.resolve(document.file()).normalize();
        if (!pdf.startsWith(pdfFolder.normalize())) {
            throw new DocumentUnavailable(id, "the file leaves the documents folder", null);
        }
        try {
            return Files.readAllBytes(pdf);
        } catch (IOException e) {
            throw new DocumentUnavailable(id, "cannot read " + document.file(), e);
        }
    }

    private List<FundDocument> all() {
        try {
            List<FundDocument> documents = new ArrayList<>();
            for (JsonNode node : Json.readList(manifest)) {
                documents.add(document(node));
            }
            return List.copyOf(documents);
        } catch (IOException | RuntimeException e) {
            throw new FundDataUnavailable("Cannot read " + manifest.getFileName(), e);
        }
    }

    /** A private placement memorandum is confidential whatever the manifest says. */
    private static Confidentiality confidentiality(JsonNode node, FundDocument.DocumentType type) {
        return type == FundDocument.DocumentType.PPM
                ? Confidentiality.CONFIDENTIAL
                : Json.enumOrNull(node, "confidentiality", Confidentiality.class);
    }

    private static FundDocument document(JsonNode node) {
        FundDocument.DocumentType type = Json.enumOrNull(node, "type", FundDocument.DocumentType.class);
        return new FundDocument(
                new DocumentId(requiredString(node, "documentId")),
                java.util.Objects.requireNonNull(strings(node, "isins"), "Missing required field: isins").stream().map(Isin::new).toList(),
                type,
                string(node, "language"),
                editionDate(node, "fmdEditionDate"),
                editionDate(node, "pdfEditionDate"),
                confidentiality(node, type),
                string(node, "contentSha256"),
                string(node, "sourceUrl"),
                string(node, "file"));
    }
}
