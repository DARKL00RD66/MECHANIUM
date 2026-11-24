package dev.mechanium.api.model;

import java.util.Locale;
import java.util.Objects;

/** One MECHANIUM item produced by a machine process. */
public record MachineProduct(String itemId, int amount) {
    public MachineProduct {
        Objects.requireNonNull(itemId, "itemId");
        itemId = itemId.trim().toLowerCase(Locale.ROOT);
        if (!itemId.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
            throw new IllegalArgumentException("Invalid machine product item id: " + itemId);
        }
        if (amount < 1 || amount > 99) {
            throw new IllegalArgumentException("Machine product amount must be 1..99");
        }
    }
}
