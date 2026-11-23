package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract Minecraft power-generation gameplay. Fuel cells are fictional game items;
 * this module contains no real reactor design or operating instructions.
 */
final class BetterNuclearReactorModule extends AbstractBuiltinModule {
    @Override public String id() { return "better-nuclear-reactor"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "eco-power"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("civil_nuclear_generation", "Geração nuclear civil",
                        "atomic_age", 66, List.of("nuclear_energy", "energy_accumulation")),
                new TechnologyDefinition("sealed_fuel_cells", "Células energéticas seladas",
                        "atomic_age", 74, List.of("civil_nuclear_generation"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("advanced_energy", "Energia avançada",
                "Geradores abstratos e componentes de armazenamento energético"));

        registerItem(api, new ItemDefinition("atomic_reactor", "&bGerador atômico civil", "BLAST_FURNACE",
                List.of("&8Gerador energético MECHANIUM", "&7Use uma célula no espaço de combustível",
                        "&7Conecte uma bateria adjacente do mesmo proprietário"),
                0, 1, "advanced_energy", "civil_nuclear_generation"));
        registerItem(api, new ItemDefinition("reactor_fuel_cell", "&eCélula energética selada", "BLAZE_ROD",
                List.of("&8Combustível fictício de uso no jogo", "&7Fornece energia ao gerador por tempo limitado"),
                0, 16, "advanced_energy", "sealed_fuel_cells"));

        registerRecipe(api, new RecipeDefinition("atomic_reactor_recipe", RecipeDefinition.Type.SHAPED,
                "atomic_reactor", 1, List.of("ICI", "NBN", "IRI"),
                Map.of('I', "IRON_BLOCK", 'C', "COPPER_BLOCK", 'N', "NETHERITE_INGOT",
                        'B', "BLAST_FURNACE", 'R', "REDSTONE_BLOCK"),
                "civil_nuclear_generation"));
        registerRecipe(api, new RecipeDefinition("reactor_fuel_cell_recipe", RecipeDefinition.Type.SHAPELESS,
                "reactor_fuel_cell", 2, List.of(),
                Map.of('B', "BLAZE_ROD", 'R', "REDSTONE", 'C', "COPPER_INGOT"),
                "sealed_fuel_cells"));
    }
}
