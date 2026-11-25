package dev.mechanium.plugin;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialItemFilterPolicyTest {
    @Test
    void emptyFilterAcceptsAllRegisteredCustomItems() {
        assertTrue(IndustrialItemFilterPolicy.accepts(Set.of(), "mechanium:steel_plate"));
    }

    @Test
    void nonEmptyFilterOnlyAcceptsConfiguredIds() {
        Set<String> filter = Set.of("mechanium:steel_plate", "mechanium:copper_coil");
        assertTrue(IndustrialItemFilterPolicy.accepts(filter, "MECHANIUM:Steel_Plate"));
        assertFalse(IndustrialItemFilterPolicy.accepts(filter, "mechanium:industrial_resin"));
    }

    @Test
    void togglingAddsAndRemovesAnAcceptedItem() {
        Set<String> first = IndustrialItemFilterPolicy.toggle(Set.of(), "mechanium:steel_plate");
        assertEquals(Set.of("mechanium:steel_plate"), first);
        assertEquals(Set.of(), IndustrialItemFilterPolicy.toggle(first, "MECHANIUM:Steel_Plate"));
    }

    @Test
    void filtersCanBeStoredAndRestoredDeterministically() {
        Set<String> source = Set.of("mechanium:zinc_ingot", "mechanium:iron_plate",
                "mechanium:copper_coil");
        String encoded = IndustrialItemFilterPolicy.encode(source);
        assertEquals("mechanium:copper_coil,mechanium:iron_plate,mechanium:zinc_ingot", encoded);
        assertEquals(source, IndustrialItemFilterPolicy.decode(encoded));
        assertEquals(Set.of(), IndustrialItemFilterPolicy.decode(""));
    }

    @Test
    void rejectsInvalidFilterInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialItemFilterPolicy.accepts(null, "mechanium:steel_plate"));
        assertThrows(IllegalArgumentException.class,
                () -> IndustrialItemFilterPolicy.toggle(Set.of(), "not an item"));
        assertFalse(IndustrialItemFilterPolicy.accepts(Set.of("mechanium:steel_plate"), null));
    }
}
