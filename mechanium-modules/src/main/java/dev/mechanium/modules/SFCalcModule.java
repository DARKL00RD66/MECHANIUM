package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recipe inspection/calculation entry point, exposed through /mechanium calc. */
final class SFCalcModule extends AbstractBuiltinModule {
    @Override public String id() { return "sfcalc"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("technical_accounting", "Contabilidade técnica", "renaissance", 12, List.of("basic_mechanics")),
                new TechnologyDefinition("industrial_estimation", "Estimativa industrial", "industrial_revolution", 24, List.of("technical_accounting"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("technical_tools", "Ferramentas técnicas", "Instrumentos de cálculo e planejamento"));
        registerItem(api, new ItemDefinition("engineering_calculator", "&bCalculadora de engenharia", "CLOCK",
                List.of("&8Instrumento para consultar receitas", "&7Use /mechanium calc <id-da-receita>"),
                0, 1, "technical_tools", "technical_accounting"));
        registerItem(api, new ItemDefinition("engineering_notes", "&fCaderno técnico", "BOOK",
                List.of("&8Anotações e estimativas de produção"), 0, 16, "technical_tools", "industrial_estimation"));
        registerRecipe(api, new RecipeDefinition("engineering_calculator_recipe", RecipeDefinition.Type.SHAPED,
                "engineering_calculator", 1, List.of(" R ", "ICI", " R "),
                Map.of('R', "REDSTONE", 'I', "IRON_INGOT", 'C', "CLOCK"), "technical_accounting"));
        registerRecipe(api, new RecipeDefinition("engineering_notes_recipe", RecipeDefinition.Type.SHAPELESS,
                "engineering_notes", 2, List.of(),
                Map.of('P', "PAPER", 'I', "INK_SAC"), "industrial_estimation"));
    }
}
