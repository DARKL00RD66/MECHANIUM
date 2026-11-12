package dev.mechanium.storage;

import org.junit.jupiter.api.Test;
import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDataStoreTest {
    @TempDir Path directory;

    @Test
    void pointsAndUnlocksPersistAcrossStoreInstances() throws Exception {
        Path database = directory.resolve("mechanium.db");
        UUID player = UUID.randomUUID();

        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.addResearchPoints(player, 80);
            assertEquals(80, store.getResearchPoints(player));
            assertTrue(store.spendAndUnlock(player, "steam_power", 35));
            assertEquals(45, store.getResearchPoints(player));
            assertFalse(store.spendAndUnlock(player, "steam_power", 35));
        }

        try (SqliteDataStore reopened = new SqliteDataStore(database, Logger.getLogger("test"))) {
            assertEquals(45, reopened.getResearchPoints(player));
            assertTrue(reopened.hasUnlocked(player, "steam_power"));
        }
    }

    @Test
    void insufficientBalanceDoesNotCreateUnlockOrNegativeBalance() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("insufficient.db"), Logger.getLogger("test"))) {
            UUID player = UUID.randomUUID();
            store.addResearchPoints(player, 10);
            assertFalse(store.spendAndUnlock(player, "expensive_technology", 11));
            assertEquals(10, store.getResearchPoints(player));
            assertFalse(store.hasUnlocked(player, "expensive_technology"));
        }
    }

    @Test
    void rejectsNegativeResearchPointGrants() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("negative.db"), Logger.getLogger("test"))) {
            assertThrows(IllegalArgumentException.class, () -> store.addResearchPoints(UUID.randomUUID(), -1));
        }
    }
    @Test
    void contentDefinitionsPersistAcrossStoreInstances() throws Exception {
        Path database = directory.resolve("content.db");
        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.saveCategory(new ContentCategory("industrial", "Industrial", "Industrial components"));
            store.saveItem(new ItemDefinition("gear", "Gear", "IRON_INGOT",
                    List.of("A mechanical part"), 0, 64, "industrial", null));
            store.saveRecipe(new RecipeDefinition("gear_recipe", RecipeDefinition.Type.SHAPED,
                    "gear", 1, List.of("II", " I"), Map.of('I', "IRON_INGOT"), null));
        }
        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            assertEquals("industrial", store.loadCategories().iterator().next().id());
            assertEquals("gear", store.loadItems().iterator().next().id());
            RecipeDefinition recipe = store.loadRecipes().iterator().next();
            assertEquals("gear_recipe", recipe.id());
            assertEquals(List.of("II", " I"), recipe.shape());
            assertEquals("IRON_INGOT", recipe.ingredients().get('I'));
        }
    }

}
