package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Additional logistics/storage components that plug into the existing protected storage network. */
final class NetworksModule extends AbstractBuiltinModule {
    @Override public String id() { return "networks"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("simple-storage"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("network_routing", "Roteamento logístico", "information_age",
                        62, List.of("portable_warehousing")),
                new TechnologyDefinition("logistics_filtering", "Filtragem logística", "digital_age",
                        74, List.of("network_routing", "automated_control"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("logistics_networks", "Redes logísticas",
                "Expansões para redes de armazenamento protegidas"));

        registerItem(api, new ItemDefinition("network_storage_hub", "&bHub de armazenamento em rede",
                "BARREL", List.of("&8Nó avançado de armazenamento", "&7Pode integrar redes MECHANIUM face a face",
                        "&7Compartilha propriedade e permissões da rede"),
                0, 1, "logistics_networks", "network_routing"));
        registerItem(api, new ItemDefinition("logistics_filter", "&eFiltro logístico",
                "HOPPER", List.of("&8Componente de classificação logística",
                        "&7Peça de expansão para futuras regras de roteamento"),
                0, 16, "logistics_networks", "logistics_filtering"));
        registerItem(api, new ItemDefinition("cargo_manifest", "&fManifesto de carga",
                "PAPER", List.of("&8Registro de inventário e remessas"),
                0, 64, "logistics_networks", "network_routing"));

        registerRecipe(api, new RecipeDefinition("network_storage_hub_recipe", RecipeDefinition.Type.SHAPED,
                "network_storage_hub", 1, List.of("IRI", "RBR", "ICI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'B', "mechanium:simple_storage_node",
                        'C', "COPPER_INGOT"), "network_routing"));
        registerRecipe(api, new RecipeDefinition("logistics_filter_recipe", RecipeDefinition.Type.SHAPED,
                "logistics_filter", 1, List.of(" I ", "RHR", " I "),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'H', "HOPPER"),
                "logistics_filtering"));
        registerRecipe(api, new RecipeDefinition("cargo_manifest_recipe", RecipeDefinition.Type.SHAPELESS,
                "cargo_manifest", 4, List.of(),
                Map.of('P', "PAPER", 'C', "CHARCOAL", 'R', "REDSTONE"),
                "network_routing"));
    }
}
