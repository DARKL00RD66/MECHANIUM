package dev.mechanium.addons;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class AddonDependencyClassLoaderTest {
    @Test
    void exposesClassesFromDeclaredDependencies() throws Exception {
        ClassLoader dependency = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(DependencyMarker.class.getName())) return DependencyMarker.class;
                return super.loadClass(name);
            }
        };

        AddonDependencyClassLoader loader =
                new AddonDependencyClassLoader(getClass().getClassLoader(), List.of(dependency));

        assertSame(DependencyMarker.class, loader.loadClass(DependencyMarker.class.getName()));
    }

    @Test
    void keepsJavaPlatformClassesParentFirst() throws Exception {
        AddonDependencyClassLoader loader =
                new AddonDependencyClassLoader(getClass().getClassLoader(), List.of());

        assertSame(String.class, loader.loadClass("java.lang.String"));
    }

    static final class DependencyMarker { }
}
