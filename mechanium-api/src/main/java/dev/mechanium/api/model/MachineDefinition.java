package dev.mechanium.api.model;

import org.bukkit.Material;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Definition of a machine process. The current Bukkit runtime uses a dispenser,
 * with six input slots and three output slots. The legacy constructor and accessors
 * remain available for existing modules and addons.
 */
public record MachineDefinition(String id, String displayName, String machineItemId,
                                String blockMaterial, List<MachineIngredient> inputs,
                                List<MachineProduct> outputs, int processingTicks,
                                String requiredTechnologyId) {
    public MachineDefinition {
        id = normalize(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName cannot be empty");

        machineItemId = normalize(machineItemId, "machineItemId");
        blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial").trim().toUpperCase(Locale.ROOT);
        Material block = Material.matchMaterial(blockMaterial);
        if (block == null) {
            throw new IllegalArgumentException("blockMaterial must be a valid placeable block: " + blockMaterial);
        }
        if (!"DISPENSER".equals(block.name())) {
            throw new IllegalArgumentException("Current machine runtime supports DISPENSER blocks only");
        }

        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        if (inputs.isEmpty() || inputs.size() > 6) {
            throw new IllegalArgumentException("Machine processes require 1..6 input types");
        }
        if (outputs.isEmpty() || outputs.size() > 3) {
            throw new IllegalArgumentException("Machine processes require 1..3 output types");
        }
        Set<String> inputIds = new HashSet<>();
        for (MachineIngredient input : inputs) {
            Objects.requireNonNull(input, "input");
            if (!inputIds.add(input.itemId())) {
                throw new IllegalArgumentException("Duplicate machine input type: " + input.itemId());
            }
        }
        Set<String> outputIds = new HashSet<>();
        for (MachineProduct output : outputs) {
            Objects.requireNonNull(output, "output");
            if (!outputIds.add(output.itemId())) {
                throw new IllegalArgumentException("Duplicate machine output type: " + output.itemId());
            }
        }
        if (processingTicks < 20 || processingTicks > 72_000) {
            throw new IllegalArgumentException("processingTicks must be between 20 and 72000");
        }
        requiredTechnologyId = normalize(requiredTechnologyId, "requiredTechnologyId");
    }

    /**
     * Backwards-compatible constructor for existing single-input/single-output definitions.
     */
    public MachineDefinition(String id, String displayName, String machineItemId,
                             String blockMaterial, String inputId, int inputAmount,
                             String outputItemId, int outputAmount, int processingTicks,
                             String requiredTechnologyId) {
        this(id, displayName, machineItemId, blockMaterial,
                List.of(new MachineIngredient(inputId, inputAmount)),
                List.of(new MachineProduct(outputItemId, outputAmount)),
                processingTicks, requiredTechnologyId);
    }

    /** Legacy accessor: returns the first input type. */
    public String inputId() {
        return inputs.getFirst().itemId();
    }

    /** Legacy accessor: returns the first input amount. */
    public int inputAmount() {
        return inputs.getFirst().amount();
    }

    /** Legacy accessor: returns the first output item ID. */
    public String outputItemId() {
        return outputs.getFirst().itemId();
    }

    /** Legacy accessor: returns the first output amount. */
    public int outputAmount() {
        return outputs.getFirst().amount();
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
