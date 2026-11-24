package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Controlled-environment agriculture and hydroponic supplies. */
final class GreenhouseModule extends AbstractBuiltinModule {
    @Override public String id() { return "greenhouse"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("agriculture", "water-treatment"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("controlled_environment_farming", "Cultivo em ambiente controlado", "modern", 42, List.of("greenhouse_cultivation")),
            new TechnologyDefinition("hydroponic_systems", "Sistemas hidropônicos", "information_age", 58, List.of("controlled_environment_farming", "industrial_water_recycling"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("greenhouse_goods", "Estufas e hidroponia", "Estruturas e insumos para cultivo protegido"));
        registerItem(api, new ItemDefinition("greenhouse_glazing", "&aPainel de estufa", "GLASS",
                List.of("&8Painel transparente para estruturas de cultivo"), 0, 64, "greenhouse_goods", "controlled_environment_farming"));
        registerItem(api, new ItemDefinition("nutrient_solution", "&2Solução nutritiva", "HONEY_BOTTLE",
                List.of("&8Insumo de cultivo hidropônico abstrato"), 0, 64, "greenhouse_goods", "hydroponic_systems"));
        registerItem(api, new ItemDefinition("hydroponic_tray", "&aBandeja hidropônica", "MANGROVE_ROOTS",
                List.of("&8Suporte para cultivo sem solo"), 0, 32, "greenhouse_goods", "hydroponic_systems"));
        registerItem(api, new ItemDefinition("climate_controller", "&bControlador climático", "DISPENSER",
                List.of("&8Controla o crescimento de cultivos próximos",
                        "&7Requer energia de uma rede MECHANIUM",
                        "&7Não quebra nem substitui blocos"), 0, 1, "greenhouse_goods", "controlled_environment_farming"));
        registerRecipe(api, new RecipeDefinition("greenhouse_glazing_recipe", RecipeDefinition.Type.SHAPED,
                "greenhouse_glazing", 4, List.of("GGG", "G G", "GGG"), Map.of('G', "GLASS"), "controlled_environment_farming"));
        registerRecipe(api, new RecipeDefinition("nutrient_solution_recipe", RecipeDefinition.Type.SHAPELESS,
                "nutrient_solution", 2, List.of(), Map.of('W', "WATER_BUCKET", 'B', "BONE_MEAL", 'S', "SUGAR"), "hydroponic_systems"));
        registerRecipe(api, new RecipeDefinition("hydroponic_tray_recipe", RecipeDefinition.Type.SHAPED,
                "hydroponic_tray", 1, List.of("III", "SCS", "III"),
                Map.of('I', "IRON_INGOT", 'S', "SMOOTH_STONE", 'C', "mechanium:nutrient_solution"), "hydroponic_systems"));
        registerRecipe(api, new RecipeDefinition("climate_controller_recipe", RecipeDefinition.Type.SHAPED,
                "climate_controller", 1, List.of("RGR", "GCD", "RGR"),
                Map.of('R', "REDSTONE", 'G', "GLASS", 'C', "COPPER_INGOT", 'D', "DISPENSER"),
                "controlled_environment_farming"));
    }
}
