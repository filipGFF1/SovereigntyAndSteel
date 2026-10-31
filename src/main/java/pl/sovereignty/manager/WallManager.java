package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WallManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final VaultEconomyManager economyManager;
    private MapManager mapManager;
    private DatabaseManager databaseManager;

    // Przechowuje bloki postawionych murów dla każdego państwa
    private final Map<UUID, Set<Location>> nationWallBlocks = new HashMap<>();

    public WallManager(Plugin plugin, NationManager nationManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.economyManager = economyManager;
        loadWallBlocks();
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        if (databaseManager != null && databaseManager.isEnabled()) {
            Map<UUID, Set<Location>> sqlWalls = databaseManager.loadAllWallBlocksSync();
            if (!sqlWalls.isEmpty()) {
                nationWallBlocks.clear();
                nationWallBlocks.putAll(sqlWalls);
            } else if (!nationWallBlocks.isEmpty()) {
                // Migracja z YAML do bazy danych SQL
                for (Map.Entry<UUID, Set<Location>> entry : nationWallBlocks.entrySet()) {
                    databaseManager.saveAllWallBlocksBatchAsync(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void setSquaremapHook(SquaremapHook squaremapHook) {
        // Zgodność wsteczna
    }

    /**
     * Oblicza obwód zewnętrzny państwa w metrach bieżących (liczba kratek granicznych stykających się z dziczą).
     */
    public int calculatePerimeterMeters(Nation nation) {
        int meters = 0;
        Set<NationClaimChunk> chunks = nation.getClaimedChunks();
        if (chunks.isEmpty()) return 0;

        for (NationClaimChunk chunk : chunks) {
            String world = chunk.getWorld();
            int cx = chunk.getX();
            int cz = chunk.getZ();

            // Północ (cz - 1)
            if (!nation.hasClaimedChunk(world, cx, cz - 1)) meters += 16;
            // Południe (cz + 1)
            if (!nation.hasClaimedChunk(world, cx, cz + 1)) meters += 16;
            // Zachód (cx - 1)
            if (!nation.hasClaimedChunk(world, cx - 1, cz)) meters += 16;
            // Wschód (cx + 1)
            if (!nation.hasClaimedChunk(world, cx + 1, cz)) meters += 16;
        }
        return meters;
    }

    /**
     * Oblicza koszt budowy lub upgrade'u muru z uwzględnieniem ustroju (np. Monarchia -10%).
     */
    public double calculateWallCost(Nation nation, WallTier targetTier) {
        int meters = calculatePerimeterMeters(nation);
        double baseCost = meters * targetTier.getCostPerMeter();
        double multiplier = nation.getRegime().getWallCostMultiplier();
        return baseCost * multiplier;
    }

    /**
     * Stawia lub uaktualnia mur obronny państwa za opłatą z Vault.
     */
    public boolean buildOrUpgradeWall(Player player, Nation nation, WallTier newTier) {
        if (newTier == WallTier.NONE) {
            clearNationWalls(nation);
            nation.setWallTier(WallTier.NONE);
            player.sendMessage(ChatColor.YELLOW + "Pomyślnie zdemontowano mury obronne nacji.");
            if (mapManager != null) mapManager.updateNation(nation);
            nationManager.saveNations();
            return true;
        }

        if (nation.getClaimedChunks().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Twoje państwo musi posiadać przynajmniej 1 skolonizowany chunk, aby stawiać mury!");
            return false;
        }

        double cost = calculateWallCost(nation, newTier);
        if (nation.getBankBalance() < cost) {
            player.sendMessage(ChatColor.RED + "Brak wystarczających funduszy w Skarbcu Narodowym na budowę murów!");
            player.sendMessage(ChatColor.GRAY + "Wymagane: " + ChatColor.GOLD + economyManager.format(cost)
                    + ChatColor.GRAY + ", Stan: " + ChatColor.YELLOW + economyManager.format(nation.getBankBalance()));
            return false;
        }

        // Pobranie opłaty
        nation.withdrawBank(cost);

        // Usuń ewentualne stare mury
        clearNationWalls(nation);

        // Postaw nowy mur
        nation.setWallTier(newTier);
        constructWallBlocks(nation, newTier);

        player.sendMessage(ChatColor.GREEN + "Pomyślnie wzniesiono " + ChatColor.GOLD + newTier.getDisplayName() + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.GRAY + "Obwód fortyfikacji: " + ChatColor.WHITE + calculatePerimeterMeters(nation) + " m.b."
                + ChatColor.GRAY + " (Pobrano: " + ChatColor.GOLD + economyManager.format(cost) + ChatColor.GRAY + ")");

        if (mapManager != null) {
            mapManager.updateNation(nation);
        }

        nationManager.saveNations();
        saveWallBlocks();
        return true;
    }

    /**
     * Automatyczne przesuwanie muru po zakupie/unclaimie chunka:
     * Usuwa odcinki muru, które znalazły się wewnątrz terytorium nacji,
     * i stawia nowy mur na nowej krawędzi zewnętrznej bez dodatkowych opłat robocizny!
     */
    public void rebuildNationWalls(Nation nation) {
        if (nation.getWallTier() == WallTier.NONE || nation.getClaimedChunks().isEmpty()) {
            clearNationWalls(nation);
            return;
        }

        clearNationWalls(nation);
        constructWallBlocks(nation, nation.getWallTier());
        saveWallBlocks();
    }

    private void constructWallBlocks(Nation nation, WallTier tier) {
        Set<Location> placedBlocks = new HashSet<>();
        Set<NationClaimChunk> chunks = nation.getClaimedChunks();

        for (NationClaimChunk chunk : chunks) {
            World world = Bukkit.getWorld(chunk.getWorld());
            if (world == null) continue;

            int cx = chunk.getX();
            int cz = chunk.getZ();

            // Północ (cz - 1): krawędź z = 0 wewnątrz chunka
            if (!nation.hasClaimedChunk(world.getName(), cx, cz - 1)) {
                buildEdge(world, cx * 16, cz * 16, cx * 16 + 15, cz * 16, tier, placedBlocks);
            }
            // Południe (cz + 1): krawędź z = 15 wewnątrz chunka
            if (!nation.hasClaimedChunk(world.getName(), cx, cz + 1)) {
                buildEdge(world, cx * 16, cz * 16 + 15, cx * 16 + 15, cz * 16 + 15, tier, placedBlocks);
            }
            // Zachód (cx - 1): krawędź x = 0 wewnątrz chunka
            if (!nation.hasClaimedChunk(world.getName(), cx - 1, cz)) {
                buildEdge(world, cx * 16, cz * 16, cx * 16, cz * 16 + 15, tier, placedBlocks);
            }
            // Wschód (cx + 1): krawędź x = 15 wewnątrz chunka
            if (!nation.hasClaimedChunk(world.getName(), cx + 1, cz)) {
                buildEdge(world, cx * 16 + 15, cz * 16, cx * 16 + 15, cz * 16 + 15, tier, placedBlocks);
            }
        }

        nationWallBlocks.put(nation.getId(), placedBlocks);
        if (databaseManager != null && databaseManager.isEnabled()) {
            databaseManager.saveAllWallBlocksBatchAsync(nation.getId(), placedBlocks);
        }
    }

    private void buildEdge(World world, int x1, int z1, int x2, int z2, WallTier tier, Set<Location> placed) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int surfaceY = world.getHighestBlockYAt(x, z);
                // Nie stawiaj muru głęboko pod wodą lub powyżej nieba
                if (surfaceY < 50 || surfaceY > 310) continue;

                Block baseBlock = world.getBlockAt(x, surfaceY, z);
                if (baseBlock.isLiquid()) continue;

                // Budowa muru o wysokości 4 bloków od powierzchni
                for (int h = 1; h <= 4; h++) {
                    Block wallBlock = world.getBlockAt(x, surfaceY + h, z);
                    // Nie niszcz istniejących ważnych struktur
                    if (!wallBlock.getType().isAir() && !wallBlock.getType().name().contains("LEAVES")
                            && !wallBlock.getType().name().contains("GRASS") && !wallBlock.getType().name().contains("FLOWER")) {
                        continue;
                    }

                    Material mat = getWallMaterialForLayer(tier, h);
                    wallBlock.setType(mat, false);
                    placed.add(wallBlock.getLocation());
                }
            }
        }
    }

    private Material getWallMaterialForLayer(WallTier tier, int layer) {
        return switch (tier) {
            case TIER_1_WOODEN_PALISADE -> (layer == 4) ? Material.OAK_FENCE : Material.STRIPPED_OAK_LOG;
            case TIER_2_STONE_WALL -> {
                if (layer == 1) yield Material.COBBLESTONE;
                if (layer == 4) yield Material.STONE_BRICK_SLAB;
                yield Material.STONE_BRICKS;
            }
            case TIER_3_COPPER_DEEPSLATE -> {
                if (layer == 1) yield Material.POLISHED_DEEPSLATE;
                if (layer == 2 || layer == 3) yield Material.DEEPSLATE_BRICKS;
                yield Material.CUT_COPPER_SLAB;
            }
            default -> Material.STONE_BRICKS;
        };
    }

    public void clearNationWalls(Nation nation) {
        Set<Location> blocks = nationWallBlocks.remove(nation.getId());
        if (blocks != null) {
            for (Location loc : blocks) {
                if (loc.getWorld() != null) {
                    Block b = loc.getBlock();
                    if (isWallBlockMaterial(b.getType())) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
        if (databaseManager != null && databaseManager.isEnabled()) {
            databaseManager.clearNationWallsAsync(nation.getId());
        }
    }

    public boolean isWallBlockMaterial(Material mat) {
        return mat == Material.OAK_FENCE || mat == Material.STRIPPED_OAK_LOG
                || mat == Material.COBBLESTONE || mat == Material.STONE_BRICKS || mat == Material.STONE_BRICK_SLAB
                || mat == Material.POLISHED_DEEPSLATE || mat == Material.DEEPSLATE_BRICKS || mat == Material.CUT_COPPER_SLAB;
    }

    public boolean isWallBlock(Location loc) {
        for (Set<Location> blocks : nationWallBlocks.values()) {
            if (blocks.contains(loc)) return true;
        }
        return false;
    }

    public void handleWallBlockDestroyed(Location loc) {
        for (Set<Location> blocks : nationWallBlocks.values()) {
            blocks.remove(loc);
        }
        if (databaseManager != null && databaseManager.isEnabled()) {
            databaseManager.deleteWallBlockAsync(loc);
        }
    }

    public void saveWallBlocks() {
        if (databaseManager != null && databaseManager.isEnabled()) {
            // Zapis jest asynchronicznie delegowany do SQL - brak lag spikes na głównym wątku!
            return;
        }
        File file = new File(plugin.getDataFolder(), "walls.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Set<Location>> entry : nationWallBlocks.entrySet()) {
            List<String> list = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                if (loc.getWorld() != null) {
                    list.add(loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
                }
            }
            config.set("walls." + entry.getKey().toString(), list);
        }

        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException ignored) {}
    }

    public void loadWallBlocks() {
        File file = new File(plugin.getDataFolder(), "walls.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("walls");
        if (sec == null) return;

        nationWallBlocks.clear();
        for (String idStr : sec.getKeys(false)) {
            try {
                UUID nId = UUID.fromString(idStr);
                List<String> list = sec.getStringList(idStr);
                Set<Location> set = new HashSet<>();
                for (String s : list) {
                    String[] parts = s.split(":");
                    World w = Bukkit.getWorld(parts[0]);
                    if (w != null) {
                        set.add(new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                    }
                }
                nationWallBlocks.put(nId, set);
            } catch (Exception ignored) {}
        }
    }
}
