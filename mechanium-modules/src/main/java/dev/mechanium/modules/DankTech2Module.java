package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Portable-storage expansion inspired by DankTech2, implemented as a two-tier inventory upgrade. */
final class DankTech2Module extends AbstractBuiltinModule {
    @Override public String id() { return "dank-tech2"; }

    @Override public boolean optional() { return true; }

    @Override public Set<String> dependencies() { return Set.of("simple-storage"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition(
                "portable_storage_expansion", "Expansão de armazém portátil",
                "information_age", 68, List.of("portable_warehousing")));
    }

    @Override protected void registerExtra(MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "portable_storage_expansions", "Expansões de armazenamento portátil",
                "Contêineres pessoais de maior capacidade"));

        registerItem(api, new ItemDefinition(
                "portable_storage_advanced", "&dArmazém portátil avançado", "SHULKER_BOX",
                List.of("&8Contêiner pessoal de 54 espaços",
                        "&7Capacidade ampliada em relação ao armazém comum",
                        "&7O conteúdo continua salvo no próprio item"),
                0, 1, "portable_storage_expansions", "portable_storage_expansion"));

        registerRecipe(api, new RecipeDefinition(
                "portable_storage_advanced_recipe", RecipeDefinition.Type.SHAPED,
                "portable_storage_advanced", 1,
                List.of("SSS", "SPS", "SES"),
                Map.of('S', "SHULKER_SHELL", 'P', "mechanium:portable_storage", 'E', "ENDER_CHEST"),
                "portable_storage_expansion"));
    }
}
