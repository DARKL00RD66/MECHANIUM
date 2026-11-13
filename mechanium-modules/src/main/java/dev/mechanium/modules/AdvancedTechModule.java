package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Advanced industrial components and additional powered processes. */
final class AdvancedTechModule extends AbstractBuiltinModule {
    @Override public String id() { return "advanced-tech"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "ore-processing", "culinary"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("precision_engineering", "Engenharia de precisão", "industrial_revolution", 38, List.of("mechanized_production", "ore_refining")),
                new TechnologyDefinition("advanced_assembly", "Montagem avançada", "modern", 58, List.of("precision_engineering")),
                new TechnologyDefinition("industrial_food_machinery", "Maquinário de processamento alimentar", "industrial_revolution", 44,
                        List.of("industrial_food_processing", "precision_engineering"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("advanced_industry", "Indústria avançada", "Peças de precisão e montagem mecanizada"));
        registerItem(api, new ItemDefinition("precision_parts", "&bPeças de precisão", "IRON_INGOT",
                List.of("&8Componentes industriais de tolerância aprimorada"), 0, 64, "advanced_industry", "precision_engineering"));
        registerItem(api, new ItemDefinition("industrial_gear", "&6Engrenagem industrial", "CLOCK",
                List.of("&8Componente para equipamentos industriais"), 0, 32, "advanced_industry", "precision_engineering"));
        registerItem(api, new ItemDefinition("industrial_crusher", "&6Britador industrial", "DISPENSER",
                List.of("&8Processa minério britado em concentrado de ferro"), 0, 1, "advanced_industry", "precision_engineering"));
        registerItem(api, new ItemDefinition("industrial_smelter", "&6Refinaria industrial", "DISPENSER",
                List.of("&8Refina concentrados em ligas industriais"), 0, 1, "advanced_industry", "advanced_assembly"));
        registerItem(api, new ItemDefinition("assembly_machine", "&bMontadora industrial", "DISPENSER",
                List.of("&8Máquina de montagem", "&7Entrada: peças mecânicas no primeiro espaço", "&7Saída: peças de precisão no último espaço"),
                0, 1, "advanced_industry", "advanced_assembly"));
        registerItem(api, new ItemDefinition("industrial_mill", "&6Moinho industrial", "DISPENSER",
                List.of("&8Mói trigo em farinha MECHANIUM", "&7Requer energia e maquinário alimentar"),
                0, 1, "advanced_industry", "industrial_food_machinery"));
        registerItem(api, new ItemDefinition("industrial_canner", "&6Conservadora industrial", "DISPENSER",
                List.of("&8Processa frutas em conserva", "&7Requer energia e maquinário alimentar"),
                0, 1, "advanced_industry", "industrial_food_machinery"));

        registerRecipe(api, new RecipeDefinition("precision_parts_recipe", RecipeDefinition.Type.SHAPELESS,
                "precision_parts", 2, List.of(), Map.of('M', "mechanium:mechanical_parts", 'C', "mechanium:iron_concentrate"),
                "precision_engineering"));
        registerRecipe(api, new RecipeDefinition("industrial_gear_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_gear", 1, List.of(" I ", "ICI", " I "),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT"), "precision_engineering"));
        registerRecipe(api, new RecipeDefinition("assembly_machine_recipe", RecipeDefinition.Type.SHAPED,
                "assembly_machine", 1, List.of("PIP", "GDG", "PIP"),
                Map.of('P', "PISTON", 'I', "IRON_INGOT", 'G', "mechanium:industrial_gear", 'D', "DISPENSER"),
                "advanced_assembly"));
        registerRecipe(api, new RecipeDefinition("industrial_crusher_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_crusher", 1, List.of("III", "PDP", "III"),
                Map.of('I', "IRON_INGOT", 'P', "PISTON", 'D', "DISPENSER"), "precision_engineering"));
        registerRecipe(api, new RecipeDefinition("industrial_smelter_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_smelter", 1, List.of("ICI", "BDB", "ICI"),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'B', "BRICKS", 'D', "DISPENSER"), "advanced_assembly"));
        registerRecipe(api, new RecipeDefinition("industrial_mill_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_mill", 1, List.of("III", "MDM", "III"),
                Map.of('I', "IRON_INGOT", 'M', "mechanium:industrial_gear", 'D', "DISPENSER"),
                "industrial_food_machinery"));
        registerRecipe(api, new RecipeDefinition("industrial_canner_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_canner", 1, List.of("ICI", "HDH", "ICI"),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'H', "HOPPER", 'D', "DISPENSER"),
                "industrial_food_machinery"));

        registerMachine(api, new MachineDefinition("industrial_crusher_machine", "Britador industrial",
                "industrial_crusher", "DISPENSER", "mechanium:crushed_ore", 2, "iron_concentrate", 2, 120,
                "precision_engineering"));
        registerMachine(api, new MachineDefinition("industrial_smelter_machine", "Refinaria industrial",
                "industrial_smelter", "DISPENSER", "mechanium:iron_concentrate", 2, "refined_alloy", 2, 180,
                "advanced_assembly"));
        registerMachine(api, new MachineDefinition("advanced_assembly_machine", "Montadora industrial",
                "assembly_machine", "DISPENSER", "mechanium:mechanical_parts", 2, "precision_parts", 1, 160,
                "advanced_assembly"));
        registerMachine(api, new MachineDefinition("industrial_mill_machine", "Moinho industrial",
                "industrial_mill", "DISPENSER", "WHEAT", 2, "mechanium:mechanium_flour", 2, 100,
                "industrial_food_machinery"));
        registerMachine(api, new MachineDefinition("industrial_canner_machine", "Conservadora industrial",
                "industrial_canner", "DISPENSER", "SWEET_BERRIES", 3, "mechanium:preserved_berries", 1, 120,
                "industrial_food_machinery"));
    }
}
