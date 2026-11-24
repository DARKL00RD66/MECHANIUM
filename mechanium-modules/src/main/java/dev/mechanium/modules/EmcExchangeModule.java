package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Native, independent equivalent-exchange progression.
 * Runtime behavior is supplied by the plugin's EMC ledger and custom station manager.
 */
final class EmcExchangeModule extends AbstractBuiltinModule {
    @Override public String id() { return "emc-exchange"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("equivalent_exchange", "Fundamentos da equivalência", "renaissance", 24, List.of()),
                new TechnologyDefinition("emc_dissolution", "Dissolução de matéria", "industrial_revolution", 32, List.of("equivalent_exchange")),
                new TechnologyDefinition("emc_synthesis", "Síntese de matéria", "industrial_revolution", 36, List.of("equivalent_exchange")),
                new TechnologyDefinition("emc_stabilization", "Estabilização de equivalência", "electrification", 48, List.of("emc_dissolution", "emc_synthesis")),
                new TechnologyDefinition("emc_refinement", "Refinamento de conversão", "modern", 60, List.of("emc_stabilization")),
                new TechnologyDefinition("emc_precision", "Síntese de alta precisão", "atomic_age", 74, List.of("emc_refinement")),
                new TechnologyDefinition("emc_high_density", "Equivalência de alta densidade", "information_age", 92, List.of("emc_precision"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "emc_exchange_machines", "Equivalência de matéria",
                "Estações de dissolução e síntese que usam saldo EMC individual"));

        registerItem(api, machine("emc_dissolver_t1", "Dissolvedor EMC • Nível I", "emc_dissolution"));
        registerItem(api, machine("emc_dissolver_t2", "Dissolvedor EMC • Nível II", "emc_stabilization"));
        registerItem(api, machine("emc_dissolver_t3", "Dissolvedor EMC • Nível III", "emc_refinement"));
        registerItem(api, machine("emc_dissolver_t4", "Dissolvedor EMC • Nível IV", "emc_precision"));
        registerItem(api, machine("emc_dissolver_t5", "Dissolvedor EMC • Nível V", "emc_high_density"));

        registerItem(api, machine("emc_materializer_t1", "Materializador EMC • Nível I", "emc_synthesis"));
        registerItem(api, machine("emc_materializer_t2", "Materializador EMC • Nível II", "emc_stabilization"));
        registerItem(api, machine("emc_materializer_t3", "Materializador EMC • Nível III", "emc_refinement"));
        registerItem(api, machine("emc_materializer_t4", "Materializador EMC • Nível IV", "emc_precision"));
        registerItem(api, machine("emc_materializer_t5", "Materializador EMC • Nível V", "emc_high_density"));

        registerRecipe(api, shaped("emc_dissolver_t1_recipe", "emc_dissolver_t1", "emc_dissolution",
                "IRI", "RDR", "IRI",
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DISPENSER")));
        registerRecipe(api, shaped("emc_dissolver_t2_recipe", "emc_dissolver_t2", "emc_stabilization",
                "CLC", "LDL", "CLC",
                Map.of('C', "COPPER_BLOCK", 'L', "LAPIS_BLOCK", 'D', "mechanium:emc_dissolver_t1")));
        registerRecipe(api, shaped("emc_dissolver_t3_recipe", "emc_dissolver_t3", "emc_refinement",
                "DMD", "MNM", "DMD",
                Map.of('D', "mechanium:emc_dissolver_t2", 'M', "DIAMOND", 'N', "NETHERITE_INGOT")));
        registerRecipe(api, shaped("emc_dissolver_t4_recipe", "emc_dissolver_t4", "emc_precision",
                "DBD", "BEB", "DBD",
                Map.of('D', "mechanium:emc_dissolver_t3", 'B', "BEACON", 'E', "EMERALD_BLOCK")));
        registerRecipe(api, shaped("emc_dissolver_t5_recipe", "emc_dissolver_t5", "emc_high_density",
                "DND", "NEN", "DND",
                Map.of('D', "mechanium:emc_dissolver_t4", 'N', "NETHERITE_BLOCK", 'E', "END_CRYSTAL")));

        registerRecipe(api, shaped("emc_materializer_t1_recipe", "emc_materializer_t1", "emc_synthesis",
                "GQG", "QDQ", "GQG",
                Map.of('G', "GOLD_INGOT", 'Q', "QUARTZ", 'D', "DISPENSER")));
        registerRecipe(api, shaped("emc_materializer_t2_recipe", "emc_materializer_t2", "emc_stabilization",
                "DED", "GAG", "DED",
                Map.of('D', "mechanium:emc_materializer_t1", 'E', "EMERALD", 'G', "GOLD_BLOCK", 'A', "AMETHYST_SHARD")));
        registerRecipe(api, shaped("emc_materializer_t3_recipe", "emc_materializer_t3", "emc_refinement",
                "DMD", "MEM", "DMD",
                Map.of('D', "mechanium:emc_materializer_t2", 'M', "DIAMOND", 'E', "EMERALD_BLOCK")));
        registerRecipe(api, shaped("emc_materializer_t4_recipe", "emc_materializer_t4", "emc_precision",
                "DND", "NEN", "DND",
                Map.of('D', "mechanium:emc_materializer_t3", 'N', "NETHERITE_INGOT", 'E', "END_CRYSTAL")));
        registerRecipe(api, shaped("emc_materializer_t5_recipe", "emc_materializer_t5", "emc_high_density",
                "DND", "NSN", "DND",
                Map.of('D', "mechanium:emc_materializer_t4", 'N', "NETHERITE_BLOCK", 'S', "NETHER_STAR")));
    }

    private ItemDefinition machine(String id, String name, String technology) {
        return new ItemDefinition(id, "&6" + name, "DISPENSER",
                List.of("&7Máquina de equivalência de matéria.",
                        "&7Converte ou sintetiza itens com EMC individual.",
                        "&8Nível limitado pela pesquisa e capacidade da estação."),
                0, 1, "emc_exchange_machines", technology);
    }

    private RecipeDefinition shaped(String id, String result, String technology,
                                    String row1, String row2, String row3, Map<Character, String> ingredients) {
        return new RecipeDefinition(id, RecipeDefinition.Type.SHAPED, result, 1,
                List.of(row1, row2, row3), ingredients, technology);
    }
}
