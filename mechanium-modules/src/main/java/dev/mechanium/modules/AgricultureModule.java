package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MobDropDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;

final class AgricultureModule extends AbstractBuiltinModule {
    @Override public String id() { return "agriculture"; }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("crop_rotation", "Rotação de culturas", "medieval", 8, List.of()),
            new TechnologyDefinition("food_preservation", "Conservação de alimentos", "renaissance", 18, List.of("crop_rotation")),
            new TechnologyDefinition("industrial_fertilizer", "Fertilizantes industriais", "industrial_revolution", 30, List.of("crop_rotation"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        api.registerCategory(new ContentCategory(
                "agricultural_goods", "Produtos agrícolas", "Subprodutos rurais, compostos e insumos"));

        api.registerItem(new ItemDefinition(
                "animal_fat", "&6Gordura animal", "SLIME_BALL",
                List.of("&8Subproduto da criação de animais", "&7Usado na produção tradicional de velas"),
                0, 64, "agricultural_goods", "crop_rotation"));
        api.registerItem(new ItemDefinition(
                "compost", "&aComposto orgânico", "BONE_MEAL",
                List.of("&8Insumo agrícola produzido a partir de matéria orgânica"),
                0, 64, "agricultural_goods", "crop_rotation"));
        api.registerItem(new ItemDefinition(
                "industrial_fertilizer", "&eFertilizante industrial", "BONE_MEAL",
                List.of("&8Fertilizante processado para a agricultura mecanizada"),
                0, 64, "agricultural_goods", "industrial_fertilizer"));
        api.registerItem(new ItemDefinition(
                "tallow_torch", "&6Tocha de sebo", "TORCH",
                List.of("&8Iluminação tradicional produzida com gordura animal"),
                0, 64, "agricultural_goods", "food_preservation"));

        api.registerRecipe(new RecipeDefinition(
                "compost_recipe", RecipeDefinition.Type.SHAPELESS, "compost", 2, List.of(),
                Map.of('W', "WHEAT", 'C', "CARROT", 'P', "POTATO"), "crop_rotation"));
        api.registerRecipe(new RecipeDefinition(
                "industrial_fertilizer_recipe", RecipeDefinition.Type.SHAPELESS, "industrial_fertilizer", 3, List.of(),
                Map.of('C', "mechanium:compost", 'H', "HAY_BLOCK", 'I', "COPPER_INGOT"),
                "industrial_fertilizer"));
        api.registerRecipe(new RecipeDefinition(
                "tallow_torch_recipe", RecipeDefinition.Type.SHAPELESS, "tallow_torch", 4, List.of(),
                Map.of('F', "mechanium:animal_fat", 'S', "STRING"), "food_preservation"));

        api.registerMobDrop(new MobDropDefinition(
                "cow_animal_fat", "COW", "animal_fat", 1, 2, 0.35, "crop_rotation"));
        api.registerMobDrop(new MobDropDefinition(
                "pig_animal_fat", "PIG", "animal_fat", 1, 2, 0.30, "crop_rotation"));
        api.registerMobDrop(new MobDropDefinition(
                "sheep_animal_fat", "SHEEP", "animal_fat", 1, 1, 0.20, "crop_rotation"));
    }
}
