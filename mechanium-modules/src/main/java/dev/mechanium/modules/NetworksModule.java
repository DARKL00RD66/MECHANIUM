package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Additional nodes for the owner-protected, connected storage network. */
final class NetworksModule extends AbstractBuiltinModule {
    @Override public String id() { return "networks"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("simple-storage", "industrial-logistics"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("distributed_storage", "Armazenamento distribuído",
                "information_age", 65, List.of("industrial_logistics")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("networks", "Redes de armazenamento",
                "Nós adicionais para redes pessoais de depósitos"));
        registerItem(api, new ItemDefinition("network_storage_node", "&bNó de rede avançado", "BARREL",
                List.of("&8Depósito conectado à rede pessoal", "&7Conecte por uma das seis faces",
                        "&7Compartilha a rede de nós do mesmo proprietário"),
                0, 1, "networks", "distributed_storage"));
        registerItem(api, new ItemDefinition("network_link", "&eConector de rede", "COMPARATOR",
                List.of("&8Componente de roteamento logístico"),
                0, 16, "networks", "distributed_storage"));
        registerRecipe(api, new RecipeDefinition("network_storage_node_recipe", RecipeDefinition.Type.SHAPED,
                "network_storage_node", 1, List.of("IRI", "RBR", "IRI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'B', "BARREL"),
                "distributed_storage"));
        registerRecipe(api, new RecipeDefinition("network_link_recipe", RecipeDefinition.Type.SHAPELESS,
                "network_link", 2, List.of(),
                Map.of('C', "COPPER_INGOT", 'R', "REDSTONE", 'Q', "QUARTZ"),
                "distributed_storage"));
    }
}
