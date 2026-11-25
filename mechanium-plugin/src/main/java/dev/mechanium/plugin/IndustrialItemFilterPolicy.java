package dev.mechanium.plugin;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Pure allow-list rules for destination crates in industrial networks. */
final class IndustrialItemFilterPolicy {
    private IndustrialItemFilterPolicy() { }

    static boolean accepts(Set<String> acceptedIds, String itemId) {
        if (acceptedIds == null) throw new IllegalArgumentException("Filter set cannot be null");
        if (itemId == null || itemId.isBlank()) return false;
        return acceptedIds.isEmpty() || acceptedIds.contains(normalize(itemId));
    }

    static Set<String> toggle(Set<String> acceptedIds, String itemId) {
        if (acceptedIds == null) throw new IllegalArgumentException("Filter set cannot be null");
        String normalized = normalize(itemId);
        Set<String> result = new LinkedHashSet<>(acceptedIds);
        if (!result.add(normalized)) result.remove(normalized);
        return Set.copyOf(result);
    }

    static String encode(Set<String> acceptedIds) {
        if (acceptedIds == null) throw new IllegalArgumentException("Filter set cannot be null");
        return new TreeSet<>(acceptedIds).stream().map(IndustrialItemFilterPolicy::normalize)
                .collect(java.util.stream.Collectors.joining(","));
    }

    static Set<String> decode(String stored) {
        if (stored == null || stored.isBlank()) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String token : stored.split(",")) {
            if (!token.isBlank()) result.add(normalize(token));
        }
        return Set.copyOf(result);
    }

    private static String normalize(String itemId) {
        if (itemId == null) throw new IllegalArgumentException("Filter item ID cannot be null");
        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9_.:-]{1,127}")) {
            throw new IllegalArgumentException("Invalid filter item ID: " + itemId);
        }
        return normalized;
    }
}
