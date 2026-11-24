package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native MECHANIUM module for local collectible-head cosmetics; no external platform integration. */
final class HeadCollectionModule extends AbstractBuiltinModule {
    @Override public String id() { return "head-collection"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("storage"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("head_collecting", "Colecionismo de cabeças", "renaissance", 20, List.of()),
            new TechnologyDefinition("head_exhibitions", "Exposições de coleção", "industrial_revolution", 32, List.of())
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        // The category ID is retained as a storage key for existing worlds; it is not a platform integration.
        registerCategory(api, new ContentCategory("geyser_heads_goods", "Coleção de cabeças",
                "Itens cosméticos e expositores locais de coleção do MECHANIUM"));
        // Persisted item/category/recipe IDs intentionally remain stable for existing worlds.
        registerItem(api, new ItemDefinition("head_token", "&eFicha de coleção", "PAPER",
                List.of("&8Coleção cosmética local do MECHANIUM"), 0, 64, "geyser_heads_goods", "head_collecting"));
        registerItem(api, new ItemDefinition("artisan_head", "&eCabeça de artesão", "PLAYER_HEAD",
                List.of("&8Coleção cosmética local do MECHANIUM"), 0, 1, "geyser_heads_goods", "head_collecting"));
        registerItem(api, new ItemDefinition("head_display", "&eSuporte de exposição", "ITEM_FRAME",
                List.of("&8Coleção cosmética local do MECHANIUM"), 0, 64, "geyser_heads_goods", "head_exhibitions"));
        registerItem(api, new ItemDefinition("head_collection_case", "&eVitrine de coleção", "CHEST",
                List.of("&8Coleção cosmética local do MECHANIUM"), 0, 64, "geyser_heads_goods", "head_exhibitions"));
        registerRecipe(api, new RecipeDefinition("head_token_recipe", RecipeDefinition.Type.SHAPELESS,
                "head_token", 4, List.of(), Map.of('P', "PAPER", 'I', "INK_SAC"), "head_collecting"));
        registerRecipe(api, new RecipeDefinition("artisan_head_recipe", RecipeDefinition.Type.SHAPELESS,
                "artisan_head", 1, List.of(), Map.of('T', "mechanium:head_token", 'S', "SKELETON_SKULL"), "head_collecting"));
        registerRecipe(api, new RecipeDefinition("head_display_recipe", RecipeDefinition.Type.SHAPELESS,
                "head_display", 2, List.of(), Map.of('S', "STICK", 'I', "IRON_INGOT"), "head_exhibitions"));
        registerRecipe(api, new RecipeDefinition("head_collection_case_recipe", RecipeDefinition.Type.SHAPED,
                "head_collection_case", 1, List.of("PPP", "P C", "PPP"),
                Map.of('P', "GLASS_PANE", 'C', "CHEST"), "head_exhibitions"));
    }
}
