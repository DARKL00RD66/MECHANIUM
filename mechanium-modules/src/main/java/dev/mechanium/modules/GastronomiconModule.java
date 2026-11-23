package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Extended food preservation and supply-chain recipes inspired by Gastronomicon. */
final class GastronomiconModule extends AbstractBuiltinModule {
    @Override public String id() { return "gastronomicon"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("culinary", "agriculture"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("nutritional_science", "Ciência nutricional", "information_age",
                        60, List.of("food_quality_control")),
                new TechnologyDefinition("mass_food_preservation", "Conservação alimentar em escala",
                        "digital_age", 72, List.of("nutritional_science", "industrial_food_processing"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("gastronomicon_foods", "Alimentos avançados",
                "Refeições conservadas e suprimentos para longas viagens"));

        registerItem(api, new ItemDefinition("nutrient_bar", "&6Barra nutritiva", "GOLDEN_CARROT",
                List.of("&8Alimento compacto para expedições"), 0, 32,
                "gastronomicon_foods", "nutritional_science"));
        registerItem(api, new ItemDefinition("dried_meal_pack", "&ePacote de refeição desidratada", "DRIED_KELP",
                List.of("&8Refeição leve, preparada para armazenamento"), 0, 32,
                "gastronomicon_foods", "nutritional_science"));
        registerItem(api, new ItemDefinition("vegetable_pie", "&aTorta de legumes", "PUMPKIN_PIE",
                List.of("&8Alimento preparado com produtos agrícolas"), 0, 16,
                "gastronomicon_foods", "food_quality_control"));
        registerItem(api, new ItemDefinition("field_ration_crate", "&6Caixa de rações de campanha", "CHEST",
                List.of("&8Suprimento alimentar padronizado para grupos"), 0, 8,
                "gastronomicon_foods", "mass_food_preservation"));

        registerRecipe(api, new RecipeDefinition("nutrient_bar_recipe", RecipeDefinition.Type.SHAPELESS,
                "nutrient_bar", 2, List.of(),
                Map.of('H', "HONEY_BOTTLE", 'G', "GOLDEN_CARROT", 'S', "SWEET_BERRIES"),
                "nutritional_science"));
        registerRecipe(api, new RecipeDefinition("dried_meal_pack_recipe", RecipeDefinition.Type.SHAPELESS,
                "dried_meal_pack", 2, List.of(),
                Map.of('B', "mechanium:bread_dough", 'M', "mechanium:preserved_meat",
                        'V', "mechanium:vegetable_stew"),
                "nutritional_science"));
        registerRecipe(api, new RecipeDefinition("vegetable_pie_recipe", RecipeDefinition.Type.SHAPED,
                "vegetable_pie", 1, List.of("PPP", "VEV", " F "),
                Map.of('P', "PUMPKIN", 'V', "CARROT", 'E', "EGG", 'F', "mechanium:mechanium_flour"),
                "food_quality_control"));
        registerRecipe(api, new RecipeDefinition("field_ration_crate_recipe", RecipeDefinition.Type.SHAPED,
                "field_ration_crate", 1, List.of("PPP", "PRP", "PPP"),
                Map.of('P', "OAK_PLANKS", 'R', "mechanium:nutrient_bar"),
                "mass_food_preservation"));
    }
}
