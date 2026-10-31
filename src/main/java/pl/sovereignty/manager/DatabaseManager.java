package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.DiplomaticRelationType;
import pl.sovereignty.model.NationClaimChunk;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseManager {
    private final Plugin plugin;
    private String jdbcUrl;
    private String user;
    private String password;
    private Connection connection;
    private boolean enabled = false;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            boolean useMysql = isMySql();

            if (useMysql) {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String db = plugin.getConfig().getString("database.mysql.database", "sovereignty");
                this.user = plugin.getConfig().getString("database.mysql.username", "root");
                this.password = plugin.getConfig().getString("database.mysql.password", "");

                this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&characterEncoding=utf8&autoReconnect=true";
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException ignored) {}
            } else {
                File dbFile = new File(plugin.getDataFolder(), "sovereignty.db");
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException ignored) {}
            }

            this.enabled = true;
            createTables();
            plugin.getLogger().info("Pomyślnie zainicjalizowano bazę danych SQL (" + (useMysql ? "MySQL" : "SQLite") + ")!");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Nie udało się połączyć z bazą danych SQL (działanie w trybie awaryjnym YAML): " + t.getMessage());
            this.enabled = false;
        }
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sovereignty_walls (" +
                    "id INTEGER PRIMARY KEY " + (isMySql() ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                    "nation_id VARCHAR(36) NOT NULL, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x INT NOT NULL, " +
                    "y INT NOT NULL, " +
                    "z INT NOT NULL, " +
                    "UNIQUE(world, x, y, z));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sovereignty_chunks (" +
                    "world VARCHAR(64) NOT NULL, " +
                    "chunk_x INT NOT NULL, " +
                    "chunk_z INT NOT NULL, " +
                    "nation_id VARCHAR(36) NOT NULL, " +
                    "claimed_at BIGINT NOT NULL, " +
                    "PRIMARY KEY(world, chunk_x, chunk_z));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sovereignty_relations (" +
                    "nation_a VARCHAR(36) NOT NULL, " +
                    "nation_b VARCHAR(36) NOT NULL, " +
                    "relation_type VARCHAR(32) NOT NULL, " +
                    "expires_at BIGINT NOT NULL, " +
                    "PRIMARY KEY(nation_a, nation_b));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sovereignty_transactions (" +
                    "id INTEGER PRIMARY KEY " + (isMySql() ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                    "nation_id VARCHAR(36) NOT NULL, " +
                    "type VARCHAR(32) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "description TEXT, " +
                    "created_at BIGINT NOT NULL);");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd tworzenia tabel SQL: " + e.getMessage(), e);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (!enabled) {
            throw new SQLException("Baza danych SQL nie jest aktywna!");
        }
        if (connection == null || connection.isClosed() || (isMySql() && !connection.isValid(2))) {
            if (isMySql()) {
                connection = DriverManager.getConnection(jdbcUrl, user, password);
            } else {
                connection = DriverManager.getConnection(jdbcUrl);
            }
        }
        return connection;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isMySql() {
        return plugin.getConfig().getBoolean("database.mysql.enabled", false);
    }

    // --- Mury obronne (Wall Blocks) ---

    public void saveWallBlockAsync(UUID nationId, Location loc) {
        if (!enabled || loc.getWorld() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = isMySql()
                    ? "INSERT IGNORE INTO sovereignty_walls (nation_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)"
                    : "INSERT OR IGNORE INTO sovereignty_walls (nation_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nationId.toString());
                ps.setString(2, loc.getWorld().getName());
                ps.setInt(3, loc.getBlockX());
                ps.setInt(4, loc.getBlockY());
                ps.setInt(5, loc.getBlockZ());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd zapisu bloku muru SQL: " + e.getMessage());
            }
        });
    }

    public void saveAllWallBlocksBatchAsync(UUID nationId, Set<Location> blocks) {
        if (!enabled || blocks == null || blocks.isEmpty()) return;
        List<Location> copy = new ArrayList<>(blocks);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = isMySql()
                    ? "INSERT IGNORE INTO sovereignty_walls (nation_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)"
                    : "INSERT OR IGNORE INTO sovereignty_walls (nation_id, world, x, y, z) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Location loc : copy) {
                        if (loc.getWorld() == null) continue;
                        ps.setString(1, nationId.toString());
                        ps.setString(2, loc.getWorld().getName());
                        ps.setInt(3, loc.getBlockX());
                        ps.setInt(4, loc.getBlockY());
                        ps.setInt(5, loc.getBlockZ());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd batchowego zapisu murów SQL: " + e.getMessage());
            }
        });
    }

    public void deleteWallBlockAsync(Location loc) {
        if (!enabled || loc.getWorld() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM sovereignty_walls WHERE world = ? AND x = ? AND y = ? AND z = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, loc.getWorld().getName());
                ps.setInt(2, loc.getBlockX());
                ps.setInt(3, loc.getBlockY());
                ps.setInt(4, loc.getBlockZ());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd usuwania bloku muru SQL: " + e.getMessage());
            }
        });
    }

    public void clearNationWallsAsync(UUID nationId) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM sovereignty_walls WHERE nation_id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nationId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd czyszczenia murów nacji SQL: " + e.getMessage());
            }
        });
    }

    public Map<UUID, Set<Location>> loadAllWallBlocksSync() {
        Map<UUID, Set<Location>> result = new ConcurrentHashMap<>();
        if (!enabled) return result;

        String sql = "SELECT nation_id, world, x, y, z FROM sovereignty_walls";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID nId = UUID.fromString(rs.getString("nation_id"));
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");

                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    result.computeIfAbsent(nId, k -> Collections.synchronizedSet(new HashSet<>()))
                            .add(new Location(w, x, y, z));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Błąd odczytu murów obronnych z bazy SQL: " + e.getMessage());
        }
        return result;
    }

    // --- Relacje Dyplomatyczne (Diplomatic Relations) ---

    public void saveRelationAsync(UUID nationA, UUID nationB, DiplomaticRelationType type, long expiresAt) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = isMySql()
                    ? "REPLACE INTO sovereignty_relations (nation_a, nation_b, relation_type, expires_at) VALUES (?, ?, ?, ?)"
                    : "INSERT OR REPLACE INTO sovereignty_relations (nation_a, nation_b, relation_type, expires_at) VALUES (?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nationA.toString());
                ps.setString(2, nationB.toString());
                ps.setString(3, type.name());
                ps.setLong(4, expiresAt);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd zapisu relacji dyplomatycznej SQL: " + e.getMessage());
            }
        });
    }

    public void deleteRelationAsync(UUID nationA, UUID nationB) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM sovereignty_relations WHERE (nation_a = ? AND nation_b = ?) OR (nation_a = ? AND nation_b = ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nationA.toString());
                ps.setString(2, nationB.toString());
                ps.setString(3, nationB.toString());
                ps.setString(4, nationA.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd usuwania relacji dyplomatycznej SQL: " + e.getMessage());
            }
        });
    }

    // --- Chunki (Claimed Chunks) ---

    public void saveChunkAsync(NationClaimChunk chunk) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = isMySql()
                    ? "REPLACE INTO sovereignty_chunks (world, chunk_x, chunk_z, nation_id, claimed_at) VALUES (?, ?, ?, ?, ?)"
                    : "INSERT OR REPLACE INTO sovereignty_chunks (world, chunk_x, chunk_z, nation_id, claimed_at) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, chunk.getWorld());
                ps.setInt(2, chunk.getX());
                ps.setInt(3, chunk.getZ());
                ps.setString(4, chunk.getNationId().toString());
                ps.setLong(5, chunk.getClaimedAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd zapisu chunka SQL: " + e.getMessage());
            }
        });
    }

    public void deleteChunkAsync(String world, int cx, int cz) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM sovereignty_chunks WHERE world = ? AND chunk_x = ? AND chunk_z = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, world);
                ps.setInt(2, cx);
                ps.setInt(3, cz);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Błąd usuwania chunka SQL: " + e.getMessage());
            }
        });
    }

    public synchronized void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Połączenie z bazą danych SQL zostało bezpiecznie zamknięte.");
            }
        } catch (Throwable ignored) {}
    }
}
