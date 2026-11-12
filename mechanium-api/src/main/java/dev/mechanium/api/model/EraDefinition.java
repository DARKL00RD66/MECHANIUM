package dev.mechanium.api.model;

import java.util.Locale;
import java.util.Objects;

/** Immutable historical era definition. Lower order means earlier history. */
public record EraDefinition(String id, String displayName, int order) {
    public EraDefinition {
        id = normalize(id);
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName cannot be empty");
        if (order < 0) throw new IllegalArgumentException("order cannot be negative");
    }
    private static String normalize(String value) {
        Objects.requireNonNull(value, "id");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_-]{1,63}"))
            throw new IllegalArgumentException("Invalid era id: " + value);
        return normalized;
    }
}