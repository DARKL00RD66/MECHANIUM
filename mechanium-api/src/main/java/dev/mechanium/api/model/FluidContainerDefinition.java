package dev.mechanium.api.model;

import java.util.Locale;
import java.util.Objects;

/** A MECHANIUM item that can fill or empty a typed-fluid tank. */
public record FluidContainerDefinition(String itemId, int amountMb,
                                       boolean canFill, boolean canExtract) {
    public FluidContainerDefinition {
        Objects.requireNonNull(itemId, "itemId");
        itemId = itemId.trim().toLowerCase(Locale.ROOT);
        if (!itemId.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
            throw new IllegalArgumentException("Invalid fluid container item ID: " + itemId);
        }
        if (amountMb < 250 || amountMb > 1_000_000 || amountMb % 250 != 0) {
            throw new IllegalArgumentException("Fluid container volume must be a multiple of 250 mB");
        }
        if (!canFill && !canExtract) {
            throw new IllegalArgumentException("A fluid container must support filling or extraction");
        }
    }
}
