package dev.mechanium.storage;

import dev.mechanium.api.MechaniumDataStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * SQLite persistence using prepared statements, WAL and an atomic research debit.
 * A single connection is synchronized; larger deployments can replace this adapter.
 */
public final class SqliteDataStore implements MechaniumDataStore {
    private final Connection connection;
    private final Logger logger;

    public SqliteDataStore(Path databaseFile, Logger logger) throws SQLException {
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception exception) {
            throw new SQLException("Could not create MECHANIUM data directory", exception);
        }
        this.logger = logger;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
        configure();
        migrate();
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_players (
                    uuid TEXT PRIMARY KEY,
                    era_id TEXT,
                    research_points INTEGER NOT NULL DEFAULT 0 CHECK (research_points >= 0)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_unlocked_technologies (
                    player_uuid TEXT NOT NULL,
                    technology_id TEXT NOT NULL,
                    unlocked_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, technology_id),
                    FOREIGN KEY (player_uuid) REFERENCES mechanium_players(uuid) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS mechanium_schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            statement.execute("INSERT OR IGNORE INTO mechanium_schema_version(version) VALUES (1)");
        }
        logger.info("MECHANIUM SQLite schema v1 pronta.");
    }

    private void ensurePlayer(UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO mechanium_players(uuid) VALUES (?)")) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public synchronized Optional<String> getPlayerEra(UUID playerId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT era_id FROM mechanium_players WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw failure("read player era", exception);
        }
    }

    @Override
    public synchronized void setPlayerEra(UUID playerId, String eraId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET era_id = ? WHERE uuid = ?")) {
                statement.setString(1, eraId);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("set player era", exception);
        }
    }

    @Override
    public synchronized long getResearchPoints(UUID playerId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT research_points FROM mechanium_players WHERE uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getLong(1) : 0L;
                }
            }
        } catch (SQLException exception) {
            throw failure("read research points", exception);
        }
    }

    @Override
    public synchronized void addResearchPoints(UUID playerId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Research point grants must be non-negative");
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE mechanium_players SET research_points = research_points + ? WHERE uuid = ?")) {
                statement.setLong(1, amount);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw failure("add research points", exception);
        }
    }

    @Override
    public synchronized boolean hasUnlocked(UUID playerId, String technologyId) {
        try {
            ensurePlayer(playerId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM mechanium_unlocked_technologies WHERE player_uuid = ? AND technology_id = ?")) {
                statement.setString(1, playerId.toString());
                statement.setString(2, technologyId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        } catch (SQLException exception) {
            throw failure("read unlocked technology", exception);
        }
    }

    @Override
    public synchronized boolean spendAndUnlock(UUID playerId, String technologyId, long cost) {
        if (cost < 0) throw new IllegalArgumentException("Technology cost must be non-negative");
        try {
            ensurePlayer(playerId);
            connection.setAutoCommit(false);
            try {
                if (hasUnlocked(playerId, technologyId)) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement debit = connection.prepareStatement("""
                    UPDATE mechanium_players
                    SET research_points = research_points - ?
                    WHERE uuid = ? AND research_points >= ?
                    """)) {
                    debit.setLong(1, cost);
                    debit.setString(2, playerId.toString());
                    debit.setLong(3, cost);
                    if (debit.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO mechanium_unlocked_technologies(player_uuid, technology_id)
                    VALUES (?, ?)
                    """)) {
                    insert.setString(1, playerId.toString());
                    insert.setString(2, technologyId);
                    insert.executeUpdate();
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
            throw failure("spend research points and unlock technology", exception);
        }
    }

    private IllegalStateException failure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + " in MECHANIUM database", exception);
    }

    @Override
    public synchronized void close() {
        try {
            if (!connection.isClosed()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                }
                connection.close();
            }
        } catch (SQLException exception) {
            logger.warning("Could not close MECHANIUM SQLite cleanly: " + exception.getMessage());
        }
    }
}
