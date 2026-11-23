package dev.mechanium.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaRecipeCatalogTest {

    @Test
    void enchantingTableBelongsToMythicEra() {
        assertEquals("mythic_age",
                VanillaRecipeCatalog.recommendedEraId("enchanting_table", Material.ENCHANTING_TABLE));
    }

    @Test
    void industrialMachinesBelongToIndustrialRevolution() {
        assertEquals("industrial_revolution",
                VanillaRecipeCatalog.recommendedEraId("hopper", Material.HOPPER));
        assertEquals("industrial_revolution",
                VanillaRecipeCatalog.recommendedEraId("piston", Material.PISTON));
    }

    @Test
    void redstoneLogicBelongsToElectrification() {
        assertEquals("electrification",
                VanillaRecipeCatalog.recommendedEraId("redstone_lamp", Material.REDSTONE_LAMP));
        assertEquals("electrification",
                VanillaRecipeCatalog.recommendedEraId("repeater", Material.REPEATER));
    }

    @Test
    void digitalAutomationBelongsToDigitalAge() {
        assertEquals("digital_age",
                VanillaRecipeCatalog.recommendedEraId("crafter", Material.CRAFTER));
    }

    @Test
    void basicUnclassifiedRecipesDefaultToAncient() {
        assertEquals("ancient",
                VanillaRecipeCatalog.recommendedEraId("oak_planks", Material.OAK_PLANKS));
    }
}
