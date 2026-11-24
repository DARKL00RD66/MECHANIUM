package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Refrigerated storage and temperature-controlled distribution components. */
final class ColdChainModule extends AbstractBuiltinModule {
    @Override public String id() { return "cold-chain"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industrial-logistics", "water-treatment", "eco-power"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("thermal_insulation", "Isolamento térmico", "modern", 46, List.of("industrial_logistics")),
            new TechnologyDefinition("refrigerated_distribution", "Distribuição refrigerada", "information_age", 62, List.of("thermal_insulation", "energy_accumulation", "industrial_water_recycling"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("cold_chain_goods", "Cadeia de frio", "Armazenamento e transporte com controle térmico"));
        registerItem(api, new ItemDefinition("insulating_fiber", "&fFibra isolante", "WHITE_WOOL", List.of("&8Material para isolamento térmico"), 0, 64, "cold_chain_goods", "thermal_insulation"));
        registerItem(api, new ItemDefinition("cooling_cell", "&bCélula de refrigeração", "PACKED_ICE", List.of("&8Componente de refrigeração para equipamentos"), 0, 32, "cold_chain_goods", "refrigerated_distribution"));
        registerItem(api, new ItemDefinition("insulated_crate", "&bCaixote térmico", "CHEST", List.of("&8Embalagem isolada para transporte controlado"), 0, 16, "cold_chain_goods", "thermal_insulation"));
        registerItem(api, new ItemDefinition("refrigeration_unit", "&3Unidade refrigeradora", "DISPENSER", List.of("&8Equipamento de refrigeração industrial"), 0, 1, "cold_chain_goods", "refrigerated_distribution"));

        registerRecipe(api, new RecipeDefinition("insulating_fiber_recipe", RecipeDefinition.Type.SHAPELESS, "insulating_fiber", 4, List.of(), Map.of('W', "WHITE_WOOL", 'S', "STRING"), "thermal_insulation"));
        registerRecipe(api, new RecipeDefinition("cooling_cell_recipe", RecipeDefinition.Type.SHAPELESS, "cooling_cell", 2, List.of(), Map.of('I', "mechanium:coolant_cell", 'P', "PACKED_ICE"), "refrigerated_distribution"));
        registerRecipe(api, new RecipeDefinition("insulated_crate_recipe", RecipeDefinition.Type.SHAPED, "insulated_crate", 1, List.of("FFF", "FCF", "FFF"), Map.of('F', "mechanium:insulating_fiber", 'C', "BARREL"), "thermal_insulation"));
        registerRecipe(api, new RecipeDefinition("refrigeration_unit_recipe", RecipeDefinition.Type.SHAPED, "refrigeration_unit", 1, List.of("ICI", "RDR", "III"), Map.of('I', "IRON_INGOT", 'C', "mechanium:cooling_cell", 'R', "REDSTONE", 'D', "DISPENSER"), "refrigerated_distribution"));
        registerMachine(api, new MachineDefinition("refrigeration_unit", "Unidade refrigeradora", "refrigeration_unit", "DISPENSER", "mechanium:coolant_cell", 1, "cooling_cell", 1, 1200, 2, "refrigerated_distribution"));
    }
}
