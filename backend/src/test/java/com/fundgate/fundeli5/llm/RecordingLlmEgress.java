package com.fundgate.fundeli5.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test-side egress that forwards to a live egress and writes each exchange as a recording the
 * replay adapter reads back. Used by the specification to record live answers.
 */
public class RecordingLlmEgress implements LlmEgress {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT).build();

    private final LlmEgress live;
    private final Path folder;

    public RecordingLlmEgress(LlmEgress live, Path folder) {
        this.live = live;
        this.folder = folder;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmResponse response = live.complete(request);
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(fileName(request)),
                    MAPPER.writeValueAsString(new RecordingFile(request, response)) + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the recording", e);
        }
        return response;
    }

    /** Same request, same file: re-recording replaces instead of duplicating. */
    private static String fileName(LlmRequest request) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String part : new String[] {request.model(), request.system(), request.prompt()}) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) ':');
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest(), 0, 12) + ".json";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
