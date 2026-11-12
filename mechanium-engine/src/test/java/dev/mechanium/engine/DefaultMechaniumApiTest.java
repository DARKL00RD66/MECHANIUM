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
            assertFalse(api.hasUnlockedTechnology(player, "steam_power"));
            assertFalse(api.canUseTechnology(player, "steam_power"));

            assertEquals(UnlockResult.Status.UNLOCKED,
                    api.unlockTechnology(player, "basic_mechanics").status());
            assertTrue(api.canUseTechnology(player, "basic_mechanics"));

            api.setPlayerEra(player, "industrial_revolution");
            assertEquals(UnlockResult.Status.UNLOCKED,
                    api.unlockTechnology(player, "steam_power").status());
            assertTrue(api.hasUnlockedTechnology(player, "steam_power"));
            assertTrue(api.canUseTechnology(player, "steam_power"));
            assertEquals(60, api.getResearchPoints(player));

            api.setPlayerEra(player, "ancient");
            assertTrue(api.hasUnlockedTechnology(player, "steam_power"),
                    "Unlocked state must be distinct from current-era authorization");
            assertFalse(api.canUseTechnology(player, "steam_power"));
        }
    }

    @Test
    void refusesRecipeOutputAboveCustomItemStackLimit() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("recipe-stack-limit.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerTechnology(new TechnologyDefinition("smithing", "Metalurgia", "ancient", 0, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory("industry", "Indústria", "Industrial"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("small_gear", "Engrenagem", "IRON_INGOT",
                    List.of(), 0, 4, "industry", "smithing"));
            assertThrows(IllegalArgumentException.class, () -> api.registerRecipe(
                    new dev.mechanium.api.model.RecipeDefinition("too_many_gears",
                            dev.mechanium.api.model.RecipeDefinition.Type.SHAPELESS, "small_gear", 5,
                            List.of(), java.util.Map.of('I', "IRON_INGOT"), "smithing")));
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
    @Test
    void itemAndRecipeRegistrationRespectTechnologyUnlocks() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("content-gates.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerTechnology(new TechnologyDefinition("smithing", "Metalurgia", "ancient", 10, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory("industry", "Indústria", "Industrial"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("gear", "Engrenagem", "IRON_INGOT",
                    List.of(), 0, 64, "industry", "smithing"));
            api.registerRecipe(new dev.mechanium.api.model.RecipeDefinition("gear_recipe",
                    dev.mechanium.api.model.RecipeDefinition.Type.SHAPED, "gear", 1,
                    List.of("I"), java.util.Map.of('I', "IRON_INGOT"), "smithing"));

            UUID player = UUID.randomUUID();
            api.setPlayerEra(player, "ancient");
            api.addResearchPoints(player, 10);
            assertFalse(api.canUseItem(player, "gear"));
            assertFalse(api.canCraft(player, "gear_recipe"));
            assertEquals(UnlockResult.Status.UNLOCKED, api.unlockTechnology(player, "smithing").status());
            assertTrue(api.canUseItem(player, "gear"));
            assertTrue(api.canCraft(player, "gear_recipe"));
            assertTrue(api.validateContent().isEmpty());
        }
    }

}
