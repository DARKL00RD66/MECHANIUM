package dev.mechanium.api.model;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** A typed fluid registered by an official module or external addon. */
public record FluidDefinition(String id, String displayName, String requiredTechnologyId,
                              List<FluidContainerDefinition> containers) {
    public FluidDefinition {
        id = normalize(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName cannot be empty");

        requiredTechnologyId = requiredTechnologyId == null || requiredTechnologyId.isBlank()
                ? null : normalize(requiredTechnologyId, "requiredTechnologyId");
        containers = List.copyOf(Objects.requireNonNull(containers, "containers"));
        if (containers.isEmpty()) throw new IllegalArgumentException("A fluid needs at least one container definition");

        Set<String> itemIds = new HashSet<>();
        boolean hasFill = false;
        boolean hasExtract = false;
        for (FluidContainerDefinition container : containers) {
            Objects.requireNonNull(container, "container");
            if (!itemIds.add(container.itemId())) {
                throw new IllegalArgumentException("Duplicate fluid container item: " + container.itemId());
            }
            hasFill |= container.canFill();
            hasExtract |= container.canExtract();
        }
        if (!hasFill || !hasExtract) {
            throw new IllegalArgumentException("A fluid needs at least one fill container and one extraction container");
        }
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
