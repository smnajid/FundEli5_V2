package com.fundgate.fundeli5.specs;

import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Wraps the app's egress in the specification: remembers every request, and can play an unreachable model. */
public class SpyLlmEgress implements LlmEgress {

    private final LlmEgress delegate;
    private final List<LlmRequest> sent = new CopyOnWriteArrayList<>();
    private volatile boolean unreachable;

    public SpyLlmEgress(LlmEgress delegate) {
        this.delegate = delegate;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        sent.add(request);
        if (unreachable) {
            throw new IllegalStateException("The model cannot be reached");
        }
        return delegate.complete(request);
    }

    public List<LlmRequest> sent() {
        return List.copyOf(sent);
    }

    public void unreachable(boolean unreachable) {
        this.unreachable = unreachable;
    }

    public void reset() {
        sent.clear();
        unreachable = false;
    }
}
