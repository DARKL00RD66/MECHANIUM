package dev.mechanium.plugin;

/** Shared balance and tier limits for the equivalent-exchange stations. */
final class EmcTierPolicy {
    private static final long[] MAXIMUM_ITEM_VALUES = {0L, 16L, 64L, 256L, 1_024L, 4_096L};

    private EmcTierPolicy() { }

    static long maximumItemValue(int tier) {
        if (tier < 1 || tier >= MAXIMUM_ITEM_VALUES.length) {
            throw new IllegalArgumentException("EMC station tier must be between 1 and 5.");
        }
        return MAXIMUM_ITEM_VALUES[tier];
    }

    static boolean canProcess(int tier, long itemValue) {
        return itemValue > 0L && itemValue <= maximumItemValue(tier);
    }

    static long totalValue(long itemValue, int amount) {
        if (itemValue <= 0L) throw new IllegalArgumentException("EMC item value must be positive.");
        if (amount < 1) throw new IllegalArgumentException("Item amount must be positive.");
        return Math.multiplyExact(itemValue, (long) amount);
    }
}
