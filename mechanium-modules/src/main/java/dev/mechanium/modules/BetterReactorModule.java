package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A deliberately abstract, game-only high-output generator.
 * It uses fictional crystal fuel and does not model real reactor engineering.
 */
final class BetterReactorModule extends AbstractBuiltinModule {
    @Override public String id() { return "better-reactor"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("energy", "eco-power", "advanced-metallurgy"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("crystal_energy_conversion", "Conversão de energia cristalina",
                        "atomic_age", 62, List.of("energy_accumulation", "advanced_metallurgy")),
                new TechnologyDefinition("high_output_generation", "Geração de alta potência",
                        "atomic_age", 76, List.of("crystal_energy_conversion"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("advanced_generation", "Geração avançada",
                "Fonte energética fictícia de alta potência para o universo do jogo"));
        registerItem(api, new ItemDefinition("reactor_crystal", "&dCristal energético",
                "AMETHYST_SHARD", List.of("&8Combustível fictício de jogo", "&7Uma unidade mantém o gerador por cinco minutos"),
                0, 64, "advanced_generation", "crystal_energy_conversion"));
        registerItem(api, new ItemDefinition("reactor_casing", "&7Carcaça de gerador",
                "IRON_BLOCK", List.of("&8Estrutura de proteção para o gerador avançado"),
                0, 64, "advanced_generation", "crystal_energy_conversion"));
        registerItem(api, new ItemDefinition("abstract_reactor", "&dGerador cristalino",
                "BLAST_FURNACE", List.of("&8Gerador abstrato de alta potência", "&7Consome um cristal energético por ciclo de combustível"),
                0, 1, "advanced_generation", "high_output_generation"));

        registerRecipe(api, new RecipeDefinition("reactor_crystal_recipe", RecipeDefinition.Type.SHAPELESS,
                "reactor_crystal", 2, List.of(),
                Map.of('A', "AMETHYST_SHARD", 'G', "GLOWSTONE_DUST", 'R', "REDSTONE"),
                "crystal_energy_conversion"));
        registerRecipe(api, new RecipeDefinition("reactor_casing_recipe", RecipeDefinition.Type.SHAPED,
                "reactor_casing", 4, List.of("III", "I I", "III"),
                Map.of('I', "IRON_INGOT"), "crystal_energy_conversion"));
        registerRecipe(api, new RecipeDefinition("abstract_reactor_recipe", RecipeDefinition.Type.SHAPED,
                "abstract_reactor", 1, List.of("ICI", "CBC", "IRI"),
                Map.of('I', "mechanium:reactor_casing", 'C', "mechanium:reactor_crystal", 'B', "BLAST_FURNACE", 'R', "REDSTONE_BLOCK"),
                "high_output_generation"));
    }
}
