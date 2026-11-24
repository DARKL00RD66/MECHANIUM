package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Aquaculture supplies and sustainable fish-farming progression. */
final class AquacultureModule extends AbstractBuiltinModule {
    @Override public String id() { return "aquaculture"; }
    @Override public Set<String> dependencies() { return Set.of("agriculture", "culinary"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("aquaculture_basics", "Fundamentos de aquicultura", "renaissance", 22, List.of("crop_rotation")),
            new TechnologyDefinition("aquaculture_processing", "Processamento aquícola", "industrial_revolution", 38, List.of("aquaculture_basics", "industrial_food_processing"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("aquaculture_goods", "Aquicultura", "Insumos e equipamentos para criação sustentável de peixes"));
        registerItem(api, new ItemDefinition("aquaculture_feed", "&eRação aquícola", "WHEAT", List.of("&8Mistura alimentar para criação de peixes"), 0, 64, "aquaculture_goods", "aquaculture_basics"));
        registerItem(api, new ItemDefinition("aquaculture_net", "&bRede de criação", "STRING", List.of("&8Material para manejo de viveiros"), 0, 32, "aquaculture_goods", "aquaculture_basics"));
        registerItem(api, new ItemDefinition("fish_fertilizer", "&aFertilizante de origem aquícola", "BONE_MEAL", List.of("&8Subproduto reaproveitado da criação"), 0, 64, "aquaculture_goods", "aquaculture_processing"));
        registerItem(api, new ItemDefinition("aquaculture_crate", "&6Caixote de pescado", "BARREL", List.of("&8Embalagem para transporte de pescado"), 0, 16, "aquaculture_goods", "aquaculture_processing"));
        registerItem(api, new ItemDefinition("aquaculture_hatchery", "&3Incubadora aquícola", "DISPENSER", List.of("&8Equipamento de processamento aquícola"), 0, 1, "aquaculture_goods", "aquaculture_processing"));

        registerRecipe(api, new RecipeDefinition("aquaculture_feed_recipe", RecipeDefinition.Type.SHAPELESS, "aquaculture_feed", 4, List.of(), Map.of('W', "WHEAT", 'S', "SEAGRASS", 'K', "KELP"), "aquaculture_basics"));
        registerRecipe(api, new RecipeDefinition("aquaculture_net_recipe", RecipeDefinition.Type.SHAPED, "aquaculture_net", 2, List.of("S S", " S ", "S S"), Map.of('S', "STRING"), "aquaculture_basics"));
        registerRecipe(api, new RecipeDefinition("fish_fertilizer_recipe", RecipeDefinition.Type.SHAPELESS, "fish_fertilizer", 2, List.of(), Map.of('F', "ROTTEN_FLESH", 'K', "KELP", 'B', "BONE_MEAL"), "aquaculture_processing"));
        registerRecipe(api, new RecipeDefinition("aquaculture_crate_recipe", RecipeDefinition.Type.SHAPED, "aquaculture_crate", 1, List.of("PPP", "P P", "PPP"), Map.of('P', "OAK_PLANKS"), "aquaculture_processing"));
        registerRecipe(api, new RecipeDefinition("aquaculture_hatchery_recipe", RecipeDefinition.Type.SHAPED, "aquaculture_hatchery", 1, List.of("INI", "FDF", "III"), Map.of('I', "IRON_INGOT", 'N', "NAUTILUS_SHELL", 'F', "FISHING_ROD", 'D', "DISPENSER"), "aquaculture_processing"));
        registerMachine(api, new MachineDefinition("aquaculture_hatchery", "Incubadora aquícola", "aquaculture_hatchery", "DISPENSER", "mechanium:aquaculture_feed", 2, "fish_fertilizer", 1, 1600, 1, "aquaculture_processing"));
    }
}
