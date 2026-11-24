package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.FluidDefinition;
import dev.mechanium.api.model.FluidContainerDefinition;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Water utility and industrial treatment progression. */
final class WaterTreatmentModule extends AbstractBuiltinModule {
    @Override public String id() { return "water-treatment"; }
    @Override public Set<String> dependencies() { return Set.of("environment"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("basic_water_filtration", "Filtragem básica de água", "industrial_revolution", 28, List.of("waste_management")),
            new TechnologyDefinition("industrial_water_recycling", "Recirculação industrial de água", "modern", 46, List.of("basic_water_filtration", "emissions_control"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("water_utility_goods", "Tratamento de água", "Componentes abstratos para filtragem e recirculação industrial"));
        registerItem(api, new ItemDefinition("water_filter_cartridge", "&bCartucho filtrante", "HONEYCOMB",
                List.of("&8Módulo substituível para filtros industriais"), 0, 64, "water_utility_goods", "basic_water_filtration"));
        registerItem(api, new ItemDefinition("purified_water_cell", "&bCélula de água purificada", "WATER_BUCKET",
                List.of("&8Unidade de água tratada para receitas industriais"), 0, 16, "water_utility_goods", "basic_water_filtration"));
        registerItem(api, new ItemDefinition("coolant_cell", "&3Célula de fluido refrigerante", "POWDER_SNOW_BUCKET",
                List.of("&8Fluido de resfriamento abstrato para máquinas"), 0, 16, "water_utility_goods", "industrial_water_recycling"));
        registerItem(api, new ItemDefinition("water_reclaimer", "&9Recuperador hídrico", "CAULDRON",
                List.of("&8Componente para recirculação de água industrial"), 0, 16, "water_utility_goods", "industrial_water_recycling"));

        registerFluid(api, new FluidDefinition("purified_water", "Água purificada", "basic_water_filtration",
                List.of(new FluidContainerDefinition("purified_water", 250, true, true),
                        new FluidContainerDefinition("purified_water_cell", 1_000, true, false))));
        registerFluid(api, new FluidDefinition("industrial_coolant", "Refrigerante industrial", "industrial_water_recycling",
                List.of(new FluidContainerDefinition("coolant_cell", 250, true, true))));
        registerRecipe(api, new RecipeDefinition("water_filter_cartridge_recipe", RecipeDefinition.Type.SHAPED,
                "water_filter_cartridge", 2, List.of("PPP", "C C", "PPP"),
                Map.of('P', "PAPER", 'C', "CHARCOAL"), "basic_water_filtration"));
        registerRecipe(api, new RecipeDefinition("purified_water_cell_recipe", RecipeDefinition.Type.SHAPELESS,
                "purified_water_cell", 1, List.of(), Map.of('W', "WATER_BUCKET", 'F', "mechanium:water_filter_cartridge"), "basic_water_filtration"));
        registerRecipe(api, new RecipeDefinition("coolant_cell_recipe", RecipeDefinition.Type.SHAPELESS,
                "coolant_cell", 1, List.of(), Map.of('W', "mechanium:purified_water_cell", 'S', "SNOWBALL", 'I', "IRON_INGOT"), "industrial_water_recycling"));
        registerRecipe(api, new RecipeDefinition("water_reclaimer_recipe", RecipeDefinition.Type.SHAPED,
                "water_reclaimer", 1, List.of("IHI", "CBC", "IHI"),
                Map.of('I', "IRON_INGOT", 'H', "HOPPER", 'C', "mechanium:water_filter_cartridge", 'B', "BUCKET"), "industrial_water_recycling"));
    }
}
