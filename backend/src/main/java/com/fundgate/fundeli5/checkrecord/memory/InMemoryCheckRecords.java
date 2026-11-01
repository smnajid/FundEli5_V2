package com.fundgate.fundeli5.checkrecord.memory;

import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.eligibility.CheckRecord;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCheckRecords implements CheckRecords {

    private final List<CheckRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void append(CheckRecord record) {
        records.add(record);
    }

    @Override
    public List<CheckRecord> all() {
        return List.copyOf(records);
    }
}
