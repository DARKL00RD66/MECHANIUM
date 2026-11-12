package dev.mechanium.plugin;

import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Pure aggregation helper for the nearby mineral survey. */
final class OreSurvey {
    private OreSurvey() { }

    static boolean isOre(Material material) {
        return material != null && material.name().endsWith("_ORE");
    }

    static Map<Material, Integer> summarize(Iterable<Material> materials) {
        Map<Material, Integer> counts = new TreeMap<>((left, right) -> left.name().compareTo(right.name()));
        for (Material material : materials) {
            if (isOre(material)) counts.merge(material, 1, Integer::sum);
        }
        return Collections.unmodifiableMap(counts);
    }
}
