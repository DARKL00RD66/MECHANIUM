package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** First functional fluid-processing slice: a dispenser-based water filter. */
final class LiquidModule extends AbstractBuiltinModule {
    @Override public String id() { return "liquid"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("fluid_handling", "Manuseio de fluidos", "renaissance", 18, List.of("basic_mechanics")),
                new TechnologyDefinition("water_filtration", "Filtragem de água", "industrial_revolution", 32, List.of("fluid_handling")),
                new TechnologyDefinition("fluid_distribution", "Distribuição de fluidos", "industrial_revolution", 46, List.of("water_filtration"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("liquid_processing", "Processamento de fluidos", "Recipientes e tratamento básico de água"));
        registerItem(api, new ItemDefinition("filter_cartridge", "&7Cartucho filtrante", "PAPER",
                List.of("&8Material consumível do processo de filtragem"), 0, 16, "liquid_processing", "fluid_handling"));
        registerItem(api, new ItemDefinition("purified_water", "&bÁgua purificada", "POTION",
                List.of("&8Água processada pelo filtro MECHANIUM"), 0, 16, "liquid_processing", "water_filtration"));
        registerItem(api, new ItemDefinition("fluid_pipe", "&bTubulação de fluidos", "DISPENSER",
                List.of("&8Conecta tanques para transportar água purificada"), 0, 64, "liquid_processing", "fluid_distribution"));
        registerItem(api, new ItemDefinition("liquid_tank", "&bTanque de fluidos", "BARREL",
                List.of("&8Armazena água purificada", "&7Conecte dois ou mais tanques com tubulações"), 0, 1, "liquid_processing", "fluid_distribution"));
        registerItem(api, new ItemDefinition("water_filter", "&bFiltro de água", "DISPENSER",
                List.of("&8Máquina de filtragem", "&7Entrada: WATER_BUCKET no primeiro espaço", "&7Saída: água purificada no último espaço"),
                0, 1, "liquid_processing", "water_filtration"));

        registerRecipe(api, new RecipeDefinition("filter_cartridge_recipe", RecipeDefinition.Type.SHAPED,
                "filter_cartridge", 2, List.of(" C ", "PSP", " C "),
                Map.of('C', "CHARCOAL", 'P', "PAPER", 'S', "STRING"), "fluid_handling"));
        registerRecipe(api, new RecipeDefinition("purified_water_recipe", RecipeDefinition.Type.SHAPELESS,
                "purified_water", 1, List.of(), Map.of('W', "WATER_BUCKET", 'F', "mechanium:filter_cartridge"),
                "water_filtration"));
        registerRecipe(api, new RecipeDefinition("water_filter_recipe", RecipeDefinition.Type.SHAPED,
                "water_filter", 1, List.of("III", "CDC", "III"),
                Map.of('I', "IRON_INGOT", 'C', "mechanium:filter_cartridge", 'D', "DISPENSER"),
                "water_filtration"));
        registerRecipe(api, new RecipeDefinition("fluid_pipe_recipe", RecipeDefinition.Type.SHAPED,
                "fluid_pipe", 4, List.of("III", "CRC", "III"),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'R', "REDSTONE"), "fluid_distribution"));
        registerRecipe(api, new RecipeDefinition("liquid_tank_recipe", RecipeDefinition.Type.SHAPED,
                "liquid_tank", 1, List.of("III", "IBI", "III"),
                Map.of('I', "IRON_INGOT", 'B', "BARREL"), "fluid_distribution"));
        registerMachine(api, new MachineDefinition("water_filter_machine", "Filtro de água",
                "water_filter", "DISPENSER", "WATER_BUCKET", 1, "purified_water", 1, 100, "water_filtration"));
    }
}
