package org.example.mechanium;

import dev.mechanium.api.MechaniumAddon;
import dev.mechanium.api.MechaniumAddonContext;
import dev.mechanium.api.event.MechaniumTechnologyUnlockedEvent;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;

/**
 * Minimal external addon example.
 * The addon loader provides the MECHANIUM API through its parent classloader.
 */
public final class SampleAgricultureAddon implements MechaniumAddon {
    private Listener listener;

    @Override
    public void onEnable(MechaniumAddonContext context) {
        var api = context.api();

        api.registerCategory(new ContentCategory(
                "sample_agriculture", "Itens agrícolas de exemplo", "Conteúdo demonstrativo de addon"));

        api.registerItem(new ItemDefinition(
                "example_flour", "&eFarinha de demonstração", "PAPER",
                List.of("&8Exemplo de item registrado por addon"),
                0, 64, "sample_agriculture", "crop_rotation"));

        api.registerRecipe(new RecipeDefinition(
                "example_flour_recipe", RecipeDefinition.Type.SHAPELESS,
                "example_flour", 1, List.of(),
                Map.of('W', "WHEAT"), "crop_rotation"));

        listener = new Listener() {
            @EventHandler
            public void onTechnologyUnlocked(MechaniumTechnologyUnlockedEvent event) {
                context.logger().info("Jogador " + event.playerId()
                        + " desbloqueou " + event.technology().id()
                        + " (exemplo de listener externo).");
            }
        };
        context.hostPlugin().getServer().getPluginManager().registerEvents(listener, context.hostPlugin());
        context.logger().info("Sample Agriculture Addon habilitado.");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
    }
}
