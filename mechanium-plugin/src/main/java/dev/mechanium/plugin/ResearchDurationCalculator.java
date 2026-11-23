package dev.mechanium.plugin;

import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import org.bukkit.configuration.file.FileConfiguration;

/** Calculates per-technology study times from the server configuration. */
final class ResearchDurationCalculator {
    private static final long ABSOLUTE_MINIMUM_SECONDS = 120L;

    private ResearchDurationCalculator() { }

    static long durationMillis(TechnologyDefinition technology, EraDefinition era, FileConfiguration config) {
        long minimum = Math.max(ABSOLUTE_MINIMUM_SECONDS,
                config.getLong("progression.research.minimum-seconds", ABSOLUTE_MINIMUM_SECONDS));
        long base = Math.max(minimum, config.getLong("progression.research.base-seconds", minimum));
        long override = config.getLong("progression.research.overrides." + technology.id(), 0L);

        long seconds;
        if (override > 0L) {
            seconds = override;
        } else {
            long perPoint = Math.max(0L,
                    config.getLong("progression.research.seconds-per-research-point", 3L));
            long perEra = Math.max(0L,
                    config.getLong("progression.research.seconds-per-era", 15L));
            long costSeconds = saturatedMultiply(technology.researchCost(), perPoint);
            long eraSeconds = saturatedMultiply(Math.max(0, era.order()), perEra);
            seconds = saturatedAdd(base, saturatedAdd(costSeconds, eraSeconds));
        }
        seconds = Math.max(minimum, seconds);
        return saturatedMultiply(seconds, 1_000L);
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long saturatedMultiply(long left, long right) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
}
