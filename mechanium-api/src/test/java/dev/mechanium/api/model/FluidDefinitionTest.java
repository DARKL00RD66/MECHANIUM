package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FluidDefinitionTest {
    @Test
    void normalizesFluidAndContainerIdentifiers() {
        FluidDefinition fluid = new FluidDefinition(
                " Purified_Water ", "Água purificada", "basic_water_filtration",
                List.of(
                        new FluidContainerDefinition(" Purified_Water ", 250, true, true),
                        new FluidContainerDefinition("Purified_Water_Cell", 1_000, true, false)));

        assertEquals("purified_water", fluid.id());
        assertEquals("purified_water", fluid.containers().getFirst().itemId());
        assertEquals(1_000, fluid.containers().getLast().amountMb());
        assertTrue(fluid.containers().getFirst().canExtract());
        assertFalse(fluid.containers().getLast().canExtract());
    }

    @Test
    void rejectsDuplicateContainerItems() {
        assertThrows(IllegalArgumentException.class, () -> new FluidDefinition(
                "water", "Water", null,
                List.of(new FluidContainerDefinition("water_item", 250, true, true),
                        new FluidContainerDefinition("WATER_ITEM", 500, true, false))));
    }

    @Test
    void requiresBothFillAndExtractionContainers() {
        assertThrows(IllegalArgumentException.class, () -> new FluidDefinition(
                "water", "Water", null,
                List.of(new FluidContainerDefinition("input", 250, true, false))));
        assertThrows(IllegalArgumentException.class, () -> new FluidDefinition(
                "water", "Water", null,
                List.of(new FluidContainerDefinition("output", 250, false, true))));
    }

    @Test
    void validatesContainerVolumesAndDirections() {
        assertThrows(IllegalArgumentException.class,
                () -> new FluidContainerDefinition("water_item", 100, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new FluidContainerDefinition("water_item", 0, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new FluidContainerDefinition("water_item", 250, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new FluidContainerDefinition("invalid id", 250, true, true));
    }

    @Test
    void rejectsMissingFluidAndTechnologyIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new FluidDefinition(
                "bad id", "Water", null,
                List.of(new FluidContainerDefinition("water_item", 250, true, true))));
        assertThrows(IllegalArgumentException.class, () -> new FluidDefinition(
                "water", "", null,
                List.of(new FluidContainerDefinition("water_item", 250, true, true))));
    }
}
