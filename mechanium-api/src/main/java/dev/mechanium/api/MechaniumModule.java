package dev.mechanium.api;

/** Lifecycle contract for an official internal module shipped inside MECHANIUM.jar. */
public interface MechaniumModule {
    String id();
    default boolean optional() { return false; }
    void onEnable(MechaniumModuleContext context) throws Exception;
    void onDisable();
}