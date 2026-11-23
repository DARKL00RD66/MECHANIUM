package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rail freight infrastructure components for the industrial logistics chain. */
final class RailFreightModule extends AbstractBuiltinModule {
    @Override public String id() { return "rail-freight"; }
    @Override public Set<String> dependencies() { return Set.of("industry", "industrial-logistics"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("rail_freight_operations", "Operação de carga ferroviária", "industrial_revolution", 42, List.of("rail_transport", "industrial_logistics")),
            new TechnologyDefinition("intermodal_freight", "Transporte intermodal", "modern", 56, List.of("rail_freight_operations", "industrial_efficiency"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("rail_freight_goods", "Carga ferroviária", "Componentes de infraestrutura e acondicionamento de cargas"));
        registerItem(api, new ItemDefinition("railway_sleeper", "&6Dormente ferroviário", "OAK_PLANKS", List.of("&8Base estrutural para trilhos"), 0, 64, "rail_freight_goods", "rail_freight_operations"));
        registerItem(api, new ItemDefinition("reinforced_rail", "&7Trilho reforçado", "RAIL", List.of("&8Componente ferroviário para transporte de cargas"), 0, 64, "rail_freight_goods", "rail_freight_operations"));
        registerItem(api, new ItemDefinition("freight_pallet", "&6Palete de carga", "BARREL", List.of("&8Suporte padronizado para mercadorias"), 0, 32, "rail_freight_goods", "intermodal_freight"));
        registerItem(api, new ItemDefinition("freight_manifest", "&eManifesto de carga", "PAPER", List.of("&8Registro de lote para transporte intermodal"), 0, 64, "rail_freight_goods", "intermodal_freight"));

        registerRecipe(api, new RecipeDefinition("railway_sleeper_recipe", RecipeDefinition.Type.SHAPED, "railway_sleeper", 4, List.of("PPP", "S S"), Map.of('P', "OAK_PLANKS", 'S', "STICK"), "rail_freight_operations"));
        registerRecipe(api, new RecipeDefinition("reinforced_rail_recipe", RecipeDefinition.Type.SHAPED, "reinforced_rail", 8, List.of("I I", "ISI", "I I"), Map.of('I', "IRON_INGOT", 'S', "STICK"), "rail_freight_operations"));
        registerRecipe(api, new RecipeDefinition("freight_pallet_recipe", RecipeDefinition.Type.SHAPED, "freight_pallet", 1, List.of("PPP", "PBP", "PPP"), Map.of('P', "OAK_PLANKS", 'B', "BARREL"), "intermodal_freight"));
        registerRecipe(api, new RecipeDefinition("freight_manifest_recipe", RecipeDefinition.Type.SHAPELESS, "freight_manifest", 4, List.of(), Map.of('P', "PAPER", 'C', "COMPASS", 'I', "INK_SAC"), "intermodal_freight"));
    }
}
