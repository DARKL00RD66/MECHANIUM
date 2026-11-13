package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Practical field-tool recipes; active tool abilities are intentionally not implied by item names. */
final class SensibleToolboxModule extends AbstractBuiltinModule {
    @Override public String id() { return "sensible-toolbox"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("field_tooling", "Ferramentas de campo", "medieval", 10, List.of()),
                new TechnologyDefinition("maintenance_workshop", "Oficina de manutenção", "renaissance", 22, List.of("field_tooling", "basic_mechanics"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("field_toolbox", "Ferramentas de campo", "Kits de utilidade e manutenção"));
        registerItem(api, new ItemDefinition("field_toolkit", "&eKit de campo", "CHEST",
                List.of("&8Conjunto de ferramentas e materiais de apoio"), 0, 1, "field_toolbox", "field_tooling"));
        registerItem(api, new ItemDefinition("maintenance_kit", "&6Kit de manutenção", "ANVIL",
                List.of("&8Conjunto de peças para manutenção de equipamentos"), 0, 1, "field_toolbox", "maintenance_workshop"));
        registerItem(api, new ItemDefinition("technical_manual", "&fManual técnico", "BOOK",
                List.of("&8Manual de consulta para trabalhadores especializados"), 0, 16, "field_toolbox", "maintenance_workshop"));

        registerRecipe(api, new RecipeDefinition("field_toolkit_recipe", RecipeDefinition.Type.SHAPED,
                "field_toolkit", 1, List.of("PIP", "ICI", "PIP"),
                Map.of('P', "PAPER", 'I', "IRON_INGOT", 'C', "CHEST"), "field_tooling"));
        registerRecipe(api, new RecipeDefinition("maintenance_kit_recipe", RecipeDefinition.Type.SHAPED,
                "maintenance_kit", 1, List.of(" I ", "ICI", " I "),
                Map.of('I', "IRON_INGOT", 'C', "mechanium:mechanical_parts"), "maintenance_workshop"));
        registerRecipe(api, new RecipeDefinition("technical_manual_recipe", RecipeDefinition.Type.SHAPELESS,
                "technical_manual", 1, List.of(), Map.of('B', "BOOK", 'R', "REDSTONE", 'I', "IRON_INGOT"),
                "maintenance_workshop"));
    }
}
