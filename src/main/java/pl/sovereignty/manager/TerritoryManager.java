package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;
import pl.sovereignty.model.StructureType;
import pl.sovereignty.model.TerritoryStructure;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TerritoryManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final Map<UUID, TerritoryStructure> structuresById = new HashMap<>();
    private final Map<Location, UUID> structureLocations = new HashMap<>();

    public TerritoryManager(Plugin plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public TerritoryStructure registerStructure(Nation nation, StructureType type, Location loc) {
        UUID id = UUID.randomUUID();
        TerritoryStructure structure = new TerritoryStructure(id, nation.getId(), type, loc.getBlock().getLocation());
        structuresById.put(id, structure);
        structureLocations.put(loc.getBlock().getLocation(), id);
        nation.addStructure(id);
        if (type == StructureType.STATE_FARM) {
            nation.incrementFarmCount();
        }
        saveStructures();
        return structure;
    }

    public void removeStructure(TerritoryStructure structure) {
        structuresById.remove(structure.getId());
        structureLocations.remove(structure.getLocation());
        Nation nation = nationManager.getNation(structure.getNationId());
        if (nation != null) {
            nation.removeStructure(structure.getId());
            if (structure.getType() == StructureType.STATE_FARM) {
                nation.decrementFarmCount();
            }
        }
        saveStructures();
    }

    public TerritoryStructure getStructureAt(Location loc) {
        UUID id = structureLocations.get(loc.getBlock().getLocation());
        return id != null ? structuresById.get(id) : null;
    }

    public TerritoryStructure getStructure(UUID id) {
        return structuresById.get(id);
    }

    public Collection<TerritoryStructure> getAllStructures() {
        return structuresById.values();
    }

    public List<TerritoryStructure> getNationStructures(UUID nationId) {
        List<TerritoryStructure> list = new ArrayList<>();
        for (TerritoryStructure s : structuresById.values()) {
            if (s.getNationId().equals(nationId)) {
                list.add(s);
            }
        }
        return list;
    }

    public TerritoryStructure getNationBastion(UUID nationId) {
        for (TerritoryStructure s : structuresById.values()) {
            if (s.getNationId().equals(nationId) && s.getType() == StructureType.BASTION_CORE) {
                return s;
            }
        }
        return null;
    }

    public void removeAllStructuresForNation(UUID nationId) {
        List<TerritoryStructure> list = getNationStructures(nationId);
        for (TerritoryStructure s : list) {
            structuresById.remove(s.getId());
            structureLocations.remove(s.getLocation());
        }
        saveStructures();
    }

    /**
     * Zwraca państwo, którego strefa wpływów obejmuje daną lokalizację.
     */
    public Nation getNationAt(Location location) {
        if (location == null || location.getWorld() == null) return null;

        // 1. Sprawdź zajęte chunki państwa (/claim)
        String worldName = location.getWorld().getName();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        for (Nation nation : nationManager.getAllNations()) {
            if (nation.hasClaimedChunk(worldName, chunkX, chunkZ)) {
                return nation;
            }
        }

        // 2. Sprawdź strefę wpływów struktur (Bastion, Strażnice, Tartak)
        for (TerritoryStructure s : structuresById.values()) {
            Location sLoc = s.getLocation();
            if (!sLoc.getWorld().equals(location.getWorld())) continue;

            double distance = sLoc.distance(location);
            if (distance <= getEffectiveInfluenceRadius(s)) {
                return nationManager.getNation(s.getNationId());
            }
        }
        return null;
    }

    /**
     * Zwraca efektywny zasięg wpływu struktury z uwzględnieniem poziomu technologii (np. Tartak)
     * oraz bonusów ustrojowych (np. Autokracja: +15 do Bastionu i Strażnic).
     */
    public int getEffectiveInfluenceRadius(TerritoryStructure s) {
        int base = s.getType().getInfluenceRadius();
        Nation nation = nationManager.getNation(s.getNationId());
        if (nation == null) return base;

        if (s.getType() == StructureType.SAWMILL_TOTEM) {
            base = switch (nation.getTechTier()) {
                case TIER_1_PRIMITIVE -> 15;
                case TIER_2_STEEL_TOOLS -> 20;
                case TIER_3_EARLY_MECHANIZATION -> 25;
                case TIER_4_INDUSTRIAL_REVOLUTION -> 30;
                case TIER_5_MASTER_CRAFTSMANSHIP -> 35;
            };
        }

        if (nation.getRegime() == PoliticalRegime.AUTOCRACY &&
                (s.getType() == StructureType.BASTION_CORE || s.getType() == StructureType.WATCHTOWER)) {
            base += 15;
        }
        return base;
    }

    /**
     * Weryfikacja Traktu Zaopatrzeniowego (Supply Route):
     * Każda struktura produkcyjna musi mieć fizyczne połączenie z Bastionem.
     * Sprawdzamy czy istnieje łańcuch połączonych strażnic lub bezpośrednia odległość do bastionu,
     * oraz czy podłoże traktu nie zostało przerwane.
     */
    public void verifyAllSupplyRoutes() {
        for (Nation nation : nationManager.getAllNations()) {
            TerritoryStructure bastion = getNationBastion(nation.getId());
            if (bastion == null) {
                // Jeśli państwo nie ma jeszcze postawionego bastionu, pozwalamy strukturom działać lokalnie
                for (TerritoryStructure s : getNationStructures(nation.getId())) {
                    if (s.getType().isRequiresSupplyRoute()) {
                        s.setSupplyConnected(true);
                    }
                }
                continue;
            }

            List<TerritoryStructure> towers = new ArrayList<>();
            towers.add(bastion);
            for (TerritoryStructure s : getNationStructures(nation.getId())) {
                if (s.getType() == StructureType.WATCHTOWER) {
                    towers.add(s);
                }
            }

            for (TerritoryStructure s : getNationStructures(nation.getId())) {
                if (!s.getType().isRequiresSupplyRoute()) continue;

                boolean connected = isConnectedToNetwork(nation, s.getLocation(), towers);
                if (s.isSupplyConnected() != connected) {
                    s.setSupplyConnected(connected);
                    if (!connected) {
                        broadcastNationAlert(nation, "&c[TRAKT ZAOPATRZENIOWY] Przerwano szlak zaopatrzeniowy do &e"
                                + s.getType().getDisplayName() + "&c! Produkcja wstrzymana!");
                    } else {
                        broadcastNationAlert(nation, "&a[TRAKT ZAOPATRZENIOWY] Szlak zaopatrzeniowy do &e"
                                + s.getType().getDisplayName() + "&a został przywrócony!");
                    }
                }
            }
        }
    }

    private boolean isConnectedToNetwork(Nation nation, Location target, List<TerritoryStructure> networkNodes) {
        if (target == null || target.getWorld() == null) return false;

        // 1. Jeśli struktura stoi na terenie zclaimowanym przez państwo, trakt jest bezpieczny
        if (nation != null && nation.hasClaimedChunk(target.getWorld().getName(), target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
            return true;
        }

        // 2. Struktura musi być w zasięgu 65 bloków od bastionu lub którejkolwiek wieży strażniczej
        for (TerritoryStructure node : networkNodes) {
            if (node.getLocation().getWorld().equals(target.getWorld())) {
                if (node.getLocation().distance(target) <= 65.0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void broadcastNationAlert(Nation nation, String message) {
        for (UUID member : nation.getMembers().keySet()) {
            org.bukkit.entity.Player p = Bukkit.getPlayer(member);
            if (p != null && p.isOnline()) {
                p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    public void saveStructures() {
        File file = new File(plugin.getDataFolder(), "structures.yml");
        FileConfiguration config = new YamlConfiguration();

        for (TerritoryStructure s : structuresById.values()) {
            String path = "structures." + s.getId().toString();
            config.set(path + ".nationId", s.getNationId().toString());
            config.set(path + ".type", s.getType().name());
            config.set(path + ".world", s.getLocation().getWorld().getName());
            config.set(path + ".x", s.getLocation().getBlockX());
            config.set(path + ".y", s.getLocation().getBlockY());
            config.set(path + ".z", s.getLocation().getBlockZ());
            config.set(path + ".health", s.getHealth());
            config.set(path + ".supplyConnected", s.isSupplyConnected());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd zapisu structures.yml: " + e.getMessage());
        }
    }

    public void loadStructures() {
        File file = new File(plugin.getDataFolder(), "structures.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("structures");
        if (section == null) return;

        structuresById.clear();
        structureLocations.clear();

        for (String idStr : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                UUID nationId = UUID.fromString(section.getString(idStr + ".nationId"));
                StructureType type = StructureType.valueOf(section.getString(idStr + ".type"));
                World world = Bukkit.getWorld(section.getString(idStr + ".world"));
                if (world == null) continue;

                int x = section.getInt(idStr + ".x");
                int y = section.getInt(idStr + ".y");
                int z = section.getInt(idStr + ".z");
                Location loc = new Location(world, x, y, z);

                TerritoryStructure structure = new TerritoryStructure(id, nationId, type, loc);
                structure.setHealth(section.getDouble(idStr + ".health", structure.getMaxHealth()));
                structure.setSupplyConnected(section.getBoolean(idStr + ".supplyConnected", true));

                structuresById.put(id, structure);
                structureLocations.put(loc, id);
            } catch (Exception e) {
                plugin.getLogger().warning("Błąd wczytywania struktury " + idStr + ": " + e.getMessage());
            }
        }

        // Synchronizacja liczby farm państwowych dla każdej nacji
        for (Nation n : nationManager.getAllNations()) {
            n.setFarmCount(getFarmCount(n.getId()));
        }
    }

    public static final int MAX_PRODUCTION_STRUCTURES = 10;

    public int getStructureCount(UUID nationId, StructureType type) {
        int count = 0;
        for (TerritoryStructure s : structuresById.values()) {
            if (s.getNationId().equals(nationId) && s.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public int getFarmCount(UUID nationId) {
        return getStructureCount(nationId, StructureType.STATE_FARM);
    }
}
