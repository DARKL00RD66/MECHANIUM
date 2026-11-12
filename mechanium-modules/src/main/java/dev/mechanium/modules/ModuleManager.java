package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import dev.mechanium.api.MechaniumModuleContext;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Enables configured modules and shuts them down in reverse order. */
public final class ModuleManager {
    private final List<MechaniumModule> enabled = new ArrayList<>();

    public void enable(List<MechaniumModule> modules, FileConfiguration config, MechaniumModuleContext context) {
        for (MechaniumModule module : modules) {
            boolean requested = config.getBoolean("modules." + module.id() + ".enabled", !module.optional());
            if (!requested) {
                context.logger().info("Módulo desativado pela configuração: " + module.id());
                continue;
            }
            try {
                module.onEnable(context);
                enabled.add(module);
            } catch (Exception exception) {
                if (!module.optional()) {
                    disable();
                    throw new IllegalStateException("Falha ao iniciar módulo obrigatório: " + module.id(), exception);
                }
                context.logger().severe("Módulo opcional " + module.id() + " foi desativado: " + exception.getMessage());
            }
        }
    }

    public List<String> enabledModuleIds() {
        return enabled.stream().map(MechaniumModule::id).toList();
    }

    public void disable() {
        for (int index = enabled.size() - 1; index >= 0; index--) {
            try {
                enabled.get(index).onDisable();
            } catch (RuntimeException ignored) {
                // Continue shutting down remaining modules.
            }
        }
        enabled.clear();
    }
}
