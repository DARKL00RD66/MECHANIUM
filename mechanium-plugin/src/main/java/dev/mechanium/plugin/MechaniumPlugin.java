package dev.mechanium.plugin;

import dev.mechanium.addons.MechaniumAddonLoader;
import dev.mechanium.api.MechaniumAddonContext;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumDataStore;
import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.integration.NationIdentityProvider;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.engine.DefaultMechaniumApi;
import dev.mechanium.modules.BuiltinModules;
import dev.mechanium.modules.ModuleManager;
import dev.mechanium.storage.SqliteDataStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.block.Block;
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
    private PrivateStorageListener privateStorageListener;
    private MechaniumMachineManager machineManager;
    private EnergyGridManager energyGridManager;
    private FluidPipeManager fluidPipeManager;
    private SpecialEquipmentListener specialEquipmentListener;
    private SimpleStorageNetworkListener simpleStorageNetworkListener;
    private TownyNationHook townyNationHook;

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
            if (api.getRegisteredModules().contains("geology")) {
                Bukkit.getPluginManager().registerEvents(new GeologicalSurveyListener(this, api), this);
            }

            addonLoader = new MechaniumAddonLoader(api, getClass().getClassLoader(),
                    getDataFolder().toPath().resolve(getConfig().getString("addons.directory", "addons")), getLogger());
            addonLoader.loadAll(new MechaniumAddonContext(api, this,
                    getDataFolder().toPath().resolve("addons"), getLogger()));
            Set<String> activeContentOwners = new HashSet<>();
            moduleManager.enabledModuleIds().forEach(id -> activeContentOwners.add("module:" + id));
            addonLoader.loadedAddonIds().forEach(id -> activeContentOwners.add("addon:" + id));
            api.pruneInactiveContentOwners(activeContentOwners);
            List<String> contentErrors = api.validateContent();
            if (!contentErrors.isEmpty()) throw new IllegalStateException("Conteúdo MECHANIUM inválido: " + String.join("; ", contentErrors));
            recipeBridge = new MechaniumRecipeBridge(this, api);
            recipeBridge.registerAll();
            Bukkit.getPluginManager().registerEvents(recipeBridge, this);

            Bukkit.getPluginManager().registerEvents(this, this);
            Bukkit.getPluginManager().registerEvents(new MechaniumContentListener(api), this);
            Bukkit.getPluginManager().registerEvents(new MechaniumMobDropListener(api), this);
            energyGridManager = new EnergyGridManager(this, api);
            Bukkit.getPluginManager().registerEvents(energyGridManager, this);
            energyGridManager.start();
            fluidPipeManager = new FluidPipeManager(this, api);
            Bukkit.getPluginManager().registerEvents(fluidPipeManager, this);
            fluidPipeManager.start();
            specialEquipmentListener = new SpecialEquipmentListener(this);
            Bukkit.getPluginManager().registerEvents(specialEquipmentListener, this);
            machineManager = new MechaniumMachineManager(this, api, energyGridManager);
            Bukkit.getPluginManager().registerEvents(machineManager, this);
            machineManager.start();
            privateStorageListener = new PrivateStorageListener(this, api);
            Bukkit.getPluginManager().registerEvents(privateStorageListener, this);
            simpleStorageNetworkListener = new SimpleStorageNetworkListener(this);
            Bukkit.getPluginManager().registerEvents(simpleStorageNetworkListener, this);
            researchMenu = new MechaniumResearchMenu(this, api);
            Bukkit.getPluginManager().registerEvents(researchMenu, this);
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
        if (researchMenu != null) researchMenu.giveGuide(event.getPlayer());
        if (townyNationHook != null) townyNationHook.sync(event.getPlayer());
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
                var result = api.unlockTechnology(player.getUniqueId(), args[1]);
                player.sendMessage((result.successful() ? ChatColor.GREEN : ChatColor.RED) + result.message());
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
                getConfig().set("progression.default-era", eraId);
                saveConfig();
                for (Player online : Bukkit.getOnlinePlayers()) api.setPlayerEra(online.getUniqueId(), eraId);
                sender.sendMessage(ChatColor.GREEN + "Era padrão do servidor alterada para " + eraId
                        + ". Jogadores online atualizados; novos jogadores também começarão nela.");
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
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin era <era> [jogador|todos]");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin staff <add|remove|list> [jogador]");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin points <jogador> <quantidade>");
        sender.sendMessage(ChatColor.YELLOW + "Uso: /mechanium admin setera <jogador> <era>");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "MECHANIUM — comandos");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium status" + ChatColor.GRAY + " — estado do motor");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research" + ChatColor.GRAY + " — abrir o menu de baús");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium research info" + ChatColor.GRAY + " — resumo da sua progressão");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium book" + ChatColor.GRAY + " — abrir o catálogo");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage trust <jogador>" + ChatColor.GRAY + " — autorizar acesso ao barril observado");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage untrust <jogador>" + ChatColor.GRAY + " — remover acesso");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium storage list" + ChatColor.GRAY + " — listar acessos");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium unlock <id>" + ChatColor.GRAY + " — pesquisar tecnologia");
        sender.sendMessage(ChatColor.YELLOW + "/mechanium calc <receita> [quantidade]" + ChatColor.GRAY + " — calcular materiais e lotes");
        if (hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin era <era> [jogador|todos]");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin staff <add|remove|list> [jogador]");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin points <jogador> <quantidade>");
            sender.sendMessage(ChatColor.YELLOW + "/mechanium admin setera <jogador> <era>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "status", "research", "book", "storage", "unlock", "calc", "admin");
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
            return List.of("era", "points", "setera", "staff");
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

    private boolean hasAdminAccess(CommandSender sender) {
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
        if (fluidPipeManager != null) {
            fluidPipeManager.stop();
            fluidPipeManager = null;
        }
        if (energyGridManager != null) {
            energyGridManager.stop();
            energyGridManager = null;
        }
        specialEquipmentListener = null;
        researchMenu = null;
        privateStorageListener = null;
        if (townyNationHook != null) {
            Bukkit.getServicesManager().unregister(NationIdentityProvider.class, townyNationHook);
            townyNationHook = null;
        }
        if (machineManager != null) {
            machineManager.stop();
            machineManager = null;
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
