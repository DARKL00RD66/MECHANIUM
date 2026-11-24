package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.*;
import java.util.List;

abstract class AbstractBuiltinModule implements MechaniumModule {
    protected abstract List<TechnologyDefinition> technologies();
    protected void registerExtra(MechaniumModuleContext context) { }


    protected final void registerCategory(MechaniumApi api, ContentCategory value) {
        requireRegistered(api.registerCategory(value), "categoria", value.id());
    }

    protected final void registerItem(MechaniumApi api, ItemDefinition value) {
        requireRegistered(api.registerItem(value), "item", value.id());
    }

    protected final void registerRecipe(MechaniumApi api, RecipeDefinition value) {
        requireRegistered(api.registerRecipe(value), "receita", value.id());
    }

    protected final void registerMobDrop(MechaniumApi api, MobDropDefinition value) {
        requireRegistered(api.registerMobDrop(value), "regra de drop", value.id());
    }

    protected final void registerMachine(MechaniumApi api, MachineDefinition value) {
        requireRegistered(api.registerMachine(value), "máquina", value.id());
    }

    protected final void registerFluid(MechaniumApi api, FluidDefinition value) {
        requireRegistered(api.registerFluid(value), "fluido", value.id());
    }

    private void requireRegistered(boolean registered, String type, String contentId) {
        if (!registered) {
            throw new IllegalStateException("Conflito ao registrar " + type + " no módulo "
                    + id() + ": identificador já existe com outra definição (" + contentId + ").");
        }
    }

    @Override
    public final void onEnable(MechaniumModuleContext context) {
        // Register technologies before items/recipes so their progression requirements can be validated.
        for (TechnologyDefinition technology : technologies()) {
            if (!context.api().registerTechnology(technology)) {
                throw new IllegalStateException("Technology ID conflict: " + technology.id());
            }
        }
        registerExtra(context);
        context.api().registerModule(id());
        context.logger().info("Módulo MECHANIUM ativado: " + id());
    }
    @Override public void onDisable() { }
}