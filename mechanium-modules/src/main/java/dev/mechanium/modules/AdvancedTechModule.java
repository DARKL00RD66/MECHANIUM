package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Advanced industrial components and an additional process using the existing machine runtime. */
final class AdvancedTechModule extends AbstractBuiltinModule {
    @Override public String id() { return "advanced-tech"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "ore-processing"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("precision_engineering", "Engenharia de precisão", "industrial_revolution", 38, List.of("mechanized_production", "ore_refining")),
                new TechnologyDefinition("advanced_assembly", "Montagem avançada", "modern", 58, List.of("precision_engineering"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("advanced_industry", "Indústria avançada", "Peças de precisão e montagem mecanizada"));
        registerItem(api, new ItemDefinition("precision_parts", "&bPeças de precisão", "IRON_INGOT",
                List.of("&8Componentes industriais de tolerância aprimorada"), 0, 64, "advanced_industry", "precision_engineering"));
        registerItem(api, new ItemDefinition("industrial_gear", "&6Engrenagem industrial", "CLOCK",
                List.of("&8Componente para equipamentos industriais"), 0, 32, "advanced_industry", "precision_engineering"));
        registerItem(api, new ItemDefinition("assembly_machine", "&bMontadora industrial", "DISPENSER",
                List.of("&8Máquina de montagem", "&7Entrada: peças mecânicas no primeiro espaço", "&7Saída: peças de precisão no último espaço"),
                0, 1, "advanced_industry", "advanced_assembly"));

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
        registerMachine(api, new MachineDefinition("advanced_assembly_machine", "Montadora industrial",
                "assembly_machine", "DISPENSER", "mechanium:mechanical_parts", 2, "precision_parts", 1, 160,
                "advanced_assembly"));
    }
}
