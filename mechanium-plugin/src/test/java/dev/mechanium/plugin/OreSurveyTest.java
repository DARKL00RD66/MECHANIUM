package dev.mechanium.plugin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OreSurveyTest {
    @Test
    void countsOreBlocksByMaterialAndIgnoresOtherBlocks() {
        Map<Material, Integer> result = OreSurvey.summarize(List.of(
                Material.IRON_ORE,
                Material.IRON_ORE,
                Material.DEEPSLATE_IRON_ORE,
                Material.COAL_ORE,
                Material.STONE,
                Material.COPPER_INGOT
        ));

        assertEquals(2, result.get(Material.IRON_ORE));
        assertEquals(1, result.get(Material.DEEPSLATE_IRON_ORE));
        assertEquals(1, result.get(Material.COAL_ORE));
        assertFalse(result.containsKey(Material.STONE));
        assertFalse(result.containsKey(Material.COPPER_INGOT));
        assertEquals(4, result.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void recognizesOverworldAndNetherOreBlocks() {
        assertTrue(OreSurvey.isOre(Material.DIAMOND_ORE));
        assertTrue(OreSurvey.isOre(Material.DEEPSLATE_REDSTONE_ORE));
        assertTrue(OreSurvey.isOre(Material.NETHER_GOLD_ORE));
        assertTrue(OreSurvey.isOre(Material.NETHER_QUARTZ_ORE));
        assertFalse(OreSurvey.isOre(Material.ANCIENT_DEBRIS));
        assertFalse(OreSurvey.isOre(Material.GOLD_INGOT));
    }
}
