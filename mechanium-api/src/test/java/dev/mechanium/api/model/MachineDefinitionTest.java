package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MachineDefinitionTest {
    @Test
    void normalizesMachineAndCustomInputIds() {
        MachineDefinition machine = new MachineDefinition(
                " Mechanical_Press ", "Mechanical Press", "Mechanical_Press",
                "dispenser", "iron_ingot", 2, "mechanical_parts", 1, 100,
                "basic_mechanics");
        assertEquals("mechanical_press", machine.id());
        assertEquals("mechanical_press", machine.machineItemId());
        assertEquals("DISPENSER", machine.blockMaterial());
        assertEquals("IRON_INGOT", machine.inputId());
    }

    @Test
    void acceptsCustomInputs() {
        MachineDefinition machine = new MachineDefinition(
                "press", "Press", "press_item", "DISPENSER",
                "mechanium:raw_material", 1, "processed_material", 1, 20,
                "basic_mechanics");
        assertEquals("mechanium:raw_material", machine.inputId());
    }

    @Test
    void rejectsInvalidBlockInputAndProcessingTimes() {
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "CHEST", "IRON_INGOT", 1,
                "processed_material", 1, 20, "basic_mechanics"));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "DISPENSER", "NOT_A_MATERIAL", 1,
                "processed_material", 1, 20, "basic_mechanics"));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "DISPENSER", "IRON_INGOT", 1,
                "processed_material", 1, 1, "basic_mechanics"));
        assertThrows(IllegalArgumentException.class, () -> new MachineDefinition(
                "press", "Press", "press_item", "DISPENSER", "IRON_INGOT", 0,
                "processed_material", 1, 20, "basic_mechanics"));
    }
}
