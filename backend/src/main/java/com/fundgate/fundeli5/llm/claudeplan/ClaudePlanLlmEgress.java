package com.fundgate.fundeli5.llm.claudeplan;

import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Calls a model through the Claude plan: {@code claude -p --output-format json [--json-schema ..]}
 * as a subprocess, the prompt on stdin (never on the command line, so it does not show in a process
 * list; the system prompt is a static instruction and does). Nothing is logged or persisted here: the prompt belongs to the Check Record (ADR 0004).
 * With a schema the answer is the {@code structured_output} JSON, else the {@code result} text.
 */
public class ClaudePlanLlmEgress implements LlmEgress {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final String executable;
    private final Optional<String> schema;
    private final Duration timeout;
    /** Neutral working directory: the model must not see this project's files or CLAUDE.md. */
    private final Path workDir = Path.of(System.getProperty("java.io.tmpdir"));

    public ClaudePlanLlmEgress(String executable, Optional<Path> schemaFile, Duration timeout) {
        this.executable = executable;
        this.schema = schemaFile.map(ClaudePlanLlmEgress::read);
        this.timeout = timeout;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the JSON schema " + file, e);
        }
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<String> command = new ArrayList<>(List.of(executable, "-p", "--output-format", "json",
                "--model", request.model(), "--system-prompt", request.system(),
                "--no-session-persistence", "--tools", "", "--setting-sources", "user",
                "--disable-slash-commands"));
        schema.ifPresent(s -> {
            command.add("--json-schema");
            command.add(s);
        });
        Process process;
        try {
            process = new ProcessBuilder(command).directory(workDir.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException e) {
            throw new ClaudePlanException("Cannot start the claude command " + executable, e);
        }
        CompletableFuture<byte[]> stdout = CompletableFuture.supplyAsync(
                () -> drain(process.getInputStream()), Executors.newVirtualThreadPerTaskExecutor());
        CompletableFuture.runAsync(() -> {
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(request.prompt().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                // the command may exit before reading stdin; the exit code below tells why
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                kill(process);
                stdout.cancel(true);
                throw new ClaudePlanException("The claude command timed out after " + timeout);
            }
            if (process.exitValue() != 0) {
                throw new ClaudePlanException("The claude command failed with exit " + process.exitValue());
            }
            return parse(stdout.get(5, TimeUnit.SECONDS), schema.isPresent());
        } catch (InterruptedException e) {
            kill(process);
            stdout.cancel(true);
            Thread.currentThread().interrupt();
            throw new ClaudePlanException("Interrupted while waiting for the claude command", e);
        } catch (ExecutionException | TimeoutException e) {
            kill(process);
            stdout.cancel(true);
            throw new ClaudePlanException("Cannot read the claude command output", e);
        }
    }

    private static byte[] drain(InputStream in) {
        try (in) {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static LlmResponse parse(byte[] output, boolean schemaRequired) {
        JsonNode root;
        try {
            root = MAPPER.readTree(output);
        } catch (RuntimeException e) {
            throw new ClaudePlanException("The claude command did not print JSON");
        }
        if (root == null || !root.isObject()) {
            throw new ClaudePlanException("The claude command did not print a JSON object");
        }
        if (root.path("is_error").asBoolean(false)) {
            throw new ClaudePlanException("The claude command reported an error");
        }
        JsonNode structured = root.get("structured_output");
        if (structured != null && !structured.isNull()) {
            return new LlmResponse(structured.toString());
        }
        if (schemaRequired) {
            throw new ClaudePlanException("The claude command printed no structured output for the schema");
        }
        JsonNode result = root.get("result");
        if (result == null || !result.isString()) {
            throw new ClaudePlanException("The claude command printed no result");
        }
        return new LlmResponse(result.asString());
    }
}
