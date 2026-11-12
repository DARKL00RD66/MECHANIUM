package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class IndustryModule extends AbstractBuiltinModule {
    @Override public String id() { return "industry"; }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        api.registerCategory(new ContentCategory("industrial_components", "Componentes industriais", "Peças e materiais para a indústria"));
        api.registerItem(new ItemDefinition("mechanical_parts", "&7Peças mecânicas", "IRON_INGOT",
                List.of("&8Componente industrial básico"), 0, 64, "industrial_components", "basic_mechanics"));
        api.registerItem(new ItemDefinition("copper_coil", "&6Bobina de cobre", "COPPER_INGOT",
                List.of("&8Componente para sistemas elétricos"), 0, 64, "industrial_components", "steam_power"));
        api.registerItem(new ItemDefinition("mechanical_press", "&6Prensa mecânica", "DISPENSER",
                List.of("&8Máquina industrial básica", "&7Entrada: primeiro espaço", "&7Saída: último espaço"),
                0, 1, "industrial_components", "basic_mechanics"));
        api.registerRecipe(new RecipeDefinition("mechanical_parts_recipe", RecipeDefinition.Type.SHAPED,
                "mechanical_parts", 4, List.of(" I ", "III"), Map.of('I', "IRON_INGOT"), "basic_mechanics"));
        api.registerRecipe(new RecipeDefinition("copper_coil_recipe", RecipeDefinition.Type.SHAPELESS,
                "copper_coil", 1, List.of(), Map.of('C', "COPPER_INGOT"), "steam_power"));
        api.registerRecipe(new RecipeDefinition("mechanical_press_recipe", RecipeDefinition.Type.SHAPED,
                "mechanical_press", 1, List.of("III", "IPI", "III"),
                Map.of('I', "IRON_INGOT", 'P', "PISTON"), "basic_mechanics"));
        api.registerMachine(new MachineDefinition("mechanical_press", "Prensa mecânica",
                "mechanical_press", "DISPENSER", "IRON_INGOT", 2,
                "mechanical_parts", 1, 100, "basic_mechanics"));
    }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("basic_mechanics", "Mecânica básica", "renaissance", 10, List.of()),
            new TechnologyDefinition("steam_power", "Energia a vapor", "industrial_revolution", 35, List.of("basic_mechanics")),
            new TechnologyDefinition("mechanized_production", "Produção mecanizada", "industrial_revolution", 55, List.of("steam_power"))
        );
    }
}