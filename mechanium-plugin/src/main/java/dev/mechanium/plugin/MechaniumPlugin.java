package dev.mechanium.plugin;

import dev.mechanium.addons.MechaniumAddonLoader;
import dev.mechanium.api.MechaniumAddonContext;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumDataStore;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.engine.DefaultMechaniumApi;
import dev.mechanium.modules.BuiltinModules;
import dev.mechanium.modules.ModuleManager;
import dev.mechanium.storage.SqliteDataStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.*;

public final class MechaniumPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private MechaniumDataStore dataStore;
    private MechaniumApi api;
    private ModuleManager moduleManager;
    private MechaniumAddonLoader addonLoader;
    private MechaniumRecipeBridge recipeBridge;
    private MechaniumResearchMenu researchMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            Path database = getDataFolder().toPath()
                    .resolve(getConfig().getString("storage.database", "mechanium.db"));
            dataStore = new SqliteDataStore(database, getLogger());
            api = new DefaultMechaniumApi(dataStore);
            registerConfiguredEras();
            Bukkit.getServicesManager().register(MechaniumApi.class, api, this, ServicePriority.Normal);

            moduleManager = new ModuleManager();
            moduleManager.enable(BuiltinModules.create(), getConfig(),
                    new MechaniumModuleContext(api, this, getDataFolder().toPath().resolve("modules"), getLogger()));

            addonLoader = new MechaniumAddonLoader(api, getClass().getClassLoader(),
                    getDataFolder().toPath().resolve(getConfig().getString("addons.directory", "addons")), getLogger());
            addonLoader.loadAll(new MechaniumAddonContext(api, this,
                    getDataFolder().toPath().resolve("addons"), getLogger()));
            List<String> contentErrors = api.validateContent();
            if (!contentErrors.isEmpty()) throw new IllegalStateException("Conteúdo MECHANIUM inválido: " + String.join("; ", contentErrors));
            recipeBridge = new MechaniumRecipeBridge(this, api);
            recipeBridge.registerAll();
            Bukkit.getPluginManager().registerEvents(recipeBridge, this);

            Bukkit.getPluginManager().registerEvents(this, this);
            Bukkit.getPluginManager().registerEvents(new MechaniumContentListener(api), this);
            Bukkit.getPluginManager().registerEvents(new PrivateStorageListener(this, api), this);
            researchMenu = new MechaniumResearchMenu(this, api);
            Bukkit.getPluginManager().registerEvents(researchMenu, this);
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
        if (api.getPlayerEra(playerId).isEmpty()) {
            String defaultEra = getConfig().getString("progression.default-era", "ancient");
            try {
                api.setPlayerEra(playerId, defaultEra);
            } catch (IllegalArgumentException exception) {
                getLogger().severe("Era inicial inválida em config.yml: " + defaultEra);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                sender.sendMessage(ChatColor.GOLD + "MECHANIUM " + getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Eras: " + api.getEras().size()
                        + " | Tecnologias: " + api.getTechnologies().size()
                        + " | Módulos: " + moduleManager.enabledModuleIds().size()
                        + " | Addons: " + addonLoader.loadedAddonIds().size());
                return true;
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
            case "unlock" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Este comando exige um jogador.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium unlock <tecnologia>");
                    return true;
                }
                var result = api.unlockTechnology(player.getUniqueId(), args[1]);
                player.sendMessage((result.successful() ? ChatColor.GREEN : ChatColor.RED) + result.message());
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

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mechanium.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
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
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin points <jogador> <quantidade>");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin setera <jogador> <era>");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "MECHANIUM — comandos");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium status" + ChatColor.GRAY + " — estado do motor");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research" + ChatColor.GRAY + " — abrir o catálogo de pesquisas");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research info" + ChatColor.GRAY + " — resumo da sua progressão");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium book" + ChatColor.GRAY + " — abrir o catálogo");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium unlock <id>" + ChatColor.GRAY + " — pesquisar tecnologia");
        if (sender.hasPermission("mechanium.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin points <jogador> <quantidade>");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin setera <jogador> <era>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "status", "research", "book", "unlock", "admin");
        if (args.length == 2 && args[0].equalsIgnoreCase("research")) return List.of("info");
        if (args.length == 2 && args[0].equalsIgnoreCase("unlock")) {
            return api.getTechnologies().stream().map(technology -> technology.id())
                    .filter(id -> id.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("mechanium.admin")) {
            return List.of("points", "setera");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setera")) {
            return api.getEras().stream().map(EraDefinition::id).toList();
        }
        return List.of();
    }

    @Override
    public void onDisable() {
        shutdownServices();
    }

    private void shutdownServices() {
        if (recipeBridge != null) {
            try {
                recipeBridge.unregisterAll();
            } catch (RuntimeException exception) {
                getLogger().warning("Falha ao remover receitas MECHANIUM: " + exception.getMessage());
            }
            recipeBridge = null;
        }
        researchMenu = null;
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
