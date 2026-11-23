package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manufacturing chain for durable energy-storage components. */
final class BatteryIndustryModule extends AbstractBuiltinModule {
    @Override public String id() { return "battery-industry"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("eco-power"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("battery_chemistry", "Química de baterias", "modern", 48, List.of("energy_accumulation")),
            new TechnologyDefinition("industrial_battery_assembly", "Montagem de baterias industriais", "information_age", 64, List.of("battery_chemistry", "electrical_grid"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("battery_industry_goods", "Fabricação de baterias", "Materiais e componentes de armazenamento elétrico"));
        registerItem(api, new ItemDefinition("battery_separator", "&7Separador de bateria", "PAPER", List.of("&8Membrana técnica para células de energia"), 0, 64, "battery_industry_goods", "battery_chemistry"));
        registerItem(api, new ItemDefinition("battery_casing", "&8Invólucro de bateria", "IRON_BLOCK", List.of("&8Estrutura protetora para módulos de energia"), 0, 32, "battery_industry_goods", "battery_chemistry"));
        registerItem(api, new ItemDefinition("charged_cell_casing", "&bCélula montada", "LIGHT_BLUE_CONCRETE", List.of("&8Componente de armazenamento preparado"), 0, 32, "battery_industry_goods", "battery_chemistry"));
        registerItem(api, new ItemDefinition("industrial_battery_pack", "&eBanco de baterias industrial", "BARREL", List.of("&8Conjunto modular de armazenamento elétrico"), 0, 8, "battery_industry_goods", "industrial_battery_assembly"));

        registerRecipe(api, new RecipeDefinition("battery_separator_recipe", RecipeDefinition.Type.SHAPELESS, "battery_separator", 4, List.of(), Map.of('P', "PAPER", 'C', "CHARCOAL", 'R', "REDSTONE"), "battery_chemistry"));
        registerRecipe(api, new RecipeDefinition("battery_casing_recipe", RecipeDefinition.Type.SHAPED, "battery_casing", 1, List.of("III", "I I", "III"), Map.of('I', "IRON_INGOT"), "battery_chemistry"));
        registerRecipe(api, new RecipeDefinition("charged_cell_casing_recipe", RecipeDefinition.Type.SHAPELESS, "charged_cell_casing", 2, List.of(), Map.of('S', "mechanium:battery_separator", 'C', "COPPER_INGOT", 'R', "REDSTONE"), "battery_chemistry"));
        registerRecipe(api, new RecipeDefinition("industrial_battery_pack_recipe", RecipeDefinition.Type.SHAPED, "industrial_battery_pack", 1, List.of("CCC", "BEB", "CCC"), Map.of('C', "mechanium:charged_cell_casing", 'B', "mechanium:battery_casing", 'E', "mechanium:energy_cell"), "industrial_battery_assembly"));
    }
}
