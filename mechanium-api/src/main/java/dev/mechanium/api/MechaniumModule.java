package dev.mechanium.api;

/** Lifecycle contract for an official internal module shipped inside MECHANIUM.jar. */
public interface MechaniumModule {
    String id();
    default boolean optional() { return false; }
    /** Other internal modules that must be enabled before this module. */
    default java.util.Set<String> dependencies() { return java.util.Set.of(); }
    void onEnable(MechaniumModuleContext context) throws Exception;
    void onDisable();
}