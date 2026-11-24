package dev.mechanium.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure charge accounting shared by the loaded-block energy network. */
final class EnergyAllocationPolicy {
    private EnergyAllocationPolicy() { }

    /**
     * Plans an all-or-nothing debit across batteries in their supplied order.
     * An empty Optional means the network does not have enough total charge.
     */
    static Optional<List<Integer>> planConsumption(List<Integer> charges, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Energy debit cannot be negative");
        List<Integer> result = validatedCopy(charges);
        int remaining = amount;
        for (int i = 0; i < result.size() && remaining > 0; i++) {
            int current = result.get(i);
            int taken = Math.min(current, remaining);
            result.set(i, current - taken);
            remaining -= taken;
        }
        return remaining == 0 ? Optional.of(List.copyOf(result)) : Optional.empty();
    }

    /** Plans generation across batteries in the supplied order without exceeding capacity. */
    static List<Integer> planGeneration(List<Integer> charges, int production, int capacity) {
        if (production < 0) throw new IllegalArgumentException("Energy production cannot be negative");
        if (capacity < 1) throw new IllegalArgumentException("Battery capacity must be positive");
        List<Integer> result = validatedCopy(charges);
        if (result.stream().anyMatch(charge -> charge > capacity)) {
            throw new IllegalArgumentException("Current battery charge exceeds capacity");
        }

        int remaining = production;
        for (int i = 0; i < result.size() && remaining > 0; i++) {
            int current = result.get(i);
            int inserted = Math.min(capacity - current, remaining);
            result.set(i, current + inserted);
            remaining -= inserted;
        }
        return List.copyOf(result);
    }

    private static List<Integer> validatedCopy(List<Integer> charges) {
        if (charges == null) throw new IllegalArgumentException("Battery charge list cannot be null");
        List<Integer> result = new ArrayList<>(charges.size());
        for (Integer charge : charges) {
            if (charge == null || charge < 0) {
                throw new IllegalArgumentException("Battery charge values must be non-negative");
            }
            result.add(charge);
        }
        return result;
    }
}
