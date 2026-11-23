package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModule;
import java.util.List;

/** Catalogue of official modules embedded in the single MECHANIUM JAR. */
public final class BuiltinModules {
    private BuiltinModules() { }
    public static List<MechaniumModule> create() {
        return List.of(new IndustryModule(), new GeologyModule(), new AgricultureModule(), new ExoticGardenModule(), new EnergyModule(),
                new StorageModule(), new EnvironmentModule(),
                new OptionalFantasyModule(), new OptionalFuturisticModule(), new CulinaryModule(), new OreProcessingModule(),
                new ExtraGearModule(), new EcoPowerModule(), new SFCalcModule(), new LiquidModule(),
                new AdvancedTechModule(), new SimpleStorageModule(), new SensibleToolboxModule(),
                new DankTech2Module(), new ElectricSpawnersModule(), new FastMachinesModule(), new HardcoreSlimefunModule());
    }
}