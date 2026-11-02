package com.fundgate.fundeli5;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecardpipeline.SourceReader;
import com.fundgate.fundeli5.rulecardpipeline.SourceReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Command-line entry of the Rule Card pipeline: {@code --isin=<ISIN>} (repeatable) names a Share Class of
 * each fund to read. For now it reads and reports the documents; drafting follows in later steps.
 */
class PipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final SourceReader reader;

    PipelineRunner(SourceReader reader) {
        this.reader = reader;
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
            }
        }
    }
}
