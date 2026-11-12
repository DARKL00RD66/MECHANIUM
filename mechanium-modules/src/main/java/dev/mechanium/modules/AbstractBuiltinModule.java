package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

abstract class AbstractBuiltinModule implements MechaniumModule {
    protected abstract List<TechnologyDefinition> technologies();
    protected void registerExtra(MechaniumModuleContext context) { }

    @Override
    public final void onEnable(MechaniumModuleContext context) {
        // Optional eras must exist before their technologies are validated.
        registerExtra(context);
        for (TechnologyDefinition technology : technologies()) {
            if (!context.api().registerTechnology(technology)) {
                throw new IllegalStateException("Technology ID conflict: " + technology.id());
            }
        }
        context.api().registerModule(id());
        context.logger().info("Módulo MECHANIUM ativado: " + id());
    }
    @Override public void onDisable() { }
}