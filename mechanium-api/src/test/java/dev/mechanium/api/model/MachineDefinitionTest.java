package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MachineDefinitionTest {
    @Test
    void legacyConstructorAndAccessorsRemainCompatible() {
        MachineDefinition machine = new MachineDefinition(
                " Mechanical_Press ", "Mechanical Press", "Mechanical_Press",
                "dispenser", "iron_ingot", 2, "mechanical_parts", 1, 100,
                "basic_mechanics");

        assertEquals("mechanical_press", machine.id());
        assertEquals("mechanical_press", machine.machineItemId());
        assertEquals("DISPENSER", machine.blockMaterial());
        assertEquals("IRON_INGOT", machine.inputId());
        assertEquals(2, machine.inputAmount());
        assertEquals("mechanical_parts", machine.outputItemId());
        assertEquals(1, machine.outputAmount());
        assertEquals(List.of(new MachineIngredient("IRON_INGOT", 2)), machine.inputs());
        assertEquals(List.of(new MachineProduct("mechanical_parts", 1)), machine.outputs());
    }

    @Test
    void acceptsMultipleInputsAndOutputs() {
        MachineDefinition machine = new MachineDefinition(
                "industrial_mixer", "Misturador industrial", "industrial_mixer", "DISPENSER",
                List.of(new MachineIngredient("IRON_INGOT", 2),
                        new MachineIngredient("COPPER_INGOT", 1),
                        new MachineIngredient("mechanium:industrial_resin", 3)),
                List.of(new MachineProduct("mechanical_parts", 2),
                        new MachineProduct("copper_coil", 1)),
                240, "industrial_chemistry");

        assertEquals(3, machine.inputs().size());
        assertEquals(2, machine.outputs().size());
        assertEquals("IRON_INGOT", machine.inputId());
        assertEquals("mechanical_parts", machine.outputItemId());
    }

    @Test
    void machineIngredientsNormalizeVanillaAndCustomIds() {
        assertEquals("IRON_INGOT", new MachineIngredient("iron_ingot", 2).itemId());
        assertEquals("mechanium:raw_material",
                new MachineIngredient("MECHANIUM:Raw_Material", 1).itemId());
        assertEquals("COPPER_INGOT", new MachineIngredient("COPPER_INGOT", 1).itemId());
    }

    @Test
    void rejectsInvalidBlockInputAndProcessingTimes() {
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "CHEST", "IRON_INGOT", 1,
                "processed_material", 1, 20, "basic_mechanics"));
        assertThrows(IllegalArgumentException.class, () -> new MachineIngredient("NOT_A_MATERIAL", 1));
        assertThrows(IllegalArgumentException.class, () -> new MachineIngredient("AIR", 1));
        assertThrows(IllegalArgumentException.class, () -> new MachineIngredient("IRON_INGOT", 0));
        assertThrows(IllegalArgumentException.class, () -> new MachineProduct("invalid id", 1));
        assertThrows(IllegalArgumentException.class, () -> new MachineProduct("processed_material", 0));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "DISPENSER", "IRON_INGOT", 1,
                "processed_material", 1, 1, "basic_mechanics"));
    }

    @Test
    void restrictsInputAndOutputCountsToAvailableMachineBuffers() {
        List<MachineIngredient> sevenInputs = List.of(
                new MachineIngredient("IRON_INGOT", 1),
                new MachineIngredient("COPPER_INGOT", 1),
                new MachineIngredient("GOLD_INGOT", 1),
                new MachineIngredient("DIAMOND", 1),
                new MachineIngredient("EMERALD", 1),
                new MachineIngredient("COAL", 1),
                new MachineIngredient("REDSTONE", 1));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "too_many_inputs", "Too many inputs", "machine_item", "DISPENSER",
                sevenInputs, List.of(new MachineProduct("processed_material", 1)),
                100, "basic_mechanics"));

        List<MachineProduct> fourOutputs = List.of(
                new MachineProduct("processed_material", 1),
                new MachineProduct("mechanical_parts", 1),
                new MachineProduct("copper_coil", 1),
                new MachineProduct("industrial_resin", 1));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "too_many_outputs", "Too many outputs", "machine_item", "DISPENSER",
                List.of(new MachineIngredient("IRON_INGOT", 1)), fourOutputs,
                100, "basic_mechanics"));
    }

    @Test
    void rejectsDuplicateInputAndOutputTypes() {
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "duplicate_inputs", "Duplicate inputs", "machine_item", "DISPENSER",
                List.of(new MachineIngredient("IRON_INGOT", 1),
                        new MachineIngredient("iron_ingot", 2)),
                List.of(new MachineProduct("processed_material", 1)), 100, "basic_mechanics"));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "duplicate_outputs", "Duplicate outputs", "machine_item", "DISPENSER",
                List.of(new MachineIngredient("IRON_INGOT", 1)),
                List.of(new MachineProduct("processed_material", 1),
                        new MachineProduct("PROCESSED_MATERIAL", 2)),
                100, "basic_mechanics"));
    }
}
