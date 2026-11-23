package dev.mechanium.plugin;

import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResearchDurationCalculatorTest {
    @Test
    void defaultResearchTakesAtLeastTwoMinutes() {
        YamlConfiguration config = new YamlConfiguration();
        TechnologyDefinition technology = new TechnologyDefinition(
                "basic_tools", "Ferramentas básicas", "ancient", 0, List.of());
        EraDefinition era = new EraDefinition("ancient", "Antiguidade", 0);

        assertEquals(120_000L, ResearchDurationCalculator.durationMillis(technology, era, config));
    }

    @Test
    void durationScalesWithResearchCostAndHistoricalEra() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("progression.research.minimum-seconds", 120L);
        config.set("progression.research.base-seconds", 120L);
        config.set("progression.research.seconds-per-research-point", 3L);
        config.set("progression.research.seconds-per-era", 15L);
        TechnologyDefinition technology = new TechnologyDefinition(
                "steam_power", "Energia a vapor", "industrial_revolution", 20, List.of());
        EraDefinition era = new EraDefinition("industrial_revolution", "Revolução Industrial", 4);

        assertEquals(240_000L, ResearchDurationCalculator.durationMillis(technology, era, config));
    }

    @Test
    void perTechnologyOverrideIsRespectedButCannotBypassMinimum() {
        YamlConfiguration config = new YamlConfiguration();
        TechnologyDefinition technology = new TechnologyDefinition(
                "steam_power", "Energia a vapor", "ancient", 20, List.of());
        EraDefinition era = new EraDefinition("ancient", "Antiguidade", 0);

        config.set("progression.research.overrides.steam_power", 60L);
        assertEquals(120_000L, ResearchDurationCalculator.durationMillis(technology, era, config));

        config.set("progression.research.overrides.steam_power", 360L);
        assertEquals(360_000L, ResearchDurationCalculator.durationMillis(technology, era, config));
    }
}
