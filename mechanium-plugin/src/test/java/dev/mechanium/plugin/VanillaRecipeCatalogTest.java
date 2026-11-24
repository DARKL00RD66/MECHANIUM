package dev.mechanium.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaRecipeCatalogTest {

    @Test
    void primitiveMaterialsAndHandToolsBelongToAntiquity() {
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("oak_planks", Material.OAK_PLANKS));
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("stone_pickaxe", Material.STONE_PICKAXE));
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("copper_ingot", Material.COPPER_INGOT));
    }

    @Test
    void basicFoodAndFishingRecipesRemainInAntiquity() {
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("bread", Material.BREAD));
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("cooked_beef", Material.COOKED_BEEF));
        assertEquals("ancient", VanillaRecipeCatalog.recommendedEraId("fishing_rod", Material.FISHING_ROD));
    }

    @Test
    void ironworkingAndWorkshopToolsBelongToMedievalEra() {
        assertEquals("medieval", VanillaRecipeCatalog.recommendedEraId("iron_ingot", Material.IRON_INGOT));
        assertEquals("medieval", VanillaRecipeCatalog.recommendedEraId("anvil", Material.ANVIL));
    }

    @Test
    void navigationAndPrecisionToolsBelongToRenaissance() {
        assertEquals("renaissance", VanillaRecipeCatalog.recommendedEraId("compass", Material.COMPASS));
        assertEquals("renaissance", VanillaRecipeCatalog.recommendedEraId("spyglass", Material.SPYGLASS));
        assertEquals("renaissance", VanillaRecipeCatalog.recommendedEraId("diamond_pickaxe", Material.DIAMOND_PICKAXE));
    }

    @Test
    void factoryAndRailInfrastructureBelongToIndustrialRevolution() {
        assertEquals("industrial_revolution", VanillaRecipeCatalog.recommendedEraId("hopper", Material.HOPPER));
        assertEquals("industrial_revolution", VanillaRecipeCatalog.recommendedEraId("piston", Material.PISTON));
        assertEquals("industrial_revolution", VanillaRecipeCatalog.recommendedEraId("rail", Material.RAIL));
    }

    @Test
    void electricalInfrastructureBelongsToElectrification() {
        assertEquals("electrification", VanillaRecipeCatalog.recommendedEraId("redstone_lamp", Material.REDSTONE_LAMP));
        assertEquals("electrification", VanillaRecipeCatalog.recommendedEraId("repeater", Material.REPEATER));
        assertEquals("electrification", VanillaRecipeCatalog.recommendedEraId("lightning_rod", Material.LIGHTNING_ROD));
        assertEquals("electrification", VanillaRecipeCatalog.recommendedEraId("copper_bulb", Material.COPPER_BULB));
    }

    @Test
    void advancedMaterialsAndBlocksBelongToModernEra() {
        assertEquals("modern", VanillaRecipeCatalog.recommendedEraId("amethyst_block", Material.AMETHYST_BLOCK));
        assertEquals("modern", VanillaRecipeCatalog.recommendedEraId("cut_copper", Material.CUT_COPPER));
    }

    @Test
    void sensorResearchAndProgrammableAutomationUseLaterEras() {
        assertEquals("information_age", VanillaRecipeCatalog.recommendedEraId("sculk_sensor", Material.SCULK_SENSOR));
        assertEquals("digital_age", VanillaRecipeCatalog.recommendedEraId("observer", Material.OBSERVER));
        assertEquals("digital_age", VanillaRecipeCatalog.recommendedEraId("crafter", Material.CRAFTER));
    }

    @Test
    void endDimensionAndNetheriteEquipmentUseEndgameEras() {
        assertEquals("mythic_age", VanillaRecipeCatalog.recommendedEraId("enchanting_table", Material.ENCHANTING_TABLE));
        assertEquals("space_age", VanillaRecipeCatalog.recommendedEraId("elytra", Material.ELYTRA));
        assertEquals("speculative_future", VanillaRecipeCatalog.recommendedEraId("netherite_ingot", Material.NETHERITE_INGOT));
    }

    @Test
    void unclassifiedRecipesUseNeutralRenaissanceFallbackInsteadOfAntiquity() {
        assertEquals("renaissance", VanillaRecipeCatalog.recommendedEraId("unknown_recipe", Material.NAME_TAG));
    }

    @Test
    void legacyClassifierRemainsAvailableForOneTimeConfigMigration() {
        assertEquals("ancient", VanillaRecipeCatalog.legacyRecommendedEraId("unknown_recipe", Material.NAME_TAG));
        assertEquals("modern", VanillaRecipeCatalog.legacyRecommendedEraId("copper_ingot", Material.COPPER_INGOT));
    }
}
