package dev.mechanium.api.model;

import java.util.Locale;
import java.util.Objects;

/** A progression-aware mob drop rule registered by a built-in module or addon. */
public record MobDropDefinition(String id, String entityType, String resultItemId,
                                int minAmount, int maxAmount, double chance,
                                String requiredTechnologyId) {
    public MobDropDefinition {
        id = normalize(id, "id");
        entityType = Objects.requireNonNull(entityType, "entityType").trim().toUpperCase(Locale.ROOT);
        if (!entityType.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("Invalid entityType: " + entityType);
        }
        resultItemId = normalize(resultItemId, "resultItemId");
        if (minAmount < 1 || maxAmount < minAmount || maxAmount > 99) {
            throw new IllegalArgumentException("Drop amount must be between 1 and 99 and min cannot exceed max");
        }
        if (!Double.isFinite(chance) || chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("chance must be a finite value between 0 and 1");
        }
        requiredTechnologyId = requiredTechnologyId == null || requiredTechnologyId.isBlank()
                ? null : normalize(requiredTechnologyId, "requiredTechnologyId");
    }

    private static String normalize(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
        return normalized;
    }
}
