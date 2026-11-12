package dev.mechanium.engine;

import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.UnlockResult;
import dev.mechanium.storage.SqliteDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMechaniumApiTest {
    @TempDir Path directory;

    @Test
    void eraAndResearchRulesAreEnforcedAtUnlockAndUseTime() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("progression.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            assertTrue(api.registerEra(new EraDefinition("ancient", "Antiguidade", 0)));
            assertTrue(api.registerEra(new EraDefinition("industrial_revolution", "Revolução Industrial", 3)));
            assertTrue(api.registerTechnology(new TechnologyDefinition(
                    "basic_mechanics", "Mecânica básica", "ancient", 10, List.of())));
            assertTrue(api.registerTechnology(new TechnologyDefinition(
                    "steam_power", "Energia a vapor", "industrial_revolution", 30, List.of("basic_mechanics"))));

            UUID player = UUID.randomUUID();
            api.setPlayerEra(player, "ancient");
            api.addResearchPoints(player, 100);

            assertEquals(UnlockResult.Status.WRONG_ERA,
                    api.unlockTechnology(player, "steam_power").status());
            assertFalse(api.canUseTechnology(player, "steam_power"));

            assertEquals(UnlockResult.Status.UNLOCKED,
                    api.unlockTechnology(player, "basic_mechanics").status());
            assertTrue(api.canUseTechnology(player, "basic_mechanics"));

            api.setPlayerEra(player, "industrial_revolution");
            assertEquals(UnlockResult.Status.UNLOCKED,
                    api.unlockTechnology(player, "steam_power").status());
            assertTrue(api.canUseTechnology(player, "steam_power"));
            assertEquals(60, api.getResearchPoints(player));
        }
    }

    @Test
    void refusesTechnologyWithUnknownEra() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("unknown-era.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            assertThrows(IllegalArgumentException.class, () -> api.registerTechnology(
                    new TechnologyDefinition("steam_power", "Energia a vapor", "unknown_age", 10, List.of())));
        }
    }
}
