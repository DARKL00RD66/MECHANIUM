package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native MECHANIUM module: slime-customizer. */
final class SlimeCustomizerModule extends AbstractBuiltinModule {
    @Override public String id() { return "slime-customizer"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("content_templates", "Modelos de conteúdo", "renaissance", 24, List.of()),
            new TechnologyDefinition("content_workshop", "Oficina de personalização", "industrial_revolution", 36, List.of())
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("slime_customizer_goods", "Personalização de conteúdo", "Modelos e ferramentas de organização para conteúdo criado por addons"));
        registerItem(api, new ItemDefinition("template_page", "&ePágina de modelo", "PAPER", List.of("&8Conteúdo do módulo slime-customizer"), 0, 64, "slime_customizer_goods", "content_templates"));
        registerItem(api, new ItemDefinition("blank_blueprint", "&eProjeto em branco", "MAP", List.of("&8Conteúdo do módulo slime-customizer"), 0, 64, "slime_customizer_goods", "content_templates"));
        registerItem(api, new ItemDefinition("customizer_tool", "&eFerramenta de personalização", "SMITHING_TABLE", List.of("&8Conteúdo do módulo slime-customizer"), 0, 64, "slime_customizer_goods", "content_workshop"));
        registerItem(api, new ItemDefinition("content_catalog", "&eCatálogo de conteúdo", "BOOK", List.of("&8Conteúdo do módulo slime-customizer"), 0, 64, "slime_customizer_goods", "content_workshop"));
        registerRecipe(api, new RecipeDefinition("template_page_recipe", RecipeDefinition.Type.SHAPELESS, "template_page", 4, List.of(), Map.of('P', "PAPER", 'I', "INK_SAC"), "content_templates"));
        registerRecipe(api, new RecipeDefinition("blank_blueprint_recipe", RecipeDefinition.Type.SHAPELESS, "blank_blueprint", 1, List.of(), Map.of('P', "PAPER", 'C', "COMPASS"), "content_templates"));
        registerRecipe(api, new RecipeDefinition("customizer_tool_recipe", RecipeDefinition.Type.SHAPED, "customizer_tool", 1, List.of(" I ", "R S", " I "), Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'S', "SMITHING_TABLE"), "content_workshop"));
        registerRecipe(api, new RecipeDefinition("content_catalog_recipe", RecipeDefinition.Type.SHAPELESS, "content_catalog", 1, List.of(), Map.of('B', "BOOK", 'P', "mechanium:template_page", 'M', "mechanium:blank_blueprint"), "content_workshop"));
    }
}
