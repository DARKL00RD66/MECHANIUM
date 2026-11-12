package dev.mechanium.api.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;

/** Definition of a single-input/single-output machine hosted in a dispenser block. */
public record MachineDefinition(String id, String displayName, String machineItemId,
                                String blockMaterial, String inputId, int inputAmount,
                                String outputItemId, int outputAmount, int processingTicks,
                                String requiredTechnologyId) {
    public MachineDefinition {
        id = normalize(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName cannot be empty");

        machineItemId = normalize(machineItemId, "machineItemId");
        blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial").trim().toUpperCase(Locale.ROOT);
        Material block = Material.matchMaterial(blockMaterial);
        if (block == null || block.isAir() || !block.isBlock()) {
            throw new IllegalArgumentException("blockMaterial must be a valid placeable block: " + blockMaterial);
        }
        if (!"DISPENSER".equals(blockMaterial)) {
            throw new IllegalArgumentException("Current machine runtime supports DISPENSER blocks only");
        }

        inputId = normalizeInput(inputId);
        if (inputAmount < 1 || inputAmount > 99) throw new IllegalArgumentException("inputAmount must be 1..99");
        outputItemId = normalize(outputItemId, "outputItemId");
        if (outputAmount < 1 || outputAmount > 99) throw new IllegalArgumentException("outputAmount must be 1..99");
        if (processingTicks < 20 || processingTicks > 72_000) {
            throw new IllegalArgumentException("processingTicks must be between 20 and 72000");
        }
        requiredTechnologyId = normalize(requiredTechnologyId, "requiredTechnologyId");
    }

    private static String normalizeInput(String value) {
        Objects.requireNonNull(value, "inputId");
        String input = value.trim();
        if (input.regionMatches(true, 0, "mechanium:", 0, 10)) {
            return "mechanium:" + normalize(input.substring(10), "custom input");
        }
        input = input.toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(input);
        if (material == null || material.isAir() || !material.isItem()) {
            throw new IllegalArgumentException("inputId must be an item material or mechanium:<id>: " + input);
        }
        return input;
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
