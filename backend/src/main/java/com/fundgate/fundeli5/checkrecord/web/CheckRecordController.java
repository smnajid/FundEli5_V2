package com.fundgate.fundeli5.checkrecord.web;

import com.fundgate.fundeli5.Runtimes;
import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.checkrecord.Correction;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Writes the dealer's corrections beside a Check Record. Nothing here changes or deletes a record. */
@RestController
@Profile("!" + Runtimes.PIPELINE)
@RequestMapping("/api/check-records")
class CheckRecordController {

    private final CheckRecords checkRecords;

    CheckRecordController(CheckRecords checkRecords) {
        this.checkRecords = checkRecords;
    }

    @PostMapping("/{id}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    void correct(@PathVariable UUID id, @RequestBody CorrectionBody body) {
        if (body.field() == null || body.parsedValue() == null || body.dealerValue() == null
                || body.field().isBlank() || body.dealerValue().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "field, parsedValue and dealerValue are required");
        }
        String by = body.by() == null || body.by().isBlank() ? "fund-dealer" : body.by();
        try {
            checkRecords.appendCorrection(new Correction(id, body.field(), body.parsedValue(), body.dealerValue(), by,
                    Instant.now()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such Check Record");
        }
    }

    record CorrectionBody(String field, String parsedValue, String dealerValue, String by) {
    }
}
