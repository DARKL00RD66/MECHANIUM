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
    void timedResearchDebitsAtStartAndUnlocksOnlyAfterPersistedDeadline() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("timed-research-engine.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerTechnology(new TechnologyDefinition(
                    "steam_power", "Energia a vapor", "ancient", 20, List.of()));

            UUID player = UUID.randomUUID();
            api.setPlayerEra(player, "ancient");
            api.addResearchPoints(player, 40);

            var result = api.startResearch(player, "steam_power", 120_000L);
            assertEquals(dev.mechanium.api.model.ResearchResult.Status.STARTED, result.status());
            assertEquals(20L, api.getResearchPoints(player));
            assertFalse(api.hasUnlockedTechnology(player, "steam_power"));

            var active = api.getResearches(player).stream().findFirst().orElseThrow();
            assertEquals("steam_power", active.technologyId());
            assertEquals(120_000L, active.durationMillis());
            assertEquals(dev.mechanium.api.model.ResearchResult.Status.ALREADY_RESEARCHING,
                    api.startResearch(player, "steam_power", 120_000L).status());

            assertTrue(api.completeDueResearches(player, active.completesAtEpochMillis() - 1L).isEmpty());
            assertFalse(api.hasUnlockedTechnology(player, "steam_power"));
            assertEquals(List.of("steam_power"),
                    List.copyOf(api.completeDueResearches(player, active.completesAtEpochMillis())));
            assertTrue(api.hasUnlockedTechnology(player, "steam_power"));
            assertTrue(api.getResearches(player).isEmpty());
        }
    }

    @Test
    void serverEraAppliesToPlayersWithoutOverridesAndPersistsAcrossReloads() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("global-era.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerEra(new EraDefinition("industrial_revolution", "Revolução Industrial", 3));
            api.registerTechnology(new TechnologyDefinition(
                    "steam_engine", "Máquina a vapor", "industrial_revolution", 10, List.of()));

            UUID regularPlayer = UUID.randomUUID();
            UUID testPlayer = UUID.randomUUID();

            api.setServerEra("ancient");
            assertEquals("ancient", api.getPlayerEra(regularPlayer).orElseThrow());
            assertTrue(api.getPlayerEraOverride(regularPlayer).isEmpty());

            // The existing individual-era tool remains available for isolated testing.
            api.setPlayerEra(testPlayer, "ancient");
            api.setServerEra("industrial_revolution");

            assertEquals("industrial_revolution", api.getServerEra().orElseThrow());
            assertEquals("industrial_revolution", api.getPlayerEra(regularPlayer).orElseThrow(),
                    "Players without a test override must inherit the global era immediately.");
            api.addResearchPoints(regularPlayer, 10);
            assertEquals(UnlockResult.Status.UNLOCKED,
                    api.unlockTechnology(regularPlayer, "steam_engine").status(),
                    "Technology progression must use the global era for players without an override.");
            assertEquals("ancient", api.getPlayerEra(testPlayer).orElseThrow(),
                    "An explicit test override must remain isolated from global changes.");

            DefaultMechaniumApi reloaded = new DefaultMechaniumApi(store);
            reloaded.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            reloaded.registerEra(new EraDefinition("industrial_revolution", "Revolução Industrial", 3));
            assertEquals("industrial_revolution", reloaded.getServerEra().orElseThrow());
            assertEquals("industrial_revolution", reloaded.getPlayerEra(regularPlayer).orElseThrow());
            assertEquals("ancient", reloaded.getPlayerEra(testPlayer).orElseThrow());

            reloaded.clearPlayerEraOverride(testPlayer);
            assertTrue(reloaded.getPlayerEraOverride(testPlayer).isEmpty());
            assertEquals("industrial_revolution", reloaded.getPlayerEra(testPlayer).orElseThrow(),
                    "Clearing a test override must restore inheritance from the server era.");
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
    void registersMobDropsWithValidatedItemsAndResearchGates() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("mob-drops.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("medieval", "Idade Média", 1));
            api.registerTechnology(new TechnologyDefinition("crop_rotation", "Rotação de culturas", "medieval", 8, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory("agricultural_goods", "Produtos agrícolas", "Rural"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("animal_fat", "Gordura animal", "SLIME_BALL",
                    List.of(), 0, 64, "agricultural_goods", "crop_rotation"));

            var drop = new dev.mechanium.api.model.MobDropDefinition(
                    "cow_animal_fat", "COW", "animal_fat", 1, 2, 0.35, "crop_rotation");
            assertTrue(api.registerMobDrop(drop));
            assertTrue(api.registerMobDrop(drop), "Identical registration must be idempotent");
            assertEquals(drop, api.getMobDrop("COW_ANIMAL_FAT").orElseThrow());
            assertEquals(List.of(drop), api.getMobDrops());
            DefaultMechaniumApi reloaded = new DefaultMechaniumApi(store);
            assertEquals(drop, reloaded.getMobDrop("cow_animal_fat").orElseThrow(),
                    "Mob drop definitions must be restored from persistent storage");
            assertTrue(api.validateContent().isEmpty());

            assertThrows(IllegalArgumentException.class, () -> api.registerMobDrop(
                    new dev.mechanium.api.model.MobDropDefinition(
                            "oversized_drop", "PIG", "animal_fat", 1, 65, 0.5, "crop_rotation")));
            assertThrows(IllegalArgumentException.class, () -> api.registerMobDrop(
                    new dev.mechanium.api.model.MobDropDefinition(
                            "unknown_tech_drop", "SHEEP", "animal_fat", 1, 1, 0.5, "unknown_technology")));
        }
    }

    @Test
    void registersMachinesOnlyWhenTheirItemsAndResearchAreValid() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("machines.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("renaissance", "Renascimento", 2));
            api.registerTechnology(new TechnologyDefinition("basic_mechanics", "Mecânica básica", "renaissance", 10, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory("industry", "Indústria", "Industrial"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("mechanical_press", "Prensa", "DISPENSER",
                    List.of(), 0, 1, "industry", "basic_mechanics"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("mechanical_parts", "Peças", "IRON_INGOT",
                    List.of(), 0, 64, "industry", "basic_mechanics"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("chest_machine", "Máquina de baú", "CHEST",
                    List.of(), 0, 1, "industry", "basic_mechanics"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("mixing_machine", "Misturador", "DISPENSER",
                    List.of(), 0, 1, "industry", "basic_mechanics"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition("copper_coil", "Bobina", "COPPER_INGOT",
                    List.of(), 0, 64, "industry", "basic_mechanics"));

            var machine = new dev.mechanium.api.model.MachineDefinition(
                    "mechanical_press", "Prensa", "mechanical_press", "DISPENSER",
                    "IRON_INGOT", 2, "mechanical_parts", 1, 100, 2, "basic_mechanics");
            assertTrue(api.registerMachine(machine));
            assertTrue(api.registerMachine(machine), "Identical machine registration must be idempotent");
            assertEquals(machine, api.getMachine("MECHANICAL_PRESS").orElseThrow());
            assertEquals(List.of(machine), api.getMachines());
            DefaultMechaniumApi reloaded = new DefaultMechaniumApi(store);
            assertEquals(machine, reloaded.getMachine("mechanical_press").orElseThrow(),
                    "Machine definitions must be restored from persistent storage");

            var multiIoMachine = new dev.mechanium.api.model.MachineDefinition(
                    "multi_io_mixer", "Misturador", "mixing_machine", "DISPENSER",
                    List.of(new dev.mechanium.api.model.MachineIngredient("IRON_INGOT", 2),
                            new dev.mechanium.api.model.MachineIngredient("mechanium:mechanical_parts", 1)),
                    List.of(new dev.mechanium.api.model.MachineProduct("mechanical_parts", 2),
                            new dev.mechanium.api.model.MachineProduct("copper_coil", 1)),
                    240, 5, "basic_mechanics");
            assertTrue(api.registerMachine(multiIoMachine));
            assertEquals(multiIoMachine, new DefaultMechaniumApi(store)
                    .getMachine("multi_io_mixer").orElseThrow(),
                    "Multi-input/multi-output machine definitions must persist across reloads");
            assertEquals(5, api.getMachine("multi_io_mixer").orElseThrow().energyCostPerOperation());
            assertTrue(api.validateContent().isEmpty());

            assertThrows(IllegalArgumentException.class, () -> api.registerMachine(
                    new dev.mechanium.api.model.MachineDefinition(
                            "missing_secondary_output", "Invalid", "mixing_machine", "DISPENSER",
                            List.of(new dev.mechanium.api.model.MachineIngredient("IRON_INGOT", 1)),
                            List.of(new dev.mechanium.api.model.MachineProduct("mechanical_parts", 1),
                                    new dev.mechanium.api.model.MachineProduct("unknown_output", 1)),
                            100, "basic_mechanics")),
                    "Every output must be registered");
            assertThrows(IllegalArgumentException.class, () -> api.registerMachine(
                    new dev.mechanium.api.model.MachineDefinition(
                            "missing_output", "Invalid", "mechanical_press", "DISPENSER",
                            "IRON_INGOT", 2, "unknown_output", 1, 100, "basic_mechanics")));
            assertThrows(IllegalArgumentException.class, () -> api.registerMachine(
                    new dev.mechanium.api.model.MachineDefinition(
                            "missing_input", "Invalid", "mechanical_press", "DISPENSER",
                            "mechanium:unknown_input", 1, "mechanical_parts", 1, 100, "basic_mechanics")));
            assertThrows(IllegalArgumentException.class, () -> api.registerMachine(
                    new dev.mechanium.api.model.MachineDefinition(
                            "unsupported_chest_machine", "Baú industrial", "chest_machine", "CHEST",
                            "IRON_INGOT", 1, "mechanical_parts", 1, 100, "basic_mechanics")),
                    "The API must reject machine block types the current runtime cannot process");
        }
    }

    @Test
    void registersAndPersistsOwnedFluidDefinitions() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("fluids.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("industrial_revolution", "Revolução Industrial", 3));
            api.registerTechnology(new TechnologyDefinition(
                    "water_filtration", "Filtragem de água", "industrial_revolution", 10, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                    "fluid_items", "Recipientes", "Itens de fluido"));

            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "water_bottle", "Água purificada", "POTION", List.of(), 0, 16,
                    "fluid_items", "water_filtration"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "water_cell", "Célula de água", "WATER_BUCKET", List.of(), 0, 16,
                    "fluid_items", "water_filtration"));

            var fluid = new dev.mechanium.api.model.FluidDefinition(
                    "purified_water", "Água purificada", "water_filtration",
                    List.of(
                            new dev.mechanium.api.model.FluidContainerDefinition("water_bottle", 250, true, true),
                            new dev.mechanium.api.model.FluidContainerDefinition("water_cell", 1_000, true, false)));

            try (var scope = api.openContentScope("module:water")) {
                assertTrue(api.registerFluid(fluid));
                scope.commit();
            }
            assertTrue(api.registerFluid(fluid), "Identical fluid registration must be idempotent");
            assertEquals(fluid, api.getFluid("PURIFIED_WATER").orElseThrow());
            assertEquals(List.of(fluid), List.copyOf(api.getFluids()));
            assertTrue(api.validateContent().isEmpty());

            DefaultMechaniumApi reloaded = new DefaultMechaniumApi(store);
            assertEquals(fluid, reloaded.getFluid("purified_water").orElseThrow(),
                    "Fluid definitions and their container mappings must survive reloads.");

            assertThrows(IllegalArgumentException.class, () -> reloaded.registerFluid(
                    new dev.mechanium.api.model.FluidDefinition(
                            "unknown_container_fluid", "Fluido inválido", null,
                            List.of(new dev.mechanium.api.model.FluidContainerDefinition(
                                    "missing_item", 250, true, true)))));
        }
    }

    @Test
    void removingAFluidFromAnOwnerScopePrunesOnlyThatFluid() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("fluid-owner-reconcile.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("industrial_revolution", "Revolução Industrial", 3));
            api.registerTechnology(new TechnologyDefinition(
                    "water_filtration", "Filtragem de água", "industrial_revolution", 10, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                    "fluid_items", "Recipientes", "Itens de fluido"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "water_bottle", "Água", "POTION", List.of(), 0, 16, "fluid_items", "water_filtration"));

            var fluid = new dev.mechanium.api.model.FluidDefinition(
                    "purified_water", "Água", "water_filtration",
                    List.of(new dev.mechanium.api.model.FluidContainerDefinition(
                            "water_bottle", 250, true, true)));
            try (var scope = api.openContentScope("module:water")) {
                assertTrue(api.registerFluid(fluid));
                scope.commit();
            }
            assertTrue(api.getFluid("purified_water").isPresent());

            try (var scope = api.openContentScope("module:water")) {
                scope.commit();
            }
            assertTrue(api.getFluid("purified_water").isEmpty());
            assertTrue(store.loadFluids().isEmpty());
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


    @Test
    void contentScopeReconcilesOwnedDefinitionsAfterSuccessfulRegistration() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("scope-reconcile.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            try (var scope = api.openContentScope("module:demo")) {
                assertTrue(api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                        "demo_category", "Demo", "Owned")));
                assertTrue(api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                        "demo_item", "Demo", "IRON_INGOT", List.of(), 0, 64, "demo_category", null)));
                scope.commit();
            }

            try (var scope = api.openContentScope("module:demo")) {
                assertTrue(api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                        "demo_category", "Demo", "Owned")));
                scope.commit();
            }

            assertTrue(api.getCategory("demo_category").isPresent());
            assertTrue(api.getItem("demo_item").isEmpty());
            assertTrue(store.loadItems().isEmpty());
        }
    }

    @Test
    void uncommittedContentScopeRestoresMemoryAndDatabase() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(directory.resolve("scope-rollback.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            try (var scope = api.openContentScope("addon:temporary")) {
                api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                        "temporary_category", "Temporary", "Must roll back"));
                api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                        "temporary_item", "Temporary", "IRON_INGOT", List.of(), 0, 64,
                        "temporary_category", null));
            }
            assertTrue(api.getCategory("temporary_category").isEmpty());
            assertTrue(api.getItem("temporary_item").isEmpty());
            assertTrue(store.loadCategories().isEmpty());
            assertTrue(store.loadItems().isEmpty());
        }
    }


    @Test
    void administrativeRecipeOverrideReplacesRuntimeDefinitionWithoutMutatingModuleOwnedStorage() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("admin-recipe-override.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerTechnology(new TechnologyDefinition("basic_crafting", "Artesanato básico",
                    "ancient", 0, List.of()));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                    "components", "Componentes", "Itens de teste"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "test_gear", "Engrenagem de teste", "IRON_INGOT", List.of(), 0, 64,
                    "components", null));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "test_plate", "Placa de teste", "COPPER_INGOT", List.of(), 0, 64,
                    "components", null));

            var original = new dev.mechanium.api.model.RecipeDefinition(
                    "test_gear_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPED,
                    "test_gear", 1, List.of("I"), java.util.Map.of('I', "IRON_INGOT"), "basic_crafting");
            assertTrue(api.registerRecipe(original));

            var replacement = new dev.mechanium.api.model.RecipeDefinition(
                    "test_gear_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPELESS,
                    "test_gear", 3, List.of(), java.util.Map.of('P', "mechanium:test_plate"), null);
            assertTrue(api.overrideRecipe(replacement));
            assertEquals(replacement, api.getRecipe("test_gear_recipe").orElseThrow());

            // The administrative layer is intentionally separate: module defaults remain the canonical
            // SQLite record, while the plugin's YAML override is applied after module registration.
            assertEquals(original, store.loadRecipes().stream()
                    .filter(recipe -> recipe.id().equals(original.id())).findFirst().orElseThrow());
            assertTrue(api.validateContent().isEmpty());
        }
    }

    @Test
    void administrativeRecipeOverrideRejectsUnknownIngredientsAndUnknownRecipeIds() throws Exception {
        try (SqliteDataStore store = new SqliteDataStore(
                directory.resolve("invalid-admin-recipe-override.db"), Logger.getLogger("test"))) {
            DefaultMechaniumApi api = new DefaultMechaniumApi(store);
            api.registerEra(new EraDefinition("ancient", "Antiguidade", 0));
            api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                    "components", "Componentes", "Itens de teste"));
            api.registerItem(new dev.mechanium.api.model.ItemDefinition(
                    "test_gear", "Engrenagem de teste", "IRON_INGOT", List.of(), 0, 64,
                    "components", null));
            var original = new dev.mechanium.api.model.RecipeDefinition(
                    "test_gear_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPELESS,
                    "test_gear", 1, List.of(), java.util.Map.of('I', "IRON_INGOT"), null);
            assertTrue(api.registerRecipe(original));

            var missingIngredient = new dev.mechanium.api.model.RecipeDefinition(
                    "test_gear_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPELESS,
                    "test_gear", 1, List.of(), java.util.Map.of('X', "mechanium:missing_item"), null);
            assertThrows(IllegalArgumentException.class, () -> api.overrideRecipe(missingIngredient));
            assertFalse(api.overrideRecipe(new dev.mechanium.api.model.RecipeDefinition(
                    "unregistered_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPELESS,
                    "test_gear", 1, List.of(), java.util.Map.of('I', "IRON_INGOT"), null)));
            assertEquals(original, api.getRecipe("test_gear_recipe").orElseThrow());
        }
    }
}
