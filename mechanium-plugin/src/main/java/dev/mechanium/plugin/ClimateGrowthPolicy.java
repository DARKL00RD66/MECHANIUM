package dev.mechanium.plugin;

/** Pure rules for the local greenhouse growth controller. */
final class ClimateGrowthPolicy {
    private ClimateGrowthPolicy() { }

    static boolean withinRange(int controllerX, int controllerY, int controllerZ,
                               int cropX, int cropY, int cropZ, int radius) {
        if (radius < 0) throw new IllegalArgumentException("Growth radius cannot be negative");
        long dx = (long) controllerX - cropX;
        long dy = (long) controllerY - cropY;
        long dz = (long) controllerZ - cropZ;
        long range = radius;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    static int boostedAge(int currentAge, int maximumAge, int bonusStages) {
        if (maximumAge < 0) throw new IllegalArgumentException("Maximum crop age cannot be negative");
        if (bonusStages < 0) throw new IllegalArgumentException("Bonus growth cannot be negative");
        if (currentAge < 0 || currentAge > maximumAge) {
            throw new IllegalArgumentException("Current crop age is outside its valid range");
        }
        return (int) Math.min(maximumAge, (long) currentAge + bonusStages);
    }
}
