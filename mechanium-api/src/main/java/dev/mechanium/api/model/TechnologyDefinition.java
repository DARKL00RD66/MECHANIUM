package dev.mechanium.api.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable technology definition consumed by progression and addon APIs. */
public record TechnologyDefinition(String id, String displayName, String requiredEraId,
                                   long researchCost, List<String> prerequisites) {
    public TechnologyDefinition {
        id = normalize(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName cannot be empty");
        requiredEraId = normalize(requiredEraId, "requiredEraId");
        if (researchCost < 0) throw new IllegalArgumentException("researchCost cannot be negative");
        prerequisites = prerequisites == null ? List.of() : prerequisites.stream()
                .map(value -> normalize(value, "prerequisite")).distinct().toList();
        if (prerequisites.contains(id)) throw new IllegalArgumentException("Technology cannot require itself");
    }
    private static String normalize(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_.-]{1,95}"))
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        return normalized;
    }
}