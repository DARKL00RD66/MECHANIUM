package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Independent, passive-livestock-only interpretation of ElectricSpawners.
 * Incubators are powered dispenser machines that produce vanilla spawn eggs;
 * hostile-mob spawning is deliberately outside this module's scope.
 */
final class ElectricSpawnersModule extends AbstractBuiltinModule {
    @Override public String id() { return "electric-spawners"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("agriculture", "eco-power", "industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("livestock_incubation", "Incubação pecuária elétrica",
                        "electrification", 48, List.of("electric_motors", "crop_rotation")),
                new TechnologyDefinition("advanced_livestock_incubation", "Incubação pecuária avançada",
                        "modern", 58, List.of("livestock_incubation", "industrial_efficiency"))
        );
    }

    @Override protected void registerExtra(MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("electric_spawners", "Incubação pecuária",
                "Produção controlada de animais passivos usando energia armazenada"));

        registerItem(api, new ItemDefinition("chicken_incubator", "&eIncubadora de aves", "DISPENSER",
                List.of("&8Incubadora elétrica de aves", "&7Entrada: sementes", "&7Saída: ovo de invocação de galinha"),
                0, 1, "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("cow_incubator", "&6Incubadora bovina", "DISPENSER",
                List.of("&8Incubadora elétrica bovina", "&7Entrada: feno", "&7Saída: ovo de invocação de vaca"),
                0, 1, "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("pig_incubator", "&dIncubadora suína", "DISPENSER",
                List.of("&8Incubadora elétrica suína", "&7Entrada: cenouras", "&7Saída: ovo de invocação de porco"),
                0, 1, "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("sheep_incubator", "&fIncubadora ovina", "DISPENSER",
                List.of("&8Incubadora elétrica ovina", "&7Entrada: trigo", "&7Saída: ovo de invocação de ovelha"),
                0, 1, "electric_spawners", "livestock_incubation"));

        registerItem(api, new ItemDefinition("incubated_chicken_egg", "&eOvo incubado de galinha",
                "CHICKEN_SPAWN_EGG", List.of("&8Produzido por uma incubadora elétrica"), 0, 16,
                "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("incubated_cow_egg", "&6Ovo incubado de vaca",
                "COW_SPAWN_EGG", List.of("&8Produzido por uma incubadora elétrica"), 0, 16,
                "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("incubated_pig_egg", "&dOvo incubado de porco",
                "PIG_SPAWN_EGG", List.of("&8Produzido por uma incubadora elétrica"), 0, 16,
                "electric_spawners", "livestock_incubation"));
        registerItem(api, new ItemDefinition("incubated_sheep_egg", "&fOvo incubado de ovelha",
                "SHEEP_SPAWN_EGG", List.of("&8Produzido por uma incubadora elétrica"), 0, 16,
                "electric_spawners", "livestock_incubation"));

        registerRecipe(api, new RecipeDefinition("chicken_incubator_recipe", RecipeDefinition.Type.SHAPED,
                "chicken_incubator", 1, List.of("IRI", "RDR", "ICI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DISPENSER", 'C', "COPPER_INGOT"),
                "livestock_incubation"));
        registerRecipe(api, new RecipeDefinition("cow_incubator_recipe", RecipeDefinition.Type.SHAPED,
                "cow_incubator", 1, List.of("HCH", "RDR", "ICI"),
                Map.of('H', "HAY_BLOCK", 'C', "COPPER_INGOT", 'R', "REDSTONE", 'D', "DISPENSER", 'I', "IRON_INGOT"),
                "livestock_incubation"));
        registerRecipe(api, new RecipeDefinition("pig_incubator_recipe", RecipeDefinition.Type.SHAPED,
                "pig_incubator", 1, List.of("ICI", "RDR", "ICI"),
                Map.of('I', "IRON_INGOT", 'C', "CARROT", 'R', "REDSTONE", 'D', "DISPENSER"),
                "livestock_incubation"));
        registerRecipe(api, new RecipeDefinition("sheep_incubator_recipe", RecipeDefinition.Type.SHAPED,
                "sheep_incubator", 1, List.of("WWW", "RDR", "ICI"),
                Map.of('W', "WHITE_WOOL", 'R', "REDSTONE", 'D', "DISPENSER", 'I', "IRON_INGOT", 'C', "COPPER_INGOT"),
                "livestock_incubation"));

        registerMachine(api, new MachineDefinition("chicken_incubator_machine", "Incubadora de aves",
                "chicken_incubator", "DISPENSER", "WHEAT_SEEDS", 8,
                "incubated_chicken_egg", 1, 1_200, "livestock_incubation"));
        registerMachine(api, new MachineDefinition("cow_incubator_machine", "Incubadora bovina",
                "cow_incubator", "DISPENSER", "HAY_BLOCK", 1,
                "incubated_cow_egg", 1, 2_400, "livestock_incubation"));
        registerMachine(api, new MachineDefinition("pig_incubator_machine", "Incubadora suína",
                "pig_incubator", "DISPENSER", "CARROT", 8,
                "incubated_pig_egg", 1, 1_800, "livestock_incubation"));
        registerMachine(api, new MachineDefinition("sheep_incubator_machine", "Incubadora ovina",
                "sheep_incubator", "DISPENSER", "WHEAT", 8,
                "incubated_sheep_egg", 1, 1_800, "livestock_incubation"));
    }
}
