package dev.mechanium.storage;

import org.junit.jupiter.api.Test;
import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void rejectsResearchPointBalanceOverflow() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("overflow.db"), Logger.getLogger("test"))) {
            UUID player = UUID.randomUUID();
            store.addResearchPoints(player, Long.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> store.addResearchPoints(player, 1));
            assertEquals(Long.MAX_VALUE, store.getResearchPoints(player));
        }
    }


    @Test
    void timedResearchPersistsAndCompletesAfterOfflineElapsedTime() throws Exception {
        Path database = directory.resolve("timed-research.db");
        UUID player = UUID.randomUUID();
        long startedAt = 1_000_000L;
        long completesAt = startedAt + 120_000L;

        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.addResearchPoints(player, 50);
            assertTrue(store.beginResearch(player, "steam_power", 20, startedAt, completesAt));
            assertEquals(30L, store.getResearchPoints(player));
            assertEquals(1, store.getResearches(player).size());
            assertTrue(store.completeDueResearches(player, completesAt - 1L).isEmpty());
        }

        try (SqliteDataStore reopened = new SqliteDataStore(database, Logger.getLogger("test"))) {
            assertEquals(1, reopened.getResearches(player).size(),
                    "An active research job must survive a server restart.");
            assertTrue(reopened.completeDueResearches(player, completesAt - 1L).isEmpty(),
                    "The job must not complete before its deadline.");
            assertEquals(List.of("steam_power"),
                    List.copyOf(reopened.completeDueResearches(player, completesAt)),
                    "Elapsed offline time must count toward the research deadline.");
            assertTrue(reopened.hasUnlocked(player, "steam_power"));
            assertTrue(reopened.getResearches(player).isEmpty());
            assertEquals(30L, reopened.getResearchPoints(player));
        }
    }

    @Test
    void timedResearchDoesNotDebitWhenPointsAreInsufficientOrJobAlreadyExists() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("timed-research-atomic.db"), Logger.getLogger("test"))) {
            UUID player = UUID.randomUUID();
            store.addResearchPoints(player, 10);
            assertFalse(store.beginResearch(player, "steam_power", 11, 1_000L, 121_000L));
            assertEquals(10L, store.getResearchPoints(player));
            assertTrue(store.beginResearch(player, "steam_power", 10, 1_000L, 121_000L));
            assertFalse(store.beginResearch(player, "steam_power", 1, 2_000L, 122_000L));
            assertEquals(0L, store.getResearchPoints(player));
            assertEquals(1, store.getResearches(player).size());
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

    @Test
    void machineAndMobDropDefinitionsPersistAcrossStoreInstances() throws Exception {
        Path database = directory.resolve("machine-content.db");
        MachineDefinition machine = new MachineDefinition(
                "mechanical_press", "Prensa mecânica", "mechanical_press", "DISPENSER",
                "IRON_INGOT", 2, "mechanical_parts", 1, 100, "basic_mechanics");
        MobDropDefinition drop = new MobDropDefinition(
                "cow_animal_fat", "COW", "animal_fat", 1, 2, 0.35, "crop_rotation");

        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.saveMachine(machine);
            store.saveMobDrop(drop);
        }

        try (SqliteDataStore reopened = new SqliteDataStore(database, Logger.getLogger("test"))) {
            assertEquals(List.of(machine), List.copyOf(reopened.loadMachines()));
            assertEquals(List.of(drop), List.copyOf(reopened.loadMobDrops()));
        }
    }


    @Test
    void ownedContentIsReconciledAndInactiveOwnersArePruned() throws Exception {
        Path database = directory.resolve("owned-content.db");
        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.saveCategory(new ContentCategory("demo_category", "Demo", "Owned category"));
            store.saveItem(new ItemDefinition("demo_item", "Demo item", "IRON_INGOT",
                    List.of(), 0, 64, "demo_category", null));

            store.beginContentTransaction();
            store.reconcileContentOwner("module:demo", Map.of(
                    "category", Set.of("demo_category"),
                    "item", Set.of("demo_item")));
            store.commitContentTransaction();

            store.beginContentTransaction();
            Map<String, Set<String>> removed = store.reconcileContentOwner("module:demo", Map.of(
                    "category", Set.of("demo_category")));
            store.commitContentTransaction();
            assertEquals(Set.of("demo_item"), removed.get("item"));
            assertEquals(List.of("demo_category"), store.loadCategories().stream().map(ContentCategory::id).toList());
            assertTrue(store.loadItems().isEmpty());

            store.beginContentTransaction();
            store.pruneInactiveContentOwners(Set.of());
            store.commitContentTransaction();
            assertTrue(store.loadCategories().isEmpty());
        }
    }

    @Test
    void contentOwnershipTransactionRollsBackDatabaseChanges() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("owned-rollback.db"), Logger.getLogger("test"))) {
            store.beginContentTransaction();
            store.saveCategory(new ContentCategory("temporary", "Temporary", "Must roll back"));
            store.rollbackContentTransaction();
            assertTrue(store.loadCategories().isEmpty());
        }
    }

    @Test
    void contentCannotBeClaimedByTwoOwners() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("owned-conflict.db"), Logger.getLogger("test"))) {
            store.saveCategory(new ContentCategory("shared", "Shared", "Single owner"));
            store.beginContentTransaction();
            store.reconcileContentOwner("module:first", Map.of("category", Set.of("shared")));
            store.commitContentTransaction();

            store.beginContentTransaction();
            assertThrows(IllegalStateException.class,
                    () -> store.reconcileContentOwner("addon:second", Map.of("category", Set.of("shared"))));
            store.rollbackContentTransaction();
            assertEquals("shared", store.loadCategories().iterator().next().id());
        }
    }


    @Test
    void migrationFromSchemaThreeAdoptsOnlyExistingLegacyDefinitions() throws Exception {
        Path database = directory.resolve("legacy-v3.db");
        try (SqliteDataStore store = new SqliteDataStore(database, Logger.getLogger("test"))) {
            store.saveCategory(new ContentCategory("industrial_components", "Industrial", "Legacy built-in category"));
        }

        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE mechanium_content_owners");
            statement.execute("DELETE FROM mechanium_schema_version WHERE version >= 4");
        }

        try (SqliteDataStore migrated = new SqliteDataStore(database, Logger.getLogger("test"))) {
            migrated.beginContentTransaction();
            Map<String, Set<String>> removed = migrated.reconcileContentOwner("module:industry", Map.of());
            migrated.commitContentTransaction();

            assertEquals(Set.of("industrial_components"), removed.get("category"));
            assertFalse(removed.containsKey("item"),
                    "Legacy ownership must not be seeded for item IDs that do not exist in the database");
        }
    }

}
