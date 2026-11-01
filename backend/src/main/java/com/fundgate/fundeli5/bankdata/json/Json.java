package com.fundgate.fundeli5.bankdata.json;

import com.fundgate.fundeli5.bankdata.BankCode;
import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.bankdata.Isin;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reading helpers for the JSON of fixtures and Rule Cards. Absent and {@code null} both read as
 * {@code null} (unknown); a malformed value fails loudly. Lives with the adapters: the domain
 * packages import no Jackson.
 */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    public static JsonNode read(Path file) throws IOException {
        return MAPPER.readTree(Files.readString(file));
    }

    /** The child, or {@code null} when absent or JSON null. */
    public static JsonNode child(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? null : child;
    }

    public static String string(JsonNode node, String field) {
        JsonNode child = child(node, field);
        return child == null ? null : child.asString();
    }

    public static String requiredString(JsonNode node, String field) {
        String value = string(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    public static Boolean bool(JsonNode node, String field) {
        JsonNode child = child(node, field);
        return child == null ? null : child.asBoolean();
    }

    public static Integer integer(JsonNode node, String field) {
        JsonNode child = child(node, field);
        return child == null ? null : child.asInt();
    }

    public static BigDecimal decimal(JsonNode node, String field) {
        JsonNode child = child(node, field);
        return child == null ? null : child.decimalValue();
    }

    public static LocalDate date(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    public static Instant instant(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : Instant.parse(value);
    }

    public static Country country(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : new Country(value);
    }

    public static Currency currency(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : new Currency(value);
    }

    public static Isin isin(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : new Isin(value);
    }

    public static EditionDate editionDate(JsonNode node, String field) {
        String value = string(node, field);
        return value == null ? null : new EditionDate(value);
    }

    public static BankCode bankCode(JsonNode node, String field) {
        JsonNode child = child(node, field);
        return child == null ? null : new BankCode(requiredString(child, "code"), string(child, "label"));
    }

    /** The array's items, or {@code null} when the array is absent (unknown). */
    public static List<JsonNode> array(JsonNode node, String field) {
        JsonNode child = child(node, field);
        if (child == null) {
            return null;
        }
        List<JsonNode> items = new ArrayList<>();
        child.forEach(items::add);
        return items;
    }

    /** The array's strings, or {@code null} when the array is absent (unknown). */
    public static List<String> strings(JsonNode node, String field) {
        List<JsonNode> items = array(node, field);
        return items == null ? null : items.stream().map(JsonNode::asString).toList();
    }

    /** A lower-snake-case value as its enum constant; {@code null} when absent or matching none. */
    public static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    public static <E extends Enum<E>> E enumOrNull(JsonNode node, String field, Class<E> type) {
        return enumOrNull(type, string(node, field));
    }

    /** As {@link #enumOrNull} but a value that matches none fails loudly. */
    public static <E extends Enum<E>> E requiredEnum(JsonNode node, String field, Class<E> type) {
        String value = requiredString(node, field);
        E result = enumOrNull(type, value);
        if (result == null) {
            throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " in " + field + ": " + value);
        }
        return result;
    }
}
