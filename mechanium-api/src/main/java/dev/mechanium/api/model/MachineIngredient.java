package dev.mechanium.api.model;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** One required input stack type for a machine process. */
public record MachineIngredient(String itemId, int amount) {
    private static final Set<String> NON_ITEMS = Set.of("AIR", "CAVE_AIR", "VOID_AIR");

    public MachineIngredient {
        itemId = normalizeInput(itemId);
        if (amount < 1 || amount > 99) {
            throw new IllegalArgumentException("Machine ingredient amount must be 1..99");
        }
    }

    private static String normalizeInput(String value) {
        Objects.requireNonNull(value, "itemId");
        String input = value.trim();
        if (input.regionMatches(true, 0, "mechanium:", 0, 10)) {
            return "mechanium:" + normalizeId(input.substring(10));
        }
        input = input.toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(input);
        if (material == null || !material.isItem() || NON_ITEMS.contains(material.name())) {
            throw new IllegalArgumentException("itemId must be an item material or mechanium:<id>: " + input);
        }
        return material.name();
    }

    private static String normalizeId(String value) {
        Objects.requireNonNull(value, "custom item id");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
            throw new IllegalArgumentException("Invalid custom item id: " + value);
        }
        return normalized;
    }
}
