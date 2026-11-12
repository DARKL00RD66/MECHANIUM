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
        java.util.Set<String> knownIds = new java.util.HashSet<>();
        for (MechaniumModule module : modules) {
            if (module.id() == null || module.id().isBlank() || !knownIds.add(module.id())) {
                throw new IllegalArgumentException("Módulo interno com ID vazio ou duplicado: " + module.id());
            }
        }

        for (MechaniumModule module : modules) {
            for (String dependency : module.dependencies()) {
                if (dependency == null || dependency.isBlank()) {
                    throw new IllegalArgumentException("Dependência vazia no módulo: " + module.id());
                }
                if (!knownIds.contains(dependency)) {
                    throw new IllegalArgumentException("Módulo " + module.id()
                            + " declara dependência interna inexistente: " + dependency);
                }
                if (dependency.equals(module.id())) {
                    throw new IllegalArgumentException("Módulo não pode depender de si mesmo: " + module.id());
                }
            }
        }

        java.util.Set<String> available = new java.util.HashSet<>();
        for (MechaniumModule module : modules) {
            boolean requested = config.getBoolean("modules." + module.id() + ".enabled", !module.optional());
            if (!requested) {
                context.logger().info("Módulo desativado pela configuração: " + module.id());
                continue;
            }

            java.util.Set<String> missing = new java.util.TreeSet<>(module.dependencies());
            missing.removeAll(available);
            if (!missing.isEmpty()) {
                String reason = "dependências internas desativadas ou ainda indisponíveis: " + missing;
                if (module.optional()) {
                    context.logger().warning("Módulo opcional " + module.id() + " ignorado; " + reason);
                    continue;
                }
                disable();
                throw new IllegalStateException("Módulo obrigatório " + module.id() + " não pode iniciar; " + reason);
            }

            // Track before onEnable so partially initialized modules receive onDisable.
            enabled.add(module);
            try {
                module.onEnable(context);
                available.add(module.id());
            } catch (Exception exception) {
                enabled.remove(module);
                try {
                    module.onDisable();
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
                if (!module.optional()) {
                    disable();
                    throw new IllegalStateException("Falha ao iniciar módulo obrigatório: " + module.id(), exception);
                }
                context.logger().severe("Módulo opcional " + module.id()
                        + " foi desativado: " + exception.getMessage());
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
