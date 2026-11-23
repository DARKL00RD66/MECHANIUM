package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ore-fragment progression inspired by ore-chunk addon gameplay, implemented natively. */
final class OreChunksModule extends AbstractBuiltinModule {
    @Override public String id() { return "ore-chunks"; }
    @Override public Set<String> dependencies() { return Set.of("geology", "ore-processing"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("ore_fragmentation", "Fragmentação de minérios", "renaissance", 22, List.of("geological_surveying")),
            new TechnologyDefinition("ore_chunk_compaction", "Compactação de fragmentos", "industrial_revolution", 38, List.of("ore_fragmentation", "ore_crushing"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("ore_chunk_goods", "Fragmentos minerais", "Fragmentos compactáveis para cadeias de beneficiamento"));
        registerItem(api, new ItemDefinition("iron_ore_chunk", "&fFragmento de ferro", "RAW_IRON",
                List.of("&8Fragmento mineral obtido por processamento"), 0, 64, "ore_chunk_goods", "ore_fragmentation"));
        registerItem(api, new ItemDefinition("copper_ore_chunk", "&6Fragmento de cobre", "RAW_COPPER",
                List.of("&8Fragmento mineral obtido por processamento"), 0, 64, "ore_chunk_goods", "ore_fragmentation"));
        registerItem(api, new ItemDefinition("gold_ore_chunk", "&eFragmento de ouro", "RAW_GOLD",
                List.of("&8Fragmento mineral obtido por processamento"), 0, 64, "ore_chunk_goods", "ore_fragmentation"));
        registerItem(api, new ItemDefinition("iron_chunk_bundle", "&fLote de fragmentos de ferro", "IRON_BLOCK",
                List.of("&8Lote compactado de fragmentos de ferro"), 0, 16, "ore_chunk_goods", "ore_chunk_compaction"));
        registerItem(api, new ItemDefinition("copper_chunk_bundle", "&6Lote de fragmentos de cobre", "COPPER_BLOCK",
                List.of("&8Lote compactado de fragmentos de cobre"), 0, 16, "ore_chunk_goods", "ore_chunk_compaction"));
        registerRecipe(api, new RecipeDefinition("iron_ore_chunk_recipe", RecipeDefinition.Type.SHAPELESS,
                "iron_ore_chunk", 3, List.of(), Map.of('I', "RAW_IRON", 'G', "GRAVEL"), "ore_fragmentation"));
        registerRecipe(api, new RecipeDefinition("copper_ore_chunk_recipe", RecipeDefinition.Type.SHAPELESS,
                "copper_ore_chunk", 3, List.of(), Map.of('C', "RAW_COPPER", 'G', "GRAVEL"), "ore_fragmentation"));
        registerRecipe(api, new RecipeDefinition("gold_ore_chunk_recipe", RecipeDefinition.Type.SHAPELESS,
                "gold_ore_chunk", 2, List.of(), Map.of('G', "RAW_GOLD", 'S', "SAND"), "ore_fragmentation"));
        registerRecipe(api, new RecipeDefinition("iron_chunk_bundle_recipe", RecipeDefinition.Type.SHAPED,
                "iron_chunk_bundle", 1, List.of("III", "III", "III"), Map.of('I', "mechanium:iron_ore_chunk"), "ore_chunk_compaction"));
        registerRecipe(api, new RecipeDefinition("copper_chunk_bundle_recipe", RecipeDefinition.Type.SHAPED,
                "copper_chunk_bundle", 1, List.of("CCC", "CCC", "CCC"), Map.of('C', "mechanium:copper_ore_chunk"), "ore_chunk_compaction"));
    }
}
