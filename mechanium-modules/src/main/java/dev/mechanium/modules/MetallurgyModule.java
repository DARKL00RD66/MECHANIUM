package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class MetallurgyModule extends AbstractBuiltinModule {
    @Override public String id() { return "advanced-metallurgy"; }
    @Override public java.util.Set<String> dependencies() { return java.util.Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("advanced_metallurgy", "Metalurgia avançada",
                "industrial_revolution", 34, List.of("steam_power")));
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("advanced_metallurgy", "Metalurgia avançada",
                "Ligas, fundição e componentes metálicos"));
        registerItem(api, new ItemDefinition("steel_component", "&7Componente de aço", "IRON_INGOT",
                List.of("&8Liga industrial reforçada"), 0, 64, "advanced_metallurgy", "advanced_metallurgy"));
        registerItem(api, new ItemDefinition("metallurgy_furnace", "&6Forno metalúrgico", "DISPENSER",
                List.of("&8Equipamento de processamento de ligas"), 0, 1,
                "advanced_metallurgy", "advanced_metallurgy"));
        registerRecipe(api, new RecipeDefinition("steel_component_recipe", RecipeDefinition.Type.SHAPELESS,
                "steel_component", 2, List.of(),
                Map.of('I', "IRON_INGOT", 'C', "COAL", 'N', "IRON_NUGGET"),
                "advanced_metallurgy"));
        registerRecipe(api, new RecipeDefinition("metallurgy_furnace_recipe", RecipeDefinition.Type.SHAPELESS,
                "metallurgy_furnace", 1, List.of(),
                Map.of('I', "IRON_INGOT", 'F', "FURNACE", 'B', "BRICKS"),
                "advanced_metallurgy"));
        registerMachine(api, new MachineDefinition("metallurgy_furnace", "Forno metalúrgico", "metallurgy_furnace",
                "DISPENSER", "IRON_INGOT", 2, "steel_component", 1, 2400, 2, "advanced_metallurgy"));
    }
}