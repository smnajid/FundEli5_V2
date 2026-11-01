package com.fundgate.fundeli5.llm;

/** The JSON shape of one recording file: {@code {"request": .., "response": ..}}. */
public record RecordingFile(LlmRequest request, LlmResponse response) {
}
