package dev.mechanium.addons;

import dev.mechanium.api.MechaniumAddon;
import dev.mechanium.api.MechaniumAddonContext;
import dev.mechanium.api.MechaniumContentRegistrationScope;
import dev.mechanium.api.MechaniumApi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Loads third-party addons from plugins/MECHANIUM/addons.
 * The parent-first classloader shares the host's public API classes.
 */
public final class MechaniumAddonLoader implements AutoCloseable {
    public static final String DESCRIPTOR = "mechanium-addon.properties";

    private final MechaniumApi api;
    private final ClassLoader parent;
    private final Path addonDirectory;
    private final Logger logger;
    private final List<LoadedAddon> loaded = new ArrayList<>();
    private final Map<String, LoadedAddon> loadedById = new HashMap<>();

    public MechaniumAddonLoader(MechaniumApi api, ClassLoader parent, Path addonDirectory, Logger logger) {
        this.api = Objects.requireNonNull(api, "api");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.addonDirectory = Objects.requireNonNull(addonDirectory, "addonDirectory");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void loadAll(MechaniumAddonContext baseContext) throws IOException {
        Files.createDirectories(addonDirectory);
        Map<String, AddonCandidate> pending = discover();
        Set<String> available = new HashSet<>(api.getRegisteredModules());

        // Stable topological loading. Unknown dependencies and cycles are skipped,
        // rather than preventing MECHANIUM itself from starting.
        while (!pending.isEmpty()) {
            boolean loadedOne = false;
            Iterator<Map.Entry<String, AddonCandidate>> iterator = pending.entrySet().iterator();
            while (iterator.hasNext()) {
                AddonCandidate candidate = iterator.next().getValue();
                Set<String> missing = new TreeSet<>(candidate.dependencies());
                missing.removeAll(available);
                if (!missing.isEmpty()) {
                    boolean dependencyCanStillAppear = missing.stream().anyMatch(pending::containsKey);
                    if (dependencyCanStillAppear) continue;
                    logger.warning("Addon " + candidate.id() + " ignorado; dependências ausentes: " + missing);
                    iterator.remove();
                    continue;
                }
                if (load(candidate, baseContext)) available.add(candidate.id());
                iterator.remove();
                loadedOne = true;
            }
            if (!loadedOne && !pending.isEmpty()) {
                logger.severe("Dependências circulares ou bloqueadas entre addons: " + pending.keySet());
                break;
            }
        }
    }

    private Map<String, AddonCandidate> discover() throws IOException {
        Map<String, AddonCandidate> candidates = new TreeMap<>();
        try (var stream = Files.list(addonDirectory)) {
            for (Path path : stream.filter(file -> file.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    var descriptor = jar.getJarEntry(DESCRIPTOR);
                    if (descriptor == null) {
                        logger.warning("Addon ignorado sem " + DESCRIPTOR + ": " + path.getFileName());
                        continue;
                    }
                    Properties properties = new Properties();
                    try (InputStream input = jar.getInputStream(descriptor)) {
                        properties.load(input);
                    }
                    AddonCandidate candidate = AddonCandidate.from(path, properties);
                    if (candidates.putIfAbsent(candidate.id(), candidate) != null) {
                        logger.warning("ID de addon duplicado; mantendo o primeiro: " + candidate.id());
                    }
                } catch (IOException | IllegalArgumentException exception) {
                    logger.warning("Addon inválido em " + path.getFileName() + ": " + exception.getMessage());
                }
            }
        }
        return candidates;
    }

    private boolean load(AddonCandidate candidate, MechaniumAddonContext baseContext) {
        URLClassLoader classLoader = null;
        MechaniumAddon instance = null;
        try {
            List<ClassLoader> dependencyLoaders = candidate.dependencies().stream()
                    .map(loadedById::get)
                    .filter(Objects::nonNull)
                    .map(LoadedAddon::classLoader)
                    .map(loader -> (ClassLoader) loader)
                    .toList();
            ClassLoader dependencyParent = new AddonDependencyClassLoader(parent, dependencyLoaders);
            classLoader = new URLClassLoader(new java.net.URL[]{candidate.jarPath().toUri().toURL()}, dependencyParent);
            Class<?> entryClass = Class.forName(candidate.mainClass(), true, classLoader);
            Object object = entryClass.getDeclaredConstructor().newInstance();
            if (!(object instanceof MechaniumAddon addon)) {
                throw new IllegalArgumentException("A classe main precisa implementar MechaniumAddon.");
            }
            Path dataDirectory = addonDirectory.resolve(candidate.id());
            Files.createDirectories(dataDirectory);
            MechaniumAddonContext context = new MechaniumAddonContext(
                    baseContext.api(), baseContext.hostPlugin(), dataDirectory,
                    Logger.getLogger("MECHANIUM.Addon." + candidate.id()));
            instance = addon;
            try (MechaniumContentRegistrationScope scope = api.openContentScope("addon:" + candidate.id())) {
                addon.onEnable(context);
                scope.commit();
            }
            LoadedAddon loadedAddon = new LoadedAddon(candidate.id(), addon, classLoader);
            loaded.add(loadedAddon);
            loadedById.put(candidate.id(), loadedAddon);
            logger.info("Addon externo carregado: " + candidate.id() + " v" + candidate.version());
            return true;
        } catch (Exception | LinkageError exception) {
            if (instance != null) {
                try { instance.onDisable(); } catch (RuntimeException ignored) { }
            }
            if (classLoader != null) {
                try { classLoader.close(); } catch (IOException ignored) { }
            }
            logger.severe("Falha ao carregar addon " + candidate.id() + ": " + exception.getMessage());
            return false;
        }
    }

    public List<String> loadedAddonIds() {
        return loaded.stream().map(LoadedAddon::id).toList();
    }

    @Override
    public void close() {
        for (int index = loaded.size() - 1; index >= 0; index--) {
            LoadedAddon addon = loaded.get(index);
            try {
                addon.instance().onDisable();
            } catch (RuntimeException exception) {
                logger.warning("Falha ao desligar addon " + addon.id() + ": " + exception.getMessage());
            }
            try {
                addon.classLoader().close();
            } catch (IOException exception) {
                logger.warning("Falha ao fechar classloader de " + addon.id() + ": " + exception.getMessage());
            }
        }
        loaded.clear();
        loadedById.clear();
    }

    private record LoadedAddon(String id, MechaniumAddon instance, URLClassLoader classLoader) { }

    private record AddonCandidate(Path jarPath, String id, String version, String mainClass,
                                  String apiVersion, Set<String> dependencies) {
        static AddonCandidate from(Path path, Properties properties) {
            String id = required(properties, "id").toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
                throw new IllegalArgumentException("ID inválido: " + id);
            }
            String apiVersion = required(properties, "api-version");
            if (!MechaniumApi.API_VERSION.equals(apiVersion)) {
                throw new IllegalArgumentException("API incompatível: " + apiVersion
                        + "; esta versão aceita " + MechaniumApi.API_VERSION);
            }
            Set<String> dependencies = new LinkedHashSet<>();
            String rawDependencies = properties.getProperty("dependencies", "").trim();
            if (!rawDependencies.isEmpty()) {
                for (String value : rawDependencies.split(",")) {
                    String dependency = value.trim().toLowerCase(Locale.ROOT);
                    if (!dependency.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
                        throw new IllegalArgumentException("Dependência inválida: " + value);
                    }
                    dependencies.add(dependency);
                }
            }
            return new AddonCandidate(path, id, required(properties, "version"),
                    required(properties, "main"), apiVersion, Set.copyOf(dependencies));
        }

        private static String required(Properties properties, String key) {
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Campo obrigatório ausente: " + key);
            return value.trim();
        }
    }
}
