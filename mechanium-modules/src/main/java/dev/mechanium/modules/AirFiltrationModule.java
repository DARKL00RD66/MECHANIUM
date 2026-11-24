package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

/** Air filtration and environmental engineering content. */
final class AirFiltrationModule extends AbstractBuiltinModule {
    @Override public String id() { return "air-filtration"; }
    @Override public java.util.Set<String> dependencies() { return java.util.Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("industrial_air_filtration", "Filtragem industrial do ar",
                "industrial_revolution", 28, List.of("industrial_chemistry")));
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("air_filtration", "Filtragem do ar",
                "Purificadores, filtros e equipamentos ambientais"));
        registerItem(api, new ItemDefinition("air_purifier", "&bPurificador de ar", "DISPENSER",
                List.of("&8Máquina de filtragem ambiental", "&7Fabricada pelo catálogo MECHANIUM"),
                0, 1, "air_filtration", "industrial_air_filtration"));
        registerItem(api, new ItemDefinition("air_filter_cartridge", "&fCartucho filtrante", "PAPER",
                List.of("&8Elemento substituível do purificador"),
                0, 16, "air_filtration", "industrial_air_filtration"));
        registerRecipe(api, new RecipeDefinition("air_filter_cartridge_recipe", RecipeDefinition.Type.SHAPELESS,
                "air_filter_cartridge", 2, List.of(),
                Map.of('C', "CHARCOAL", 'P', "PAPER", 'I', "IRON_NUGGET"),
                "industrial_air_filtration"));
        registerRecipe(api, new RecipeDefinition("air_purifier_recipe", RecipeDefinition.Type.SHAPELESS,
                "air_purifier", 1, List.of(),
                Map.of('W', "WATER_BUCKET", 'I', "IRON_INGOT", 'G', "GLASS", 'R', "REDSTONE"),
                "industrial_air_filtration"));
        registerMachine(api, new MachineDefinition("air_purifier", "Purificador de ar", "air_purifier",
                "DISPENSER", "CHARCOAL", 2, "air_filter_cartridge", 1, 1200, 1, "industrial_air_filtration"));
    }
}