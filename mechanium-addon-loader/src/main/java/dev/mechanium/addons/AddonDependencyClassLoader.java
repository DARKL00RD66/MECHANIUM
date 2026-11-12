package dev.mechanium.addons;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * Parent for an addon class loader. It exposes classes and resources from the
 * addon's declared dependencies without making unrelated addons visible.
 */
final class AddonDependencyClassLoader extends ClassLoader {
    private static final List<String> PARENT_FIRST = List.of(
            "java.", "javax.", "jdk.", "sun.", "org.bukkit.",
            "io.papermc.", "dev.mechanium.api."
    );

    private final List<ClassLoader> dependencies;

    AddonDependencyClassLoader(ClassLoader parent, List<ClassLoader> dependencies) {
        super(Objects.requireNonNull(parent, "parent"));
        this.dependencies = List.copyOf(dependencies);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) return loaded;

            if (isParentFirst(name)) {
                return super.loadClass(name, resolve);
            }

            for (ClassLoader dependency : dependencies) {
                try {
                    return dependency.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                    // Try the next declared dependency.
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader dependency : dependencies) {
            URL resource = dependency.getResource(name);
            if (resource != null) return resource;
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();
        for (ClassLoader dependency : dependencies) {
            Enumeration<URL> found = dependency.getResources(name);
            while (found.hasMoreElements()) {
                URL resource = found.nextElement();
                if (!resources.contains(resource)) resources.add(resource);
            }
        }
        Enumeration<URL> parentResources = getParent().getResources(name);
        while (parentResources.hasMoreElements()) {
            URL resource = parentResources.nextElement();
            if (!resources.contains(resource)) resources.add(resource);
        }
        return java.util.Collections.enumeration(resources);
    }

    private boolean isParentFirst(String name) {
        return PARENT_FIRST.stream().anyMatch(name::startsWith);
    }
}
