package dev.mechanium.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemDefinitionTest {
    @Test
    void normalizesIdentifiersAndMaterialNames() {
        ItemDefinition item = new ItemDefinition(
                "  Mechanical_Parts ", "Peças mecânicas", "iron_ingot",
                List.of("&7Componente"), 0, 64, "industrial_components", null);

        assertEquals("mechanical_parts", item.id());
        assertEquals("IRON_INGOT", item.material());
        assertEquals("industrial_components", item.categoryId());
    }

    @Test
    void rejectsAirAsAnItemMaterial() {
        assertThrows(IllegalArgumentException.class, () -> new ItemDefinition(
                "empty_item", "Item vazio", "AIR", List.of(), 0, 64, "general", null));
    }

    @Test
    void rejectsStackSizesOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> new ItemDefinition(
                "oversized_item", "Item inválido", "IRON_INGOT", List.of(), 0, 100, "general", null));
    }
}
