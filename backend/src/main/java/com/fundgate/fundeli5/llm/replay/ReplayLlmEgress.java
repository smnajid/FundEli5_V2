package com.fundgate.fundeli5.llm.replay;

import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import com.fundgate.fundeli5.llm.RecordingFile;
import com.fundgate.fundeli5.llm.UnrecordedLlmRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serves recorded answers, one JSON file per recording, matched on the whole request. A file that
 * does not read exactly as {@code {"request": .., "response": ..}} fails at construction: an
 * unknown or missing property would otherwise load as null and never match.
 */
public class ReplayLlmEgress implements LlmEgress {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .build();

    private final Map<LlmRequest, LlmResponse> recordings;

    public ReplayLlmEgress(Path folder) {
        Map<LlmRequest, LlmResponse> loaded = new HashMap<>();
        List<Path> files;
        try (Stream<Path> listing = Files.list(folder)) {
            files = listing.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the recordings folder " + folder, e);
        }
        for (Path file : files) {
            RecordingFile recording;
            try {
                recording = MAPPER.readValue(file.toFile(), RecordingFile.class);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Bad recording " + file.getFileName() + ": " + e.getMessage(), e);
            }
            if (recording.request() == null || recording.response() == null) {
                throw new IllegalStateException("Bad recording " + file.getFileName() + ": needs request and response");
            }
            if (loaded.putIfAbsent(recording.request(), recording.response()) != null) {
                throw new IllegalStateException("Bad recording " + file.getFileName() + ": request already recorded");
            }
        }
        this.recordings = Map.copyOf(loaded);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmResponse response = recordings.get(request);
        if (response == null) {
            throw new UnrecordedLlmRequest(request);
        }
        return response;
    }
}
