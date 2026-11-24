package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ItemDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Independent equivalent-exchange runtime for MECHANIUM.
 * Each player's EMC balance and learned item catalog are persisted in a dedicated SQLite file.
 */
final class EmcExchangeManager implements Listener, AutoCloseable {
    private static final int SIZE = 54;
    private static final int INPUT_SLOT = 13;
    private static final int[] LIST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };
    private static final NamespacedKey ITEM_ID = new NamespacedKey("mechanium", "item_id");

    private final MechaniumPlugin plugin;
    private final MechaniumApi api;
    private final Connection connection;
    private final NamespacedKey machineIdKey;
    private final NamespacedKey ownerKey;
    private final Map<String, Long> itemValues = new LinkedHashMap<>();

    EmcExchangeManager(MechaniumPlugin plugin, MechaniumApi api, Path databaseFile,
                       ConfigurationSection configuration) throws SQLException {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.api = Objects.requireNonNull(api, "api");
        this.machineIdKey = new NamespacedKey(plugin, "emc_machine_id");
        this.ownerKey = new NamespacedKey(plugin, "emc_machine_owner");
        loadValues(configuration);
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception exception) {
            throw new SQLException("Não foi possível criar o diretório do ledger EMC.", exception);
        }
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        configureDatabase();
        plugin.getLogger().info("Ledger EMC pronto: " + itemValues.size() + " valores configurados.");
    }

    private void loadValues(ConfigurationSection configuration) {
        itemValues.clear();
        ConfigurationSection values = configuration == null
                ? null : configuration.getConfigurationSection("values");
        if (values == null) return;
        for (String key : values.getKeys(false)) {
            long value = values.getLong(key, 0L);
            if (value > 0L) itemValues.put(key.toLowerCase(Locale.ROOT), value);
            else plugin.getLogger().warning("Valor EMC ignorado para '" + key + "': deve ser maior que zero.");
        }
    }

    private void configureDatabase() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS emc_player_balances (
                        player_uuid TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL DEFAULT 0 CHECK(balance >= 0)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS emc_player_learned_items (
                        player_uuid TEXT NOT NULL,
                        item_key TEXT NOT NULL,
                        unit_value INTEGER NOT NULL CHECK(unit_value > 0),
                        learned_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY(player_uuid, item_key)
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_emc_learned_player ON emc_player_learned_items(player_uuid, item_key)");
        }
    }

    long balance(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        synchronized (connection) {
            try {
                ensurePlayer(playerId);
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT balance FROM emc_player_balances WHERE player_uuid = ?")) {
                    query.setString(1, playerId.toString());
                    try (ResultSet rows = query.executeQuery()) {
                        return rows.next() ? rows.getLong(1) : 0L;
                    }
                }
            } catch (SQLException exception) {
                throw storageFailure("consultar saldo EMC", exception);
            }
        }
    }

    int learnedCount(UUID playerId) { return learnedItems(playerId).size(); }

    List<LearnedItem> learnedItems(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        synchronized (connection) {
            List<LearnedItem> entries = new ArrayList<>();
            try {
                ensurePlayer(playerId);
                try (PreparedStatement query = connection.prepareStatement("""
                        SELECT item_key, unit_value
                        FROM emc_player_learned_items
                        WHERE player_uuid = ?
                        ORDER BY item_key
                        """)) {
                    query.setString(1, playerId.toString());
                    try (ResultSet rows = query.executeQuery()) {
                        while (rows.next()) entries.add(new LearnedItem(rows.getString(1), rows.getLong(2)));
                    }
                }
                return List.copyOf(entries);
            } catch (SQLException exception) {
                throw storageFailure("consultar catálogo aprendido de EMC", exception);
            }
        }
    }

    boolean creditAndLearn(UUID playerId, String itemKey, long unitValue, long totalValue) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(itemKey, "itemKey");
        if (unitValue <= 0L || totalValue <= 0L) throw new IllegalArgumentException("Valores EMC devem ser positivos.");
        synchronized (connection) {
            try {
                ensurePlayer(playerId);
                connection.setAutoCommit(false);
                try {
                    try (PreparedStatement credit = connection.prepareStatement("""
                            UPDATE emc_player_balances
                            SET balance = balance + ?
                            WHERE player_uuid = ? AND balance <= ?
                            """)) {
                        credit.setLong(1, totalValue);
                        credit.setString(2, playerId.toString());
                        credit.setLong(3, Long.MAX_VALUE - totalValue);
                        if (credit.executeUpdate() != 1) {
                            connection.rollback();
                            return false;
                        }
                    }
                    try (PreparedStatement learn = connection.prepareStatement("""
                            INSERT OR IGNORE INTO emc_player_learned_items(player_uuid, item_key, unit_value)
                            VALUES (?, ?, ?)
                            """)) {
                        learn.setString(1, playerId.toString());
                        learn.setString(2, itemKey);
                        learn.setLong(3, unitValue);
                        learn.executeUpdate();
                    }
                    connection.commit();
                    return true;
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException exception) {
                throw storageFailure("converter itens em EMC", exception);
            }
        }
    }

    boolean debit(UUID playerId, long amount) {
        Objects.requireNonNull(playerId, "playerId");
        if (amount <= 0L) throw new IllegalArgumentException("O custo EMC deve ser positivo.");
        synchronized (connection) {
            try {
                ensurePlayer(playerId);
                try (PreparedStatement debit = connection.prepareStatement("""
                        UPDATE emc_player_balances
                        SET balance = balance - ?
                        WHERE player_uuid = ? AND balance >= ?
                        """)) {
                    debit.setLong(1, amount);
                    debit.setString(2, playerId.toString());
                    debit.setLong(3, amount);
                    return debit.executeUpdate() == 1;
                }
            } catch (SQLException exception) {
                throw storageFailure("debitar saldo EMC", exception);
            }
        }
    }

    private void ensurePlayer(UUID playerId) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO emc_player_balances(player_uuid, balance) VALUES (?, 0)")) {
            insert.setString(1, playerId.toString());
            insert.executeUpdate();
        }
    }

    private IllegalStateException storageFailure(String operation, SQLException exception) {
        plugin.getLogger().severe("Falha ao " + operation + ": " + exception.getMessage());
        return new IllegalStateException("Falha ao " + operation + ".", exception);
    }

    void openDashboard(Player player) {
        MenuHolder holder = create("MECHANIUM • Equivalência EMC", MenuKind.DASHBOARD, 0, 0);
        fillFrame(holder.inventory);
        long balance = balance(player.getUniqueId());
        List<LearnedItem> learned = learnedItems(player.getUniqueId());
        holder.inventory.setItem(4, item(Material.NETHER_STAR, "§6§lLEDGER DE EMC", List.of(
                "§7Saldo individual: §b" + balance + " EMC",
                "§7Itens aprendidos: §a" + learned.size(),
                "§8O saldo e o catálogo pertencem ao jogador.",
                "§8Dissolva itens para aprender e sintetizar depois."
        )));
        button(holder, 11, Material.BOOK, "§eItens aprendidos", List.of(
                "§7Itens que você já converteu.",
                "§7Clique para consultar valores e sínteses."
        ), p -> openLearned(p, 0));
        button(holder, 15, Material.HOPPER, "§bComo funciona", List.of(
                "§7Dissolva um item em uma estação compatível.",
                "§7A primeira dissolução registra o item no catálogo.",
                "§7Use um materializador para sintetizar uma unidade.",
                "§7O custo é o valor EMC individual do item."
        ), p -> openHelp(p));
        button(holder, 22, Material.CLOCK, "§6Limites das estações", List.of(
                "§7Nível I: até 16 EMC por item",
                "§7Nível II: até 64 EMC por item",
                "§7Nível III: até 256 EMC por item",
                "§7Nível IV: até 1.024 EMC por item",
                "§7Nível V: até 4.096 EMC por item"
        ), p -> openHelp(p));
        button(holder, 26, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private void openHelp(Player player) {
        MenuHolder holder = create("MECHANIUM • Regras EMC", MenuKind.HELP, 0, 0);
        fillFrame(holder.inventory);
        holder.inventory.setItem(13, item(Material.WRITABLE_BOOK, "§6Regras da equivalência", List.of(
                "§7O saldo EMC é individual e persistente.",
                "§7A dissolução consome o item inserido e credita",
                "§7seu valor multiplicado pela quantidade.",
                "§7A primeira dissolução aprende o item.",
                "§7A síntese só mostra itens aprendidos.",
                "§7Cada síntese consome o valor de uma unidade.",
                "§7Itens sem valor configurado não podem ser dissolvidos.",
                "§7Cada tier impõe um limite de valor por item.",
                "§8Os valores são configuráveis pelo administrador."
        )));
        button(holder, 26, Material.ARROW, "§eVoltar", List.of(), this::openDashboard);
        open(player, holder);
    }

    private void openLearned(Player player, int requestedPage) {
        List<LearnedItem> entries = learnedItems(player.getUniqueId());
        int pages = Math.max(1, (entries.size() + LIST_SLOTS.length - 1) / LIST_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        MenuHolder holder = create("MECHANIUM • Catálogo EMC", MenuKind.LEARNED, page, 0);
        fillFrame(holder.inventory);
        holder.inventory.setItem(4, item(Material.BOOK, "§eCATÁLOGO APRENDIDO", List.of(
                "§7Itens registrados: §f" + entries.size(),
                "§7Saldo: §b" + balance(player.getUniqueId()) + " EMC",
                "§7Página: §f" + (page + 1) + "/" + pages,
                "§8Consulte um item que pode ser sintetizado."
        )));
        int start = page * LIST_SLOTS.length;
        for (int i = 0; i < Math.min(LIST_SLOTS.length, entries.size() - start); i++) {
            LearnedItem entry = entries.get(start + i);
            int slot = LIST_SLOTS[i];
            ItemStack icon = createItem(entry.itemKey(), 1);
            if (icon == null) icon = new ItemStack(Material.BARRIER);
            holder.inventory.setItem(slot, named(icon, "§e" + itemName(entry.itemKey()), List.of(
                    "§7Valor unitário: §b" + entry.unitValue() + " EMC",
                    "§7Identificador: §8" + entry.itemKey(),
                    "§8Use um materializador para sintetizar."
            )));
        }
        if (page > 0) button(holder, 45, Material.ARROW, "§ePágina anterior", List.of(),
                p -> openLearned(p, page - 1));
        if (page + 1 < pages) button(holder, 53, Material.ARROW, "§ePróxima página", List.of(),
                p -> openLearned(p, page + 1));
        button(holder, 49, Material.ARROW, "§eVoltar ao ledger", List.of(), this::openDashboard);
        open(player, holder);
    }

    private void openDematerializer(Player player, String machineId, int tier) {
        MenuHolder holder = create("EMC • Dissolvedor • Nível " + tier, MenuKind.DEMATERIALIZER, 0, tier);
        fillFrame(holder.inventory);
        holder.machineId = machineId;
        holder.inventory.setItem(4, item(Material.HOPPER, "§6§lDISSOLUÇÃO DE MATÉRIA", List.of(
                "§7Seu saldo: §b" + balance(player.getUniqueId()) + " EMC",
                "§7Limite por item: §f" + EmcTierPolicy.maximumItemValue(tier) + " EMC",
                "§8Coloque um item no espaço central.",
                "§8A conversão consome toda a pilha inserida."
        )));
        holder.inventory.setItem(INPUT_SLOT, null);
        button(holder, 11, Material.NETHER_STAR, "§bSaldo EMC", List.of(
                "§7Saldo atual: §f" + balance(player.getUniqueId()) + " EMC"
        ), p -> openDashboard(p));
        button(holder, 15, Material.LIME_DYE, "§aDissolver pilha", List.of(
                "§7Converte a pilha no espaço central.",
                "§7O item será aprendido na primeira conversão."
        ), p -> processDematerialization(p, holder));
        button(holder, 22, Material.PAPER, "§eColoque o item no centro", List.of(
                "§7Itens sem valor EMC configurado são recusados."
        ), p -> p.sendMessage("§eInsira um item no espaço central e clique em Dissolver pilha."));
        button(holder, 26, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private void processDematerialization(Player player, MenuHolder holder) {
        ItemStack input = holder.inventory.getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            player.sendMessage("§cColoque um item no espaço central antes de dissolver.");
            return;
        }
        String itemKey = itemKey(input);
        long unitValue = itemValues.getOrDefault(itemKey, 0L);
        if (unitValue <= 0L) {
            player.sendMessage("§cEste item não possui valor EMC configurado.");
            return;
        }
        if (!EmcTierPolicy.canProcess(holder.tier, unitValue)) {
            player.sendMessage("§cEste item excede o limite do Dissolvedor nível " + holder.tier
                    + " (" + EmcTierPolicy.maximumItemValue(holder.tier) + " EMC por unidade).");
            return;
        }
        long total;
        try {
            total = EmcTierPolicy.totalValue(unitValue, input.getAmount());
        } catch (ArithmeticException exception) {
            player.sendMessage("§cO total EMC desta pilha excede o limite numérico suportado.");
            return;
        }
        if (!api.canUseItem(player.getUniqueId(), "emc_dissolver_t" + holder.tier)) {
            player.sendMessage("§cVocê ainda não desbloqueou esta estação.");
            return;
        }
        if (!ledgerCredit(player.getUniqueId(), itemKey, unitValue, total)) {
            player.sendMessage("§cO saldo EMC atingiria o limite numérico suportado.");
            return;
        }
        int amount = input.getAmount();
        holder.inventory.setItem(INPUT_SLOT, null);
        player.sendMessage("§aDissolução concluída: §f" + amount + "x " + itemName(itemKey)
                + " §arecebidos como §b+" + total + " EMC§a.");
        holder.inventory.setItem(4, item(Material.HOPPER, "§6§lDISSOLUÇÃO DE MATÉRIA", List.of(
                "§7Seu saldo: §b" + balance(player.getUniqueId()) + " EMC",
                "§7Última conversão: §a+" + total + " EMC",
                "§7Limite por item: §f" + EmcTierPolicy.maximumItemValue(holder.tier) + " EMC"
        )));
    }

    private boolean ledgerCredit(UUID playerId, String itemKey, long unitValue, long totalValue) {
        return creditAndLearn(playerId, itemKey, unitValue, totalValue);
    }

    private void openMaterializer(Player player, String machineId, int tier, int requestedPage) {
        List<LearnedItem> entries = learnedItems(player.getUniqueId()).stream()
                .filter(entry -> EmcTierPolicy.canProcess(tier, entry.unitValue()))
                .toList();
        int pages = Math.max(1, (entries.size() + LIST_SLOTS.length - 1) / LIST_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        MenuHolder holder = create("EMC • Materializador • Nível " + tier, MenuKind.MATERIALIZER, page, tier);
        holder.machineId = machineId;
        fillFrame(holder.inventory);
        holder.inventory.setItem(4, item(Material.CRAFTING_TABLE, "§b§lSÍNTESE DE MATÉRIA", List.of(
                "§7Seu saldo: §b" + balance(player.getUniqueId()) + " EMC",
                "§7Itens disponíveis: §f" + entries.size(),
                "§7Limite por item: §f" + EmcTierPolicy.maximumItemValue(tier) + " EMC",
                "§8Clique em um item para sintetizar uma unidade."
        )));
        int start = page * LIST_SLOTS.length;
        for (int i = 0; i < Math.min(LIST_SLOTS.length, entries.size() - start); i++) {
            LearnedItem entry = entries.get(start + i);
            int slot = LIST_SLOTS[i];
            ItemStack icon = createItem(entry.itemKey(), 1);
            if (icon == null) icon = new ItemStack(Material.BARRIER);
            holder.inventory.setItem(slot, named(icon, "§e" + itemName(entry.itemKey()), List.of(
                    "§7Custo por unidade: §b" + entry.unitValue() + " EMC",
                    "§7Saldo disponível: §f" + balance(player.getUniqueId()) + " EMC",
                    "§aClique para sintetizar 1 unidade."
            )));
            holder.actions.put(slot, p -> synthesize(p, holder, entry));
        }
        if (entries.isEmpty()) holder.inventory.setItem(22, item(Material.BARRIER,
                "§cNenhum item compatível aprendido", List.of(
                        "§7Dissolva itens primeiro para registrá-los.",
                        "§7Ou use uma estação de nível superior."
                )));
        if (page > 0) button(holder, 45, Material.ARROW, "§ePágina anterior", List.of(),
                p -> openMaterializer(p, machineId, tier, page - 1));
        if (page + 1 < pages) button(holder, 53, Material.ARROW, "§ePróxima página", List.of(),
                p -> openMaterializer(p, machineId, tier, page + 1));
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private void synthesize(Player player, MenuHolder holder, LearnedItem entry) {
        if (!EmcTierPolicy.canProcess(holder.tier, entry.unitValue())) {
            player.sendMessage("§cO item excede a capacidade desta estação.");
            return;
        }
        if (!api.canUseItem(player.getUniqueId(), "emc_materializer_t" + holder.tier)) {
            player.sendMessage("§cVocê ainda não desbloqueou a tecnologia deste Materializador.");
            return;
        }
        ItemStack result = createItem(entry.itemKey(), 1);
        if (result == null) {
            player.sendMessage("§cEste item não está disponível nesta instalação do MECHANIUM.");
            return;
        }
        Inventory simulation = Bukkit.createInventory(null, 36);
        simulation.setStorageContents(player.getInventory().getStorageContents());
        if (!simulation.addItem(result.clone()).isEmpty()) {
            player.sendMessage("§cLibere espaço no inventário antes de sintetizar.");
            return;
        }
        if (!debit(player.getUniqueId(), entry.unitValue())) {
            player.sendMessage("§cSaldo EMC insuficiente. Custo: " + entry.unitValue() + " EMC.");
            return;
        }
        player.getInventory().addItem(result);
        player.sendMessage("§aSíntese concluída: §f" + itemName(entry.itemKey())
                + " §7por §b" + entry.unitValue() + " EMC§a.");
        openMaterializer(player, holder.machineId, holder.tier, holder.page);
    }

    private void openMachine(Player player, String machineId) {
        int tier = tierOf(machineId);
        if (tier < 1) {
            player.sendMessage("§cIdentificador da estação EMC inválido.");
            return;
        }
        if (machineId.startsWith("emc_dissolver_")) openDematerializer(player, machineId, tier);
        else if (machineId.startsWith("emc_materializer_")) openMaterializer(player, machineId, tier, 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMachinePlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (!isEmcMachineId(id)) return;
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER || !(block.getState() instanceof Dispenser dispenser)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cAs estações EMC precisam ser colocadas como dispensadores.");
            return;
        }
        dispenser.getPersistentDataContainer().set(machineIdKey, PersistentDataType.STRING, id);
        dispenser.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        if (!dispenser.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar a estação EMC.");
            return;
        }
        event.getPlayer().sendMessage("§aEstação EMC instalada: §f" + itemName("mechanium:" + id)
                + "§a. Clique com o botão direito para abrir.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Dispenser dispenser)) return;
        String machineId = dispenser.getPersistentDataContainer().get(machineIdKey, PersistentDataType.STRING);
        if (!isEmcMachineId(machineId)) return;
        event.setCancelled(true);
        String ownerText = dispenser.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        Player player = event.getPlayer();
        if (!canManage(player, ownerText)) {
            player.sendMessage("§cEsta estação pertence a outro jogador.");
            return;
        }
        if (!api.canUseItem(player.getUniqueId(), machineId)) {
            player.sendMessage("§cVocê ainda não desbloqueou a tecnologia desta estação.");
            return;
        }
        openMachine(player, machineId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMachineBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof Dispenser dispenser)) return;
        String machineId = dispenser.getPersistentDataContainer().get(machineIdKey, PersistentDataType.STRING);
        if (!isEmcMachineId(machineId)) return;
        String ownerText = dispenser.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        Player player = event.getPlayer();
        if (!canManage(player, ownerText)) {
            event.setCancelled(true);
            player.sendMessage("§cSomente o proprietário ou um administrador pode desmontar esta estação.");
            return;
        }
        ItemStack recovered = api.createItemStack(machineId, 1).orElse(new ItemStack(Material.DISPENSER));
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        event.setDropItems(false);
        event.setExpToDrop(0);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) return;
            block.getWorld().dropItemNaturally(location, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isEmcMachine);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isEmcMachine);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEmcMachine)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEmcMachine)) event.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;
        int topSize = event.getView().getTopInventory().getSize();

        if (rawSlot < topSize) {
            if (holder.kind == MenuKind.DEMATERIALIZER && rawSlot == INPUT_SLOT && !event.isShiftClick()) {
                return;
            }
            event.setCancelled(true);
            Consumer<Player> action = holder.actions.get(rawSlot);
            if (action != null) action.accept(player);
        } else if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.kind != MenuKind.DEMATERIALIZER) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlots().stream().anyMatch(raw -> raw < SIZE && raw != INPUT_SLOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        if (holder.kind != MenuKind.DEMATERIALIZER) return;
        ItemStack input = holder.inventory.getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) return;
        holder.inventory.setItem(INPUT_SLOT, null);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(input.clone());
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    void openCommand(Player player) {
        openDashboard(player);
    }

    private void open(Player player, MenuHolder holder) {
        player.openInventory(holder.inventory);
    }

    private MenuHolder create(String title, MenuKind kind, int page, int tier) {
        MenuHolder holder = new MenuHolder();
        holder.kind = kind;
        holder.page = page;
        holder.tier = tier;
        holder.inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        return holder;
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, "§0 ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(MenuHolder holder, int slot, Material material, String name, List<String> lore,
                        Consumer<Player> action) {
        holder.inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        return named(new ItemStack(material), name, lore);
    }

    private ItemStack named(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createItem(String itemKey, int amount) {
        if (itemKey.regionMatches(true, 0, "mechanium:", 0, 10)) {
            return api.createItemStack(itemKey.substring(10), amount).orElse(null);
        }
        Material material = Material.matchMaterial(itemKey);
        if (material == null || !material.isItem()) return null;
        return new ItemStack(material, Math.min(amount, material.getMaxStackSize()));
    }

    private String itemKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return "";
        if (stack.hasItemMeta()) {
            String customId = stack.getItemMeta().getPersistentDataContainer()
                    .get(ITEM_ID, PersistentDataType.STRING);
            if (customId != null && !customId.isBlank()) return "mechanium:" + customId.toLowerCase(Locale.ROOT);
        }
        return stack.getType().getKey().toString().toLowerCase(Locale.ROOT);
    }

    private String itemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
    }

    private String itemName(String itemKey) {
        if (itemKey.regionMatches(true, 0, "mechanium:", 0, 10)) {
            return api.getItem(itemKey.substring(10)).map(ItemDefinition::displayName).orElse(itemKey.substring(10));
        }
        Material material = Material.matchMaterial(itemKey);
        if (material == null) return itemKey;
        String name = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder title = new StringBuilder();
        for (String part : name.split(" ")) {
            if (!title.isEmpty()) title.append(' ');
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.toString();
    }

    private boolean isEmcMachineId(String id) {
        return id != null && (id.matches("emc_dissolver_t[1-5]") || id.matches("emc_materializer_t[1-5]"));
    }

    private int tierOf(String id) {
        if (!isEmcMachineId(id)) return 0;
        return id.charAt(id.length() - 1) - '0';
    }

    private boolean isEmcMachine(Block block) {
        if (!(block.getState() instanceof Dispenser dispenser)) return false;
        return isEmcMachineId(dispenser.getPersistentDataContainer().get(machineIdKey, PersistentDataType.STRING));
    }

    private boolean canManage(Player player, String ownerText) {
        return plugin.hasAdminAccess(player)
                || (ownerText != null && ownerText.equals(player.getUniqueId().toString()));
    }

    @Override
    public void close() {
        synchronized (connection) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().warning("Falha ao fechar ledger EMC: " + exception.getMessage());
            }
        }
    }

    private enum MenuKind { DASHBOARD, HELP, LEARNED, DEMATERIALIZER, MATERIALIZER }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        private MenuKind kind;
        private int page;
        private int tier;
        private String machineId;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }

    private record LearnedItem(String itemKey, long unitValue) { }
}
