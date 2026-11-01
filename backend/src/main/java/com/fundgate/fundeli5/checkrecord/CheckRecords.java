package com.fundgate.fundeli5.checkrecord;

import com.fundgate.fundeli5.eligibility.CheckRecord;
import java.util.List;

/** Append-only store of Check Records. There is no update and no delete. */
public interface CheckRecords {

    void append(CheckRecord record);

    /** Oldest first. */
    List<CheckRecord> all();
}
