package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Independent historical food-chain reimplementation inspired by culinary and garden addons. */
final class CulinaryModule extends AbstractBuiltinModule {
    @Override public String id() { return "culinary"; }
    @Override public Set<String> dependencies() { return Set.of("agriculture", "industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("horticulture", "Horticultura", "medieval", 12, List.of("crop_rotation")),
                new TechnologyDefinition("culinary_preservation", "Conservação culinária", "renaissance", 20, List.of("food_preservation")),
                new TechnologyDefinition("industrial_food_processing", "Processamento alimentar industrial", "industrial_revolution", 38,
                        List.of("culinary_preservation", "mechanized_production")),
                new TechnologyDefinition("food_quality_control", "Controle de qualidade alimentar", "modern", 46,
                        List.of("industrial_food_processing", "greenhouse_cultivation"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "culinary_goods", "Culinária e horticultura", "Ingredientes processados e cadeias alimentares históricas"));
        registerItem(api, new ItemDefinition("mechanium_flour", "&fFarinha", "PAPER",
                List.of("&8Ingrediente moído para produção de alimentos"), 0, 64, "culinary_goods", "horticulture"));
        registerItem(api, new ItemDefinition("bread_dough", "&eMassa de pão", "BREAD",
                List.of("&8Massa preparada para panificação"), 0, 64, "culinary_goods", "culinary_preservation"));
        registerItem(api, new ItemDefinition("preserved_berries", "&dFrutas em conserva", "HONEY_BOTTLE",
                List.of("&8Alimento conservado para armazenamento"), 0, 16, "culinary_goods", "culinary_preservation"));
        registerItem(api, new ItemDefinition("industrial_ration", "&6Ração industrial", "COOKED_BEEF",
                List.of("&8Alimento padronizado para cadeias de abastecimento"), 0, 64, "culinary_goods", "industrial_food_processing"));
        registerItem(api, new ItemDefinition("cheese_wheel", "&eQueijo artesanal", "HONEYCOMB",
                List.of("&8Alimento produzido por processamento de leite"), 0, 16, "culinary_goods", "culinary_preservation"));
        registerItem(api, new ItemDefinition("vegetable_stew", "&aEnsopado de legumes", "RABBIT_STEW",
                List.of("&8Refeição preparada com produtos agrícolas"), 0, 16, "culinary_goods", "horticulture"));
        registerItem(api, new ItemDefinition("preserved_meat", "&6Carne conservada", "COOKED_PORKCHOP",
                List.of("&8Proteína preparada para armazenamento prolongado"), 0, 16, "culinary_goods", "food_quality_control"));
        registerItem(api, new ItemDefinition("ration_crate", "&6Caixa de rações", "CHEST",
                List.of("&8Lote de alimentos para abastecimento"), 0, 16, "culinary_goods", "food_quality_control"));

        registerRecipe(api, new RecipeDefinition("mechanium_flour_recipe", RecipeDefinition.Type.SHAPELESS,
                "mechanium_flour", 2, List.of(), Map.of('W', "WHEAT", 'S', "WHEAT"), "horticulture"));
        registerRecipe(api, new RecipeDefinition("bread_dough_recipe", RecipeDefinition.Type.SHAPELESS,
                "bread_dough", 1, List.of(), Map.of('F', "mechanium:mechanium_flour", 'E', "EGG", 'M', "MILK_BUCKET"),
                "culinary_preservation"));
        registerRecipe(api, new RecipeDefinition("preserved_berries_recipe", RecipeDefinition.Type.SHAPELESS,
                "preserved_berries", 1, List.of(), Map.of('B', "SWEET_BERRIES", 'S', "SUGAR", 'H', "HONEY_BOTTLE"),
                "culinary_preservation"));
        registerRecipe(api, new RecipeDefinition("industrial_ration_recipe", RecipeDefinition.Type.SHAPELESS,
                "industrial_ration", 2, List.of(), Map.of('D', "mechanium:bread_dough", 'M', "COOKED_BEEF",
                        'C', "COOKED_COD"), "industrial_food_processing"));
        registerRecipe(api, new RecipeDefinition("cheese_wheel_recipe", RecipeDefinition.Type.SHAPELESS,
                "cheese_wheel", 2, List.of(), Map.of('M', "MILK_BUCKET", 'S', "SUGAR", 'H', "HONEY_BOTTLE"),
                "culinary_preservation"));
        registerRecipe(api, new RecipeDefinition("vegetable_stew_recipe", RecipeDefinition.Type.SHAPELESS,
                "vegetable_stew", 1, List.of(), Map.of('C', "CARROT", 'P', "POTATO", 'B', "BEETROOT"),
                "horticulture"));
        registerRecipe(api, new RecipeDefinition("preserved_meat_recipe", RecipeDefinition.Type.SHAPELESS,
                "preserved_meat", 2, List.of(), Map.of('M', "COOKED_PORKCHOP", 'S', "SUGAR", 'F', "SWEET_BERRIES"),
                "food_quality_control"));
        registerRecipe(api, new RecipeDefinition("ration_crate_recipe", RecipeDefinition.Type.SHAPED,
                "ration_crate", 1, List.of("PPP", "P R", "PPP"),
                Map.of('P', "OAK_PLANKS", 'R', "mechanium:industrial_ration"), "food_quality_control"));
    }
}
