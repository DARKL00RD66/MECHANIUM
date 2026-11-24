package dev.mechanium.plugin;

import dev.mechanium.addons.MechaniumAddonLoader;
import dev.mechanium.api.MechaniumAddonContext;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumDataStore;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.integration.NationIdentityProvider;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.ResearchProgress;
import dev.mechanium.api.model.ResearchResult;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.engine.DefaultMechaniumApi;
import dev.mechanium.modules.BuiltinModules;
import dev.mechanium.modules.ModuleManager;
import dev.mechanium.storage.SqliteDataStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MechaniumPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private MechaniumDataStore dataStore;
    private MechaniumApi api;
    private ModuleManager moduleManager;
    private MechaniumAddonLoader addonLoader;
    private MechaniumRecipeBridge recipeBridge;
    private MechaniumResearchMenu researchMenu;
    private MechaniumAdminMenu adminMenu;
    private MechaniumRecipeAdminMenu recipeAdminMenu;
    private SlimeCustomizerMenu slimeCustomizerMenu;
    private PrivateStorageListener privateStorageListener;
    private MechaniumMachineManager machineManager;
    private EmcExchangeManager emcExchangeManager;
    private EnergyGridManager energyGridManager;
    private GreenhouseManager greenhouseManager;
    private FluidPipeManager fluidPipeManager;
    private IndustrialNetworkManager industrialNetworkManager;
    private MaterialGeneratorManager materialGeneratorManager;
    private SpecialEquipmentListener specialEquipmentListener;
    private SimpleStorageNetworkListener simpleStorageNetworkListener;
    private TownyNationHook townyNationHook;
    private BukkitTask researchCompletionTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        mergeDefaultErasIntoExistingConfig();
        saveConfig();
        try {
            Path database = getDataFolder().toPath()
                    .resolve(getConfig().getString("storage.database", "mechanium.db"));
            dataStore = new SqliteDataStore(database, getLogger());
            api = new DefaultMechaniumApi(dataStore);
            migrateHeadCollectionModuleConfig();
            api.migrateContentOwner("module:geyser-heads", "module:head-collection");
            registerConfiguredEras();
            initializeServerEra();
            migrateVanillaRecipeGateSettings();
            migrateVanillaRecipeEraDistribution();
            seedMissingVanillaRecipeGates();
            Bukkit.getServicesManager().register(MechaniumApi.class, api, this, ServicePriority.Normal);

            moduleManager = new ModuleManager();
            moduleManager.enable(BuiltinModules.create(), getConfig(),
                    new MechaniumModuleContext(api, this, getDataFolder().toPath().resolve("modules"), getLogger()));
            if (api.getRegisteredModules().contains("geology")) {
                Bukkit.getPluginManager().registerEvents(new GeologicalSurveyListener(this, api), this);
            }

            addonLoader = new MechaniumAddonLoader(api, getClass().getClassLoader(),
                    getDataFolder().toPath().resolve(getConfig().getString("addons.directory", "addons")), getLogger());
            addonLoader.loadAll(new MechaniumAddonContext(api, this,
                    getDataFolder().toPath().resolve("addons"), getLogger()));

            // The customizer category is independent from the optional module. This keeps
            // existing user-created item definitions valid when the editor is temporarily disabled.
            try (var customizerScope = api.openContentScope("system:customizer")) {
                if (!api.registerCategory(new dev.mechanium.api.model.ContentCategory(
                        "customizer_user_items", "Itens personalizados",
                        "Itens criados por administradores com o editor MECHANIUM."))) {
                    throw new IllegalStateException("Conflito ao registrar a categoria customizer_user_items.");
                }
                customizerScope.commit();
            }

            Set<String> activeContentOwners = new HashSet<>();
            moduleManager.enabledModuleIds().forEach(id -> activeContentOwners.add("module:" + id));
            addonLoader.loadedAddonIds().forEach(id -> activeContentOwners.add("addon:" + id));
            activeContentOwners.add("system:customizer");
            for (String itemId : getConfig().getStringList("slime-customizer.user-item-ids")) {
                if (itemId != null && itemId.trim().toLowerCase(Locale.ROOT)
                        .matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
                    activeContentOwners.add(SlimeCustomizerMenu.ownerId(itemId));
                }
            }
            for (String recipeId : getConfig().getStringList("slime-customizer.user-recipe-ids")) {
                if (recipeId != null && recipeId.trim().toLowerCase(Locale.ROOT)
                        .matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
                    activeContentOwners.add(SlimeCustomizerMenu.recipeOwnerId(recipeId));
                }
            }
            api.pruneInactiveContentOwners(activeContentOwners);
            applyConfiguredRecipeOverrides();
            List<String> contentErrors = api.validateContent();
            if (!contentErrors.isEmpty()) throw new IllegalStateException("Conteúdo MECHANIUM inválido: " + String.join("; ", contentErrors));
            recipeBridge = new MechaniumRecipeBridge(this, api);
            recipeBridge.registerAll();
            Bukkit.getPluginManager().registerEvents(recipeBridge, this);

            Bukkit.getPluginManager().registerEvents(this, this);
            Bukkit.getPluginManager().registerEvents(new MechaniumContentListener(api), this);
            Bukkit.getPluginManager().registerEvents(new VanillaRecipeGateListener(this, api), this);
            Bukkit.getPluginManager().registerEvents(new ResearchPointRewardListener(this, api), this);
            Bukkit.getPluginManager().registerEvents(new MechaniumMobDropListener(api), this);
            energyGridManager = new EnergyGridManager(this, api);
            Bukkit.getPluginManager().registerEvents(energyGridManager, this);
            energyGridManager.start();
            if (moduleManager.enabledModuleIds().contains("greenhouse")) {
                greenhouseManager = new GreenhouseManager(this, api, energyGridManager);
                Bukkit.getPluginManager().registerEvents(greenhouseManager, this);
                greenhouseManager.start();
            }
            fluidPipeManager = new FluidPipeManager(this, api);
            Bukkit.getPluginManager().registerEvents(fluidPipeManager, this);
            fluidPipeManager.start();
            if (moduleManager.enabledModuleIds().contains("industrial-networks")) {
                industrialNetworkManager = new IndustrialNetworkManager(this, api);
                Bukkit.getPluginManager().registerEvents(industrialNetworkManager, this);
                industrialNetworkManager.start();
            }
            specialEquipmentListener = new SpecialEquipmentListener(this);
            Bukkit.getPluginManager().registerEvents(specialEquipmentListener, this);
            if (moduleManager.enabledModuleIds().contains("material-generators")) {
                materialGeneratorManager = new MaterialGeneratorManager(this, api);
                Bukkit.getPluginManager().registerEvents(materialGeneratorManager, this);
                materialGeneratorManager.start();
            }
            machineManager = new MechaniumMachineManager(this, api, energyGridManager);
            Bukkit.getPluginManager().registerEvents(machineManager, this);
            machineManager.start();
            if (moduleManager.enabledModuleIds().contains("emc-exchange")) {
                emcExchangeManager = new EmcExchangeManager(this, api,
                        getDataFolder().toPath().resolve("emc-exchange.db"),
                        getConfig().getConfigurationSection("emc-exchange"));
                Bukkit.getPluginManager().registerEvents(emcExchangeManager, this);
            }
            privateStorageListener = new PrivateStorageListener(this, api);
            Bukkit.getPluginManager().registerEvents(privateStorageListener, this);
            simpleStorageNetworkListener = new SimpleStorageNetworkListener(this);
            Bukkit.getPluginManager().registerEvents(simpleStorageNetworkListener, this);
            if (moduleManager.enabledModuleIds().contains("simple-storage")) {
                Bukkit.getPluginManager().registerEvents(new PortableStorageListener(this), this);
            }
            researchMenu = new MechaniumResearchMenu(this, api, emcExchangeManager);
            Bukkit.getPluginManager().registerEvents(researchMenu, this);
            researchCompletionTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                long now = System.currentTimeMillis();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    processDueResearches(online, now);
                    if (researchMenu != null) researchMenu.refreshProgress(online, now);
                }
            }, 20L, 20L);
            adminMenu = new MechaniumAdminMenu(this, api);
            Bukkit.getPluginManager().registerEvents(adminMenu, this);
            recipeAdminMenu = new MechaniumRecipeAdminMenu(this, api);
            Bukkit.getPluginManager().registerEvents(recipeAdminMenu, this);
            slimeCustomizerMenu = new SlimeCustomizerMenu(this, api);
            Bukkit.getPluginManager().registerEvents(slimeCustomizerMenu, this);
            townyNationHook = new TownyNationHook(this);
            townyNationHook.start();
            Bukkit.getServicesManager().register(NationIdentityProvider.class, townyNationHook, this, ServicePriority.Normal);
            PluginCommand command = Objects.requireNonNull(getCommand("mechanium"), "mechanium command missing");
            command.setExecutor(this);
            command.setTabCompleter(this);

            getLogger().info("MECHANIUM ativo | API " + api.getApiVersion()
                    + " | módulos: " + moduleManager.enabledModuleIds().size()
                    + " | addons externos: " + addonLoader.loadedAddonIds().size());
        } catch (Exception exception) {
            getLogger().severe("Falha ao iniciar MECHANIUM: " + exception.getMessage());
            exception.printStackTrace();
            shutdownServices();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Existing servers keep their old YAML list after upgrading. Merge newly shipped eras by ID,
     * while preserving custom eras and avoiding order collisions with administrator-defined eras.
     */
    private void mergeDefaultErasIntoExistingConfig() {
        var defaults = getConfig().getDefaults();
        if (defaults == null) return;

        List<Map<?, ?>> merged = new ArrayList<>(getConfig().getMapList("progression.eras"));
        Set<String> existingIds = new HashSet<>();
        Set<Integer> usedOrders = new HashSet<>();
        for (Map<?, ?> row : merged) {
            Object id = row.get("id");
            Object order = row.get("order");
            if (id instanceof String eraId) existingIds.add(eraId.toLowerCase(Locale.ROOT));
            if (order instanceof Number number) usedOrders.add(number.intValue());
        }
        int nextOrder = usedOrders.stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        for (Map<?, ?> row : defaults.getMapList("progression.eras")) {
            Object rawId = row.get("id");
            Object rawName = row.get("name");
            Object rawOrder = row.get("order");
            if (!(rawId instanceof String id) || !(rawName instanceof String name)
                    || !(rawOrder instanceof Number number)) continue;
            if (!existingIds.add(id.toLowerCase(Locale.ROOT))) continue;

            int order = number.intValue();
            if (usedOrders.contains(order)) {
                while (usedOrders.contains(nextOrder)) nextOrder++;
                order = nextOrder++;
            }
            usedOrders.add(order);
            merged.add(Map.of("id", id, "name", name, "order", order));
        }
        getConfig().set("progression.eras", merged);
    }

    /**
     * Renames the old cosmetic module without changing persisted item/recipe IDs.
     * Existing administrator enable/disable choices take precedence over the new default.
     */
    private void migrateHeadCollectionModuleConfig() {
        String migrationPath = "modules.migrations.head-collection-from-geyser-heads";
        if (getConfig().getBoolean(migrationPath, false)) return;

        String oldPath = "modules.geyser-heads.enabled";
        String newPath = "modules.head-collection.enabled";
        if (!getConfig().contains(newPath, true) && getConfig().contains(oldPath, true)) {
            getConfig().set(newPath, getConfig().getBoolean(oldPath, true));
        }
        getConfig().set(oldPath, null);
        getConfig().set(migrationPath, true);
        saveConfig();
        getLogger().info("Módulo cosmético renomeado para Head Collection; IDs de itens e receitas foram preservados.");
    }

    private void migrateVanillaRecipeGateSettings() {
        String migrationPath = "vanilla-recipe-gates.migrations.enchanting-table-mythic-era";
        if (getConfig().getBoolean(migrationPath, false)) return;

        String enchantingTablePath = "vanilla-recipe-gates.recipes.minecraft:enchanting_table";
        String configuredEra = getConfig().getString(enchantingTablePath, "");
        // Previous packaged defaults placed the enchanting table in the Renaissance.
        // Move only that old default; preserve any deliberate custom setting or "off".
        if ("renaissance".equalsIgnoreCase(configuredEra)) {
            getConfig().set(enchantingTablePath, "mythic_age");
            getLogger().info("Receita minecraft:enchanting_table corrigida para a Era Mítica.");
        }
        getConfig().set(migrationPath, true);
        saveConfig();
    }

    /**
     * Reclassifies values matching the previous automatic classifier and a small set of
     * known incorrect packaged defaults. "off" is preserved. Older manual overrides that
     * coincide with either former default cannot be distinguished from generated values,
     * so administrators should review the logged migration summary after upgrading.
     */
    private void migrateVanillaRecipeEraDistribution() {
        String migrationPath = "vanilla-recipe-gates.migrations.historical-distribution-0-7-4-02";
        if (getConfig().getBoolean(migrationPath, false)) return;

        int updated = 0;
        for (VanillaRecipeCatalog.Entry recipe : VanillaRecipeCatalog.discover()) {
            String configPath = "vanilla-recipe-gates.recipes." + recipe.key();
            String configuredEra = getConfig().getString(configPath);
            if (configuredEra == null || configuredEra.equalsIgnoreCase("off")) continue;

            String previousSuggestion = VanillaRecipeCatalog.legacyRecommendedEraId(
                    recipe.key().getKey(), recipe.result());
            String correctedSuggestion = recipe.suggestedEraId();
            if (configuredEra.equalsIgnoreCase(previousSuggestion)
                    && !configuredEra.equalsIgnoreCase(correctedSuggestion)) {
                getConfig().set(configPath, correctedSuggestion);
                updated++;
            }
        }

        // Correct the former packaged sample values that disagreed with the old/new classifier.
        Map<String, String> packagedDefaults = Map.of(
                "minecraft:anvil", "renaissance",
                "minecraft:blast_furnace", "modern",
                "minecraft:smoker", "modern",
                "minecraft:daylight_detector", "modern",
                "minecraft:observer", "modern");
        Map<String, String> correctedDefaults = Map.of(
                "minecraft:anvil", "medieval",
                "minecraft:blast_furnace", "industrial_revolution",
                "minecraft:smoker", "industrial_revolution",
                "minecraft:daylight_detector", "electrification",
                "minecraft:observer", "digital_age");
        for (Map.Entry<String, String> correction : correctedDefaults.entrySet()) {
            String configPath = "vanilla-recipe-gates.recipes." + correction.getKey();
            String configuredEra = getConfig().getString(configPath, "");
            if (packagedDefaults.get(correction.getKey()).equalsIgnoreCase(configuredEra)) {
                getConfig().set(configPath, correction.getValue());
                updated++;
            }
        }

        getConfig().set(migrationPath, true);
        saveConfig();
        getLogger().info("Rebalance histórico aplicado: " + updated
                + " regras vanilla atualizadas. Valores 'off' foram preservados; escolhas manuais que coincidiam "
                + "com uma sugestão automática antiga ou com um default empacotado devem ser revisadas.");
    }

    private void seedMissingVanillaRecipeGates() {
        if (!getConfig().getBoolean("vanilla-recipe-gates.auto-register", true)) return;

        int added = 0;
        for (VanillaRecipeCatalog.Entry recipe : VanillaRecipeCatalog.discover()) {
            String path = "vanilla-recipe-gates.recipes." + recipe.key();
            if (getConfig().contains(path)) continue;
            getConfig().set(path, recipe.suggestedEraId());
            added++;
        }
        if (added > 0) {
            saveConfig();
            getLogger().info("Catálogo vanilla atualizado: " + added
                    + " novas receitas receberam eras históricas sugeridas.");
        }
    }

    private void initializeServerEra() {
        String fallback = getConfig().getString("progression.server-era",
                getConfig().getString("progression.default-era", "ancient"));
        String selected = api.getServerEra().orElse(fallback);
        if (api.getEra(selected).isEmpty()) {
            getLogger().warning("Era global persistida inválida: " + selected + ". Usando fallback.");
            selected = getConfig().getString("progression.default-era", "ancient");
        }
        if (api.getEra(selected).isEmpty()) {
            selected = api.getEras().stream()
                    .min(Comparator.comparingInt(EraDefinition::order))
                    .orElseThrow(() -> new IllegalStateException("Nenhuma era disponível para inicializar o servidor."))
                    .id();
        }
        api.setServerEra(selected);
        getConfig().set("progression.server-era", selected);
        saveConfig();
        getLogger().info("Era global do servidor: "
                + api.getEra(selected).map(EraDefinition::displayName).orElse(selected));
    }

    String serverEraName() {
        return api.getServerEra().flatMap(api::getEra)
                .map(EraDefinition::displayName).orElse("Não definida");
    }

    void changeServerEra(String eraId) {
        EraDefinition era = api.getEra(eraId)
                .orElseThrow(() -> new IllegalArgumentException("Era inexistente: " + eraId));
        String current = api.getServerEra().orElse(null);
        if (era.id().equalsIgnoreCase(Objects.toString(current, ""))) return;

        api.setServerEra(era.id());
        getConfig().set("progression.server-era", era.id());
        long revision = Math.max(0L,
                getConfig().getLong("progression.global-era-announcement.revision", 0L)) + 1L;
        getConfig().set("progression.global-era-announcement.revision", revision);
        getConfig().set("progression.global-era-announcement.era-id", era.id());
        getConfig().set("progression.global-era-announcement.display-name", era.displayName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            showEraAnnouncement(online, era.displayName());
            getConfig().set("progression.global-era-announcement.seen."
                    + online.getUniqueId(), revision);
        }
        saveConfig();
    }

    private void deliverPendingGlobalEraAnnouncement(Player player) {
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) return;
            long revision = getConfig().getLong("progression.global-era-announcement.revision", 0L);
            if (revision <= 0L) return;
            String seenPath = "progression.global-era-announcement.seen." + player.getUniqueId();
            if (getConfig().getLong(seenPath, 0L) >= revision) return;
            String eraId = getConfig().getString("progression.global-era-announcement.era-id",
                    api.getServerEra().orElse("ancient"));
            String displayName = getConfig().getString("progression.global-era-announcement.display-name",
                    api.getEra(eraId).map(EraDefinition::displayName).orElse(eraId));
            showEraAnnouncement(player, displayName);
            getConfig().set(seenPath, revision);
            saveConfig();
        });
    }

    private void showEraAnnouncement(Player player, String displayName) {
        player.sendTitle("§6§lNova era", "§e" + displayName, 10, 80, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /** Applies administrator-managed recipe definitions after modules and addons register their canonical content. */
    private void applyConfiguredRecipeOverrides() {
        ConfigurationSection overrides = getConfig().getConfigurationSection("admin-recipe-overrides");
        if (overrides == null) return;
        for (String storageKey : overrides.getKeys(false)) {
            ConfigurationSection section = overrides.getConfigurationSection(storageKey);
            if (section == null) continue;
            String recipeId = section.getString("id", "");
            try {
                RecipeDefinition.Type type = RecipeDefinition.Type.valueOf(
                        section.getString("type", "SHAPED").toUpperCase(Locale.ROOT));
                String resultItemId = section.getString("result-item", "");
                int resultAmount = section.getInt("result-amount", 1);
                List<String> shape = section.getStringList("shape");
                Map<Character, String> ingredients = new LinkedHashMap<>();
                ConfigurationSection ingredientSection = section.getConfigurationSection("ingredients");
                if (ingredientSection != null) {
                    for (String symbol : ingredientSection.getKeys(false)) {
                        if (symbol.length() != 1) throw new IllegalArgumentException(
                                "Símbolo de ingrediente deve ter um caractere: " + symbol);
                        ingredients.put(symbol.charAt(0), ingredientSection.getString(symbol, ""));
                    }
                }
                String technology = section.getString("required-technology", "");
                RecipeDefinition override = new RecipeDefinition(recipeId, type, resultItemId,
                        resultAmount, shape, ingredients, technology.isBlank() ? null : technology);
                if (!api.overrideRecipe(override)) {
                    getLogger().warning("Override administrativo ignorado: receita não encontrada: " + recipeId);
                } else {
                    getLogger().info("Override administrativo aplicado à receita " + recipeId + ".");
                }
            } catch (RuntimeException exception) {
                getLogger().warning("Override administrativo inválido para '" + recipeId + "': "
                        + exception.getMessage());
            }
        }
    }


    long researchDurationMillis(TechnologyDefinition technology) {
        EraDefinition era = api.getEra(technology.requiredEraId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Era da tecnologia não encontrada: " + technology.requiredEraId()));
        return ResearchDurationCalculator.durationMillis(technology, era, getConfig());
    }

    String formatResearchDuration(long durationMillis) {
        long totalSeconds = durationMillis <= 0L ? 0L
                : durationMillis / 1_000L + (durationMillis % 1_000L == 0L ? 0L : 1L);
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) return hours + "h " + minutes + "m";
        if (minutes > 0L) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    void startResearch(Player player, TechnologyDefinition technology) {
        long duration = researchDurationMillis(technology);
        ResearchResult result = api.startResearch(player.getUniqueId(), technology.id(), duration);
        ChatColor color = result.status() == ResearchResult.Status.STARTED
                ? ChatColor.GREEN : ChatColor.YELLOW;
        player.sendMessage(color + result.message());
        if (result.status() == ResearchResult.Status.STARTED) {
            player.sendMessage(ChatColor.GRAY + "Tempo estimado: " + ChatColor.AQUA
                    + formatResearchDuration(duration) + ChatColor.GRAY
                    + ". O tempo continua contando mesmo enquanto você estiver offline.");
        }
    }

    void processDueResearches(Player player, long nowEpochMillis) {
        if (!player.isOnline() || api == null) return;
        Collection<ResearchProgress> active = api.getResearches(player.getUniqueId());
        if (active.stream().noneMatch(research -> research.completesAtEpochMillis() <= nowEpochMillis)) return;
        Collection<String> completed = api.completeDueResearches(player.getUniqueId(), nowEpochMillis);
        if (completed.isEmpty()) return;
        for (String technologyId : completed) {
            String name = api.getTechnology(technologyId)
                    .map(TechnologyDefinition::displayName).orElse(technologyId);
            player.sendMessage(ChatColor.GREEN + "Pesquisa concluída: " + ChatColor.GOLD + name
                    + ChatColor.GREEN + "!");
        }
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.15f);
    }

    /** Updates the runtime registry and saves a separate YAML override, leaving module-owned SQLite data intact. */
    boolean saveAdminRecipeOverride(RecipeDefinition replacement) {
        RecipeDefinition previous = api.getRecipe(replacement.id())
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada: " + replacement.id()));
        try {
            if (!api.overrideRecipe(replacement)) {
                throw new IllegalArgumentException("Não foi possível substituir a receita " + replacement.id() + ".");
            }
            String storageKey = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(replacement.id().getBytes(StandardCharsets.UTF_8));
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("id", replacement.id());
            values.put("type", replacement.type().name());
            values.put("result-item", replacement.resultItemId());
            values.put("result-amount", replacement.resultAmount());
            values.put("shape", replacement.shape());
            Map<String, String> ingredients = new LinkedHashMap<>();
            replacement.ingredients().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> ingredients.put(String.valueOf(entry.getKey()), entry.getValue()));
            values.put("ingredients", ingredients);
            values.put("required-technology", Objects.toString(replacement.requiredTechnologyId(), ""));
            getConfig().set("admin-recipe-overrides." + storageKey, values);
            saveConfig();
            return true;
        } catch (RuntimeException exception) {
            api.overrideRecipe(previous);
            throw exception;
        }
    }

    void openRecipeAdminMenu(Player player) {
        if (!hasAdminAccess(player)) {
            player.sendMessage(ChatColor.RED + "Sem permissão.");
            return;
        }
        if (recipeAdminMenu == null) {
            player.sendMessage(ChatColor.RED + "O editor de receitas ainda não está disponível.");
            return;
        }
        recipeAdminMenu.open(player);
    }

    void openSlimeCustomizerMenu(Player player) {
        if (!hasAdminAccess(player)) {
            player.sendMessage(ChatColor.RED + "Sem permissão.");
            return;
        }
        if (slimeCustomizerMenu == null) {
            player.sendMessage(ChatColor.RED + "O Slime Customizer ainda não está disponível.");
            return;
        }
        slimeCustomizerMenu.open(player);
    }

    private void registerConfiguredEras() {
        for (Map<?, ?> row : getConfig().getMapList("progression.eras")) {
            Object id = row.get("id");
            Object name = row.get("name");
            Object order = row.get("order");
            if (!(id instanceof String eraId) || !(name instanceof String displayName) || !(order instanceof Number number)) {
                throw new IllegalStateException("Entrada inválida em progression.eras: " + row);
            }
            if (!api.registerEra(new EraDefinition(eraId, displayName, number.intValue()))) {
                throw new IllegalStateException("Era duplicada: " + eraId);
            }
        }
        if (api.getEras().isEmpty()) throw new IllegalStateException("Nenhuma era configurada.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        processDueResearches(event.getPlayer(), System.currentTimeMillis());
        // Players without a test override automatically inherit the current server-wide era.
        deliverPendingGlobalEraAnnouncement(event.getPlayer());
        long startingPoints = Math.max(0L, getConfig().getLong("progression.starting-research-points", 40L));
        boolean hasResearch = !api.getResearches(playerId).isEmpty()
                || api.getTechnologies().stream()
                .anyMatch(technology -> api.hasUnlockedTechnology(playerId, technology.id()));
        // Existing players who never received points are bootstrapped too; spending the balance
        // cannot trigger another grant because a successful research remains recorded.
        if (startingPoints > 0 && api.getResearchPoints(playerId) == 0 && !hasResearch) {
            api.addResearchPoints(playerId, startingPoints);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Você recebeu " + startingPoints
                    + " pontos científicos iniciais do MECHANIUM.");
        }
        if (researchMenu != null) researchMenu.giveGuide(event.getPlayer());
        if (townyNationHook != null) townyNationHook.sync(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (researchMenu != null) researchMenu.open(player);
                else player.sendMessage(ChatColor.RED + "O menu MECHANIUM ainda não está disponível.");
            } else {
                sendHelp(sender);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "environment" -> {
                if (!moduleManager.enabledModuleIds().contains("environment")) {
                    sender.sendMessage(ChatColor.RED + "O módulo ambiental está desativado.");
                    return true;
                }
                if (sender instanceof Player player) {
                    long impact = machineManager == null ? 0L : machineManager.industrialImpact(player.getWorld());
                    sender.sendMessage(ChatColor.GOLD + "Impacto industrial em " + player.getWorld().getName());
                    sender.sendMessage(ChatColor.GRAY + "Unidades acumuladas: " + ChatColor.YELLOW + impact);
                    sender.sendMessage(ChatColor.GRAY + "Pesquise Controle de emissões para evitar novas emissões de máquinas.");
                } else {
                    for (org.bukkit.World world : Bukkit.getWorlds()) {
                        long impact = machineManager == null ? 0L : machineManager.industrialImpact(world);
                        sender.sendMessage(ChatColor.GOLD + world.getName() + ChatColor.GRAY + ": " + impact + " unidades");
                    }
                }
                return true;
            }
            case "status" -> {
                sender.sendMessage(ChatColor.GOLD + "MECHANIUM " + getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Eras: " + api.getEras().size()
                        + " | Tecnologias: " + api.getTechnologies().size()
                        + " | Módulos: " + moduleManager.enabledModuleIds().size()
                        + " | Addons: " + addonLoader.loadedAddonIds().size());
                return true;
            }
            case "emc" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                if (emcExchangeManager == null) {
                    player.sendMessage(ChatColor.RED + "O módulo de equivalência EMC está desativado.");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("balance")) {
                    player.sendMessage(ChatColor.GOLD + "Seu saldo EMC: "
                            + ChatColor.AQUA + emcExchangeManager.balance(player.getUniqueId()) + " EMC");
                } else {
                    emcExchangeManager.openCommand(player);
                }
                return true;
            }
            case "menu", "gui", "hud" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                researchMenu.open(player);
                return true;
            }
            case "era" -> {
                String[] adminEraArgs = new String[args.length + 1];
                adminEraArgs[0] = "admin";
                adminEraArgs[1] = "era";
                System.arraycopy(args, 1, adminEraArgs, 2, args.length - 1);
                return handleAdmin(sender, adminEraArgs);
            }
            case "research" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("info")) {
                    String eraId = api.getPlayerEra(player.getUniqueId()).orElse("não definida");
                    String eraName = api.getEra(eraId).map(EraDefinition::displayName).orElse(eraId);
                    player.sendMessage(ChatColor.GOLD + "Progressão MECHANIUM");
                    player.sendMessage(ChatColor.GRAY + "Era: " + ChatColor.WHITE + eraName);
                    player.sendMessage(ChatColor.GRAY + "Pontos científicos: " + ChatColor.WHITE
                            + api.getResearchPoints(player.getUniqueId()));
                } else {
                    researchMenu.open(player);
                }
                return true;
            }
            case "book" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                researchMenu.open(player);
                return true;
            }
            case "storage" -> {
                return handleStorage(sender, args);
            }
            case "unlock" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium unlock <tecnologia>");
                    return true;
                }
                Optional<TechnologyDefinition> technology = api.getTechnology(args[1]);
                if (technology.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Tecnologia desconhecida.");
                    return true;
                }
                startResearch(player, technology.orElseThrow());
                return true;
            }
            case "calc" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                if (args.length < 2 || args.length > 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium calc <id-da-receita> [quantidade-final]");
                    return true;
                }
                long requested = 1;
                if (args.length == 3) {
                    try {
                        requested = Long.parseLong(args[2]);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage(ChatColor.RED + "Quantidade inválida.");
                        return true;
                    }
                }
                try {
                    RecipeCostCalculator.Calculation calculation =
                            new RecipeCostCalculator(api).calculate(args[1], requested);
                    var definition = calculation.recipe();
                    String resultName = api.getItem(definition.resultItemId())
                            .map(dev.mechanium.api.model.ItemDefinition::displayName)
                            .orElse(definition.resultItemId());
                    sender.sendMessage(ChatColor.GOLD + "Cálculo de produção: " + resultName);
                    sender.sendMessage(ChatColor.GRAY + "Solicitado: " + requested
                            + " | Produzido: " + calculation.produced()
                            + " | Lotes: " + calculation.batches());
                    sender.sendMessage(ChatColor.GRAY + "Disponibilidade da receita principal: "
                            + (api.canCraft(player.getUniqueId(), definition.id())
                            ? ChatColor.GREEN + "liberada" : ChatColor.RED + "bloqueada pela progressão"));
                    sender.sendMessage(ChatColor.YELLOW + "Materiais totais necessários:");
                    calculation.materials().forEach((ingredient, amount) -> {
                        String display = ingredient;
                        if (ingredient.startsWith("mechanium:")) {
                            display = api.getItem(ingredient.substring(10))
                                    .map(dev.mechanium.api.model.ItemDefinition::displayName).orElse(ingredient);
                        } else {
                            org.bukkit.Material material = org.bukkit.Material.matchMaterial(ingredient);
                            if (material != null) display = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                        }
                        sender.sendMessage(ChatColor.GRAY + " • " + display + " x" + amount);
                    });
                    sender.sendMessage(ChatColor.DARK_GRAY
                            + "Custos representam materiais; não há integração com moedas/economia.");
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                }
                return true;
            }
            case "admin" -> {
                return handleAdmin(sender, args);
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private boolean handleStorage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
            return true;
        }
        if (privateStorageListener == null || args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /mechanium storage <trust|untrust|list> [jogador]");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(6);
        if (targetBlock == null || !privateStorageListener.canManageStorage(player, targetBlock)) {
            player.sendMessage(ChatColor.RED + "Olhe para um dos seus armazéns privados para gerenciar o acesso.");
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            List<UUID> trusted = privateStorageListener.trustedPlayers(player, targetBlock);
            if (trusted.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Nenhum jogador autorizado neste armazém.");
            } else {
                player.sendMessage(ChatColor.GOLD + "Jogadores autorizados:");
                for (UUID id : trusted) {
                    String name = Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(id.toString());
                    player.sendMessage(ChatColor.GRAY + "• " + name);
                }
            }
            return true;
        }

        if (!action.equals("trust") && !action.equals("untrust")) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /mechanium storage <trust|untrust|list> [jogador]");
            return true;
        }
        if (args.length != 3) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /mechanium storage " + action + " <jogador>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Você já é o proprietário deste armazém.");
            return true;
        }
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            player.sendMessage(ChatColor.RED + "Esse jogador ainda não entrou neste servidor.");
            return true;
        }

        boolean trust = action.equals("trust");
        boolean changed = privateStorageListener.changeTrustedPlayer(
                player, targetBlock, target.getUniqueId(), trust);
        if (changed) {
            String targetName = Optional.ofNullable(target.getName()).orElse(args[2]);
            player.sendMessage(ChatColor.GREEN + (trust ? "Acesso concedido a " : "Acesso removido de ")
                    + targetName + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Não foi possível alterar o acesso. Ele pode já estar nesse estado ou o limite de 32 jogadores foi atingido.");
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }
        if (args.length == 1 || (args.length == 2
                && (args[1].equalsIgnoreCase("hud") || args[1].equalsIgnoreCase("gui")))) {
            if (sender instanceof Player player && adminMenu != null) adminMenu.open(player);
            else sendHelp(sender);
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("recipes")) {
            if (sender instanceof Player player && adminMenu != null) adminMenu.openRecipes(player);
            else sender.sendMessage(ChatColor.RED + "O HUD de receitas exige um jogador.");
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("custom-recipes")) {
            if (sender instanceof Player player) openRecipeAdminMenu(player);
            else sender.sendMessage(ChatColor.RED + "O editor de receitas exige um jogador.");
            return true;
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("customizer")) {
            if (sender instanceof Player player && slimeCustomizerMenu != null) slimeCustomizerMenu.open(player);
            else sender.sendMessage(ChatColor.RED + "O Slime Customizer exige um jogador e o módulo ativo.");
            return true;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("recipe")) {
            String key = args[2].toLowerCase(Locale.ROOT);
            String eraId = args[3].toLowerCase(Locale.ROOT);
            if (!key.matches("minecraft:[a-z0-9_./-]+")) {
                sender.sendMessage(ChatColor.RED + "ID inválido. Use minecraft:<id-da-receita>.");
                return true;
            }
            String configPath = "vanilla-recipe-gates.recipes." + key;
            if (eraId.equals("off") || eraId.equals("none")) {
                getConfig().set(configPath, "off");
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Bloqueio removido da receita " + key + ".");
            } else if (api.getEra(eraId).isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Era inexistente: " + eraId);
            } else {
                getConfig().set(configPath, eraId);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Receita " + key + " agora exige a era "
                        + api.getEra(eraId).orElseThrow().displayName() + ".");
            }
            return true;
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("staff") && args[2].equalsIgnoreCase("list")) {
            List<String> admins = getConfig().getStringList("staff.admins");
            sender.sendMessage(ChatColor.GOLD + "Equipe administrativa configurada:");
            if (admins.isEmpty()) sender.sendMessage(ChatColor.GRAY + "Nenhum membro adicional. Operadores continuam com acesso.");
            else admins.forEach(id -> sender.sendMessage(ChatColor.GRAY + "• " + id));
            return true;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("staff")
                && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
            List<String> admins = new ArrayList<>(getConfig().getStringList("staff.admins"));
            String id = target.getUniqueId().toString();
            if (args[2].equalsIgnoreCase("add")) {
                if (!admins.contains(id)) admins.add(id);
                getConfig().set("staff.admins", admins);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Acesso administrativo concedido a " + args[3] + ".");
            } else {
                admins.remove(id);
                getConfig().set("staff.admins", admins);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Acesso administrativo removido de " + args[3] + ".");
            }
            return true;
        }
        if (args.length >= 3 && args.length <= 4 && args[1].equalsIgnoreCase("era")) {
            String eraId = args[2].toLowerCase(Locale.ROOT);
            if (api.getEra(eraId).isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Era inexistente: " + eraId);
                return true;
            }
            if (args.length == 3) {
                changeServerEra(eraId);
                String displayName = api.getEra(eraId).orElseThrow().displayName();
                sender.sendMessage(ChatColor.GREEN + "Era GLOBAL do servidor alterada para "
                        + displayName + ". Todos os jogadores herdam essa era; os avisos pendentes serão exibidos no próximo login.");
            } else if (args[3].equalsIgnoreCase("todos") || args[3].equalsIgnoreCase("all")) {
                for (Player online : Bukkit.getOnlinePlayers()) api.setPlayerEra(online.getUniqueId(), eraId);
                sender.sendMessage(ChatColor.GREEN + "Era de todos os jogadores online alterada para " + eraId + ".");
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
                api.setPlayerEra(target.getUniqueId(), eraId);
                sender.sendMessage(ChatColor.GREEN + "Era de " + args[3] + " alterada para " + eraId + ".");
            }
            return true;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("points")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            try {
                long amount = Long.parseLong(args[3]);
                api.addResearchPoints(target.getUniqueId(), amount);
                sender.sendMessage(ChatColor.GREEN + "Adicionados " + amount + " pontos para " + target.getName() + ".");
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(ChatColor.RED + "Quantidade inválida: " + exception.getMessage());
            }
            return true;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("setera")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            try {
                api.setPlayerEra(target.getUniqueId(), args[3]);
                sender.sendMessage(ChatColor.GREEN + "Era de " + target.getName() + " atualizada para " + args[3] + ".");
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(ChatColor.RED + exception.getMessage());
            }
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin hud — abrir o painel administrativo");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin recipes — configurar receitas vanilla");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin custom-recipes — editar receitas MECHANIUM");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin customizer — criar e remover itens personalizados");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin recipe <minecraft:id> <era|off>");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin era <era> [jogador|todos] — sem alvo altera a era global");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin staff <add|remove|list> [jogador]");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin points <jogador> <quantidade>");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin setera <jogador> <era>");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "MECHANIUM — comandos");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium status" + ChatColor.GRAY + " — estado do motor");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium" + ChatColor.GRAY + " — abrir o HUD em baú");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium menu" + ChatColor.GRAY + " — abrir o HUD em baú");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research" + ChatColor.GRAY + " — abrir pesquisas e progressão");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research info" + ChatColor.GRAY + " — resumo da sua progressão");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium book" + ChatColor.GRAY + " — abrir o catálogo");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage trust <jogador>" + ChatColor.GRAY + " — autorizar acesso ao barril observado");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage untrust <jogador>" + ChatColor.GRAY + " — remover acesso");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage list" + ChatColor.GRAY + " — listar acessos");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium unlock <id>" + ChatColor.GRAY + " — pesquisar tecnologia");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium calc <receita> [quantidade]" + ChatColor.GRAY + " — calcular materiais e lotes");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium environment" + ChatColor.GRAY + " — consultar impacto industrial do mundo");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium emc" + ChatColor.GRAY + " — abrir seu ledger de equivalência");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium emc balance" + ChatColor.GRAY + " — consultar saldo EMC");
        if (hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.YELLOW + "/mechanium era <era> [jogador|todos]" + ChatColor.GRAY + " — alterar era (staff)");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin hud" + ChatColor.GRAY + " — painel administrativo");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin recipes" + ChatColor.GRAY + " — configurar bloqueios de receitas vanilla");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin custom-recipes" + ChatColor.GRAY + " — editar receitas MECHANIUM");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin customizer" + ChatColor.GRAY + " — criar e remover itens personalizados");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin recipe <minecraft:id> <era|off>");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin era <era> [jogador|todos] — sem alvo altera a era global");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin staff <add|remove|list> [jogador]");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin points <jogador> <quantidade>");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin setera <jogador> <era>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "status", "menu", "gui", "hud", "research", "book", "storage", "unlock", "calc", "environment", "emc", "era", "admin");
        if (args.length == 2 && args[0].equalsIgnoreCase("emc")) return List.of("balance");
        if (args.length == 2 && args[0].equalsIgnoreCase("era") && hasAdminAccess(sender)) {
            return api.getEras().stream().map(EraDefinition::id).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("era") && hasAdminAccess(sender)) {
            List<String> suggestions = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            suggestions.add("todos");
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("storage")) return List.of("trust", "untrust", "list");
        if (args.length == 3 && args[0].equalsIgnoreCase("storage")
                && (args[1].equalsIgnoreCase("trust") || args[1].equalsIgnoreCase("untrust"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("research")) return List.of("info");
        if (args.length == 2 && args[0].equalsIgnoreCase("calc")) {
            return api.getRecipes().stream().map(recipe -> recipe.id())
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("unlock")) {
            return api.getTechnologies().stream().map(technology -> technology.id())
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && hasAdminAccess(sender)) {
            return List.of("hud", "gui", "recipes", "custom-recipes", "customizer", "recipe", "era", "points", "setera", "staff");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("recipe")) {
            return List.of("minecraft:iron_pickaxe", "minecraft:piston", "minecraft:hopper", "minecraft:repeater");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("recipe")) {
            List<String> values = new ArrayList<>(api.getEras().stream().map(EraDefinition::id).toList());
            values.add("off");
            return values;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("era")) {
            return api.getEras().stream().map(EraDefinition::id).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("staff")) {
            return List.of("add", "remove", "list");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("era")) {
            List<String> suggestions = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            suggestions.add("todos");
            return suggestions;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("staff")
                && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setera")) {
            return api.getEras().stream().map(EraDefinition::id).toList();
        }
        return List.of();
    }

    boolean hasAdminAccess(CommandSender sender) {
        if (sender.hasPermission("mechanium.admin")) return true;
        if (!(sender instanceof Player player)) return false;
        List<String> configured = getConfig().getStringList("staff.admins");
        return configured.contains(player.getUniqueId().toString())
                || configured.stream().anyMatch(value -> value.equalsIgnoreCase(player.getName()));
    }

    @Override
    public void onDisable() {
        shutdownServices();
    }

    private void shutdownServices() {
        if (researchCompletionTask != null) {
            researchCompletionTask.cancel();
            researchCompletionTask = null;
        }
        if (recipeBridge != null) {
            try {
                recipeBridge.unregisterAll();
            } catch (RuntimeException exception) {
                getLogger().warning("Falha ao remover receitas MECHANIUM: " + exception.getMessage());
            }
            recipeBridge = null;
        }
        if (simpleStorageNetworkListener != null) {
            simpleStorageNetworkListener.closeAll();
            simpleStorageNetworkListener = null;
        }
        if (greenhouseManager != null) {
            greenhouseManager.stop();
            greenhouseManager = null;
        }
        if (fluidPipeManager != null) {
            fluidPipeManager.stop();
            fluidPipeManager = null;
        }
        if (materialGeneratorManager != null) {
            materialGeneratorManager.stop();
            materialGeneratorManager = null;
        }
        if (industrialNetworkManager != null) {
            industrialNetworkManager.stop();
            industrialNetworkManager = null;
        }
        if (energyGridManager != null) {
            energyGridManager.stop();
            energyGridManager = null;
        }
        specialEquipmentListener = null;
        researchMenu = null;
        adminMenu = null;
        recipeAdminMenu = null;
        slimeCustomizerMenu = null;
        privateStorageListener = null;
        if (townyNationHook != null) {
            Bukkit.getServicesManager().unregister(NationIdentityProvider.class, townyNationHook);
            townyNationHook = null;
        }
        if (machineManager != null) {
            machineManager.stop();
            machineManager = null;
        }
        if (emcExchangeManager != null) {
            emcExchangeManager.close();
            emcExchangeManager = null;
        }
        if (addonLoader != null) {
            addonLoader.close();
            addonLoader = null;
        }
        if (moduleManager != null) {
            moduleManager.disable();
            moduleManager = null;
        }
        if (api != null) {
            Bukkit.getServicesManager().unregister(MechaniumApi.class, api);
            api = null;
        }
        if (dataStore != null) {
            dataStore.close();
            dataStore = null;
        }
    }
}
