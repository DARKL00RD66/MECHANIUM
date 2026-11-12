package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.MechaniumContentRegistrationScope;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Enables configured modules in dependency order and shuts them down in reverse order. */
public final class ModuleManager {
    private final List<MechaniumModule> enabled = new ArrayList<>();

    public void enable(List<MechaniumModule> modules, FileConfiguration config, MechaniumModuleContext context) {
        List<MechaniumModule> ordered = orderByDependencies(modules);
        Set<String> available = new HashSet<>();

        for (MechaniumModule module : ordered) {
            boolean requested = config.getBoolean("modules." + module.id() + ".enabled", !module.optional());
            if (!requested) {
                context.logger().info("Módulo desativado pela configuração: " + module.id());
                continue;
            }

            Set<String> missing = new java.util.TreeSet<>(module.dependencies());
            missing.removeAll(available);
            if (!missing.isEmpty()) {
                String reason = "dependências internas desativadas ou indisponíveis: " + missing;
                if (module.optional()) {
                    context.logger().warning("Módulo opcional " + module.id() + " ignorado; " + reason);
                    continue;
                }
                disable();
                throw new IllegalStateException("Módulo obrigatório " + module.id()
                        + " não pode iniciar; " + reason);
            }

            // Track before onEnable so partially initialized modules receive onDisable.
            enabled.add(module);
            try (MechaniumContentRegistrationScope scope =
                         context.api().openContentScope("module:" + module.id())) {
                module.onEnable(context);
                scope.commit();
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

    static List<MechaniumModule> orderByDependencies(List<MechaniumModule> modules) {
        if (modules == null) throw new IllegalArgumentException("Lista de módulos não pode ser nula.");
        Map<String, MechaniumModule> byId = new LinkedHashMap<>();
        Map<String, Set<String>> dependenciesById = new HashMap<>();

        for (MechaniumModule module : modules) {
            if (module == null || module.id() == null || module.id().isBlank()
                    || byId.putIfAbsent(module.id(), module) != null) {
                throw new IllegalArgumentException("Módulo interno nulo, com ID vazio ou duplicado: "
                        + (module == null ? "null" : module.id()));
            }
        }

        for (MechaniumModule module : modules) {
            Set<String> declared = module.dependencies();
            if (declared == null) {
                throw new IllegalArgumentException("Módulo retornou dependências nulas: " + module.id());
            }
            Set<String> dependencies = new LinkedHashSet<>();
            for (String dependency : declared) {
                if (dependency == null || dependency.isBlank()) {
                    throw new IllegalArgumentException("Dependência vazia no módulo: " + module.id());
                }
                if (!byId.containsKey(dependency)) {
                    throw new IllegalArgumentException("Módulo " + module.id()
                            + " declara dependência interna inexistente: " + dependency);
                }
                if (dependency.equals(module.id())) {
                    throw new IllegalArgumentException("Módulo não pode depender de si mesmo: " + module.id());
                }
                dependencies.add(dependency);
            }
            dependenciesById.put(module.id(), Set.copyOf(dependencies));
        }

        List<MechaniumModule> ordered = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        while (ordered.size() < modules.size()) {
            boolean progress = false;
            for (MechaniumModule module : modules) {
                if (resolved.contains(module.id())) continue;
                if (resolved.containsAll(dependenciesById.get(module.id()))) {
                    ordered.add(module);
                    resolved.add(module.id());
                    progress = true;
                }
            }
            if (!progress) {
                Set<String> cyclic = new java.util.TreeSet<>(byId.keySet());
                cyclic.removeAll(resolved);
                throw new IllegalArgumentException("Dependências circulares entre módulos internos: " + cyclic);
            }
        }
        return List.copyOf(ordered);
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
