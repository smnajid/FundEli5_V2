package com.fundgate.fundeli5.checkrecord.memory;

import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.checkrecord.Correction;
import com.fundgate.fundeli5.eligibility.CheckRecord;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCheckRecords implements CheckRecords {

    private final List<CheckRecord> records = new CopyOnWriteArrayList<>();
    private final List<Correction> corrections = new CopyOnWriteArrayList<>();

    @Override
    public void append(CheckRecord record) {
        records.add(record);
    }

    @Override
    public List<CheckRecord> all() {
        return List.copyOf(records);
    }

    @Override
    public void appendCorrection(Correction correction) {
        if (records.stream().noneMatch(r -> r.id().equals(correction.recordId()))) {
            throw new IllegalArgumentException("No Check Record " + correction.recordId());
        }
        corrections.add(correction);
    }

    @Override
    public List<Correction> correctionsOf(UUID recordId) {
        return corrections.stream().filter(c -> c.recordId().equals(recordId)).toList();
    }
}
