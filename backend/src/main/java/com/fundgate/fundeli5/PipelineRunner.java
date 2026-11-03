package com.fundgate.fundeli5;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecardpipeline.CardDrafter;
import com.fundgate.fundeli5.rulecardpipeline.DraftCardWriter;
import com.fundgate.fundeli5.rulecardpipeline.SourceReader;
import com.fundgate.fundeli5.rulecardpipeline.SourceReading;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.folder.FolderSourcePages;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Command-line entry of the Rule Card pipeline: {@code --isin=<ISIN>} (repeatable) names a Share Class of
 * each fund to draft. Reads the fund's documents, drafts a Draft Rule Card and writes it to the output folder.
 */
class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final SourceReader reader;
    private final CardDrafter drafter;
    private final DraftCardWriter writer;
    private final java.nio.file.Path pagesOut;

    PipelineRunner(SourceReader reader, CardDrafter drafter, DraftCardWriter writer, java.nio.file.Path pagesOut) {
        this.pagesOut = pagesOut;
        this.reader = reader;
        this.drafter = drafter;
        this.writer = writer;
    }

    @Override
    public void run(ApplicationArguments args) {
        var isins = args.getOptionValues("isin");
        if (isins == null || isins.isEmpty()) {
            log.info("Nothing to do: pass --isin=<ISIN> for each fund to read");
            return;
        }
        for (String isin : isins) {
            SourceReading reading = reader.read(new Isin(isin));
            reading.sources().forEach(s -> log.info("{}: {} pages read", s.document().documentId(), s.pages().size()));
            reading.refusals().forEach(r -> log.warn("{}", r.getMessage()));
            if (!reading.canDraft()) {
                log.warn("No Rule Card can be drafted for {}", isin);
                continue;
            }
            CardDrafter.Drafted drafted = drafter.draft(reading);
            log.info("Draft written to {} ({} rules); possibly missed: {}", writer.write(drafted.card()),
                    drafted.card().rules().size(), drafted.possiblyMissed());
            log.info("Cited pages written to {}", writePages(drafted.card(), reading));
        }
    }

    /** The text of each cited page, kept next to the Draft so the validation screen needs no Fund Document. */
    private java.nio.file.Path writePages(RuleCard card, SourceReading reading) {
        Map<String, Map<Integer, String>> cited = new TreeMap<>();
        for (Rule rule : card.rules()) {
            for (Citation c : rule.citations()) {
                reading.sources().stream().filter(s -> s.document().documentId().value().equals(c.document())).findFirst()
                        .filter(s -> {
                            boolean inRange = c.page() >= 1 && c.page() <= s.pages().size();
                            if (!inRange) {
                                log.warn("Page text not kept: {} has no page {}", c.document(), c.page());
                            }
                            return inRange;
                        })
                        .ifPresent(s -> cited.computeIfAbsent(c.document(), d -> new TreeMap<>())
                                .put(c.page(), s.pages().get(c.page() - 1)));
            }
        }
        return FolderSourcePages.write(pagesOut, card.fundName(), card.version(), cited);
    }
}
