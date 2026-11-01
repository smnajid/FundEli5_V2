package com.fundgate.fundeli5.checkrecord;

import com.fundgate.fundeli5.eligibility.CheckRecord;
import java.util.List;
import java.util.UUID;

/** Append-only store of Check Records. There is no update and no delete. */
public interface CheckRecords {

    void append(CheckRecord record);

    /** Oldest first. */
    List<CheckRecord> all();

    /** Appends a correction beside an existing record; the record stays as it was. */
    void appendCorrection(Correction correction);

    /** The corrections of one record, oldest first. */
    List<Correction> correctionsOf(UUID recordId);
}
