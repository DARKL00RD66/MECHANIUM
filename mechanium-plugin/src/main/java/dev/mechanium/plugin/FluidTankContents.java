package dev.mechanium.plugin;

import java.util.Locale;

/** A typed fluid volume stored by a MECHANIUM tank, measured in millibuckets. */
record FluidTankContents(String fluidId, int amountMb) {
    FluidTankContents {
        if (amountMb < 0) throw new IllegalArgumentException("Fluid amount cannot be negative");
        if (amountMb == 0) {
            fluidId = null;
        } else {
            if (fluidId == null || fluidId.isBlank()) {
                throw new IllegalArgumentException("A non-empty tank needs a fluid ID");
            }
            fluidId = fluidId.trim().toLowerCase(Locale.ROOT);
            if (!fluidId.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
                throw new IllegalArgumentException("Invalid fluid ID: " + fluidId);
            }
        }
    }

    static FluidTankContents empty() {
        return new FluidTankContents(null, 0);
    }

    boolean isEmpty() {
        return amountMb == 0;
    }
}
