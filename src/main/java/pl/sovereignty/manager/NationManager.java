package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationRole;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NationManager {
    private final Plugin plugin;
    private final Map<UUID, Nation> nationsById = new HashMap<>();
    private final Map<String, UUID> nationsByName = new HashMap<>();
    private final Map<UUID, UUID> playerNationMap = new HashMap<>();

    public NationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Nation createNation(String name, String tag, Player leader) {
        if (nationsByName.containsKey(name.toLowerCase())) {
            return null;
        }
        if (playerNationMap.containsKey(leader.getUniqueId())) {
            return null;
        }

        UUID nationId = UUID.randomUUID();
        Nation nation = new Nation(nationId, name, tag, leader.getUniqueId());
        nationsById.put(nationId, nation);
        nationsByName.put(name.toLowerCase(), nationId);
        playerNationMap.put(leader.getUniqueId(), nationId);

        saveNations();
        return nation;
    }

    public Nation getNation(UUID id) {
        return nationsById.get(id);
    }

    public Nation getNationByName(String name) {
        if (name == null) return null;
        UUID id = nationsByName.get(name.toLowerCase());
        return id != null ? nationsById.get(id) : null;
    }

    public Nation getNationByPlayer(UUID playerUuid) {
        UUID nationId = playerNationMap.get(playerUuid);
        return nationId != null ? nationsById.get(nationId) : null;
    }

    public Collection<Nation> getAllNations() {
        return nationsById.values();
    }

    private final Map<UUID, Map<UUID, Long>> pendingInvites = new HashMap<>();

    public void invitePlayer(UUID playerUuid, UUID nationId) {
        pendingInvites.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(nationId, System.currentTimeMillis() + 120_000L); // Ważne przez 2 minuty
    }

    public boolean hasInvite(UUID playerUuid, UUID nationId) {
        Map<UUID, Long> invites = pendingInvites.get(playerUuid);
        if (invites == null) return false;
        Long expireTime = invites.get(nationId);
        if (expireTime == null) return false;
        if (System.currentTimeMillis() > expireTime) {
            invites.remove(nationId);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID playerUuid, UUID nationId) {
        Map<UUID, Long> invites = pendingInvites.get(playerUuid);
        if (invites != null) {
            invites.remove(nationId);
        }
    }

    public boolean joinNation(Player player, Nation nation) {
        if (playerNationMap.containsKey(player.getUniqueId())) {
            return false;
        }
        nation.addMember(player.getUniqueId(), NationRole.CITIZEN);
        playerNationMap.put(player.getUniqueId(), nation.getId());
        saveNations();
        return true;
    }

    public boolean leaveNation(Player player) {
        UUID nationId = playerNationMap.get(player.getUniqueId());
        if (nationId == null) return false;

        Nation nation = nationsById.get(nationId);
        if (nation != null) {
            if (nation.getLeaderUuid().equals(player.getUniqueId())) {
                return false; // Leader cannot simply leave, must transfer or disband
            }
            nation.removeMember(player.getUniqueId());
            playerNationMap.remove(player.getUniqueId());
            saveNations();
            return true;
        }
        return false;
    }

    public void disbandNation(Nation nation) {
        for (UUID member : nation.getMembers().keySet()) {
            playerNationMap.remove(member);
        }
        nationsByName.remove(nation.getName().toLowerCase());
        nationsById.remove(nation.getId());
        saveNations();
    }

    public void saveNations() {
        File file = new File(plugin.getDataFolder(), "nations.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Nation nation : nationsById.values()) {
            String path = "nations." + nation.getId().toString();
            config.set(path + ".name", nation.getName());
            config.set(path + ".tag", nation.getTag());
            config.set(path + ".leader", nation.getLeaderUuid().toString());
            config.set(path + ".treasury", nation.getTreasury());
            config.set(path + ".food", nation.getFoodStockpile());
            config.set(path + ".wood", nation.getWoodStockpile());
            config.set(path + ".rawMaterials", nation.getRawMaterialsStockpile());
            config.set(path + ".minted", nation.getMintedCurrency());
            config.set(path + ".tradeVolume", nation.getTradeVolume());
            config.set(path + ".gdp", nation.getGdp());
            config.set(path + ".militaryScore", nation.getMilitaryScore());
            config.set(path + ".piratesDefeated", nation.getPiratesDefeated());
            config.set(path + ".tributesPaid", nation.getTributesPaid());
            config.set(path + ".customsTaxRate", nation.getCustomsTaxRate());
            config.set(path + ".starving", nation.isStarving());

            // Nowe systemy:
            config.set(path + ".bankBalance", nation.getBankBalance());
            config.set(path + ".regime", nation.getRegime().name());
            config.set(path + ".techTier", nation.getTechTier().name());
            config.set(path + ".wallTier", nation.getWallTier().name());
            config.set(path + ".vaultTier", nation.getVaultTier().name());
            config.set(path + ".vaultAutoSell", nation.isVaultAutoSell());
            config.set(path + ".mapColorHex", nation.getMapColorHex());
            config.set(path + ".lastClaimTimestamp", nation.getLastClaimTimestamp());
            config.set(path + ".isIndebted", nation.isIndebted());
            config.set(path + ".debtDays", nation.getDebtDays());
            config.set(path + ".farmCount", nation.getFarmCount());
            config.set(path + ".raidThreatChance", nation.getRaidThreatChance());
            config.set(path + ".lastRaidThreatIncrementTime", nation.getLastRaidThreatIncrementTime());
            config.set(path + ".siegeWindowStartHour", nation.getSiegeWindowStartHour());
            config.set(path + ".siegeWindowEndHour", nation.getSiegeWindowEndHour());
            config.set(path + ".declaredWarCount", nation.getDeclaredWarCount());
            config.set(path + ".overlordNationId", nation.getOverlordNationId() != null ? nation.getOverlordNationId().toString() : null);
            config.set(path + ".tributeDaysRemaining", nation.getTributeDaysRemaining());

            List<String> relList = new ArrayList<>();
            for (Map.Entry<UUID, pl.sovereignty.model.DiplomaticRelationType> entry : nation.getAllRelations().entrySet()) {
                Long exp = nation.getRelationExpiries().get(entry.getKey());
                relList.add(entry.getKey().toString() + ":" + entry.getValue().name() + ":" + (exp != null ? exp : -1L));
            }
            config.set(path + ".relations", relList);

            List<String> chunkList = new ArrayList<>();
            for (pl.sovereignty.model.NationClaimChunk c : nation.getClaimedChunks()) {
                chunkList.add(c.getWorld() + ":" + c.getX() + ":" + c.getZ() + ":" + c.getClaimedAt());
            }
            config.set(path + ".claimedChunks", chunkList);

            List<String> memberList = new ArrayList<>();
            for (Map.Entry<UUID, NationRole> entry : nation.getMembers().entrySet()) {
                memberList.add(entry.getKey().toString() + ":" + entry.getValue().name());
            }
            config.set(path + ".members", memberList);

            List<String> structList = new ArrayList<>();
            for (UUID sId : nation.getStructures()) {
                structList.add(sId.toString());
            }
            config.set(path + ".structures", structList);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać nations.yml: " + e.getMessage());
        }
    }

    public void loadNations() {
        File file = new File(plugin.getDataFolder(), "nations.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("nations");
        if (section == null) return;

        nationsById.clear();
        nationsByName.clear();
        playerNationMap.clear();

        for (String idStr : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idStr);
                String name = section.getString(idStr + ".name");
                String tag = section.getString(idStr + ".tag");
                UUID leaderUuid = UUID.fromString(section.getString(idStr + ".leader"));

                Nation nation = new Nation(id, name, tag, leaderUuid);
                nation.setTreasury(section.getDouble(idStr + ".treasury", 100.0));
                nation.setFoodStockpile(section.getDouble(idStr + ".food", 50.0));
                nation.setWoodStockpile(section.getDouble(idStr + ".wood", 50.0));
                nation.setRawMaterialsStockpile(section.getDouble(idStr + ".rawMaterials", 20.0));
                nation.addMintedCurrency(section.getDouble(idStr + ".minted", 0.0));
                nation.addTradeVolume(section.getDouble(idStr + ".tradeVolume", 0.0));
                nation.setGdp(section.getDouble(idStr + ".gdp", 100.0));
                nation.setMilitaryScore(section.getInt(idStr + ".militaryScore", 0));
                nation.setCustomsTaxRate(section.getDouble(idStr + ".customsTaxRate", 0.005));
                nation.setStarving(section.getBoolean(idStr + ".starving", false));

                int pirDef = section.getInt(idStr + ".piratesDefeated", 0);
                for (int i = 0; i < pirDef; i++) nation.incrementPiratesDefeated();

                int trib = section.getInt(idStr + ".tributesPaid", 0);
                for (int i = 0; i < trib; i++) nation.incrementTributesPaid();

                List<String> memberList = section.getStringList(idStr + ".members");
                for (String m : memberList) {
                    String[] parts = m.split(":");
                    UUID pUuid = UUID.fromString(parts[0]);
                    NationRole role = NationRole.valueOf(parts[1]);
                    nation.addMember(pUuid, role);
                    playerNationMap.put(pUuid, id);
                }

                nation.setBankBalance(section.getDouble(idStr + ".bankBalance", 2000.0));
                nation.setFarmCount(section.getInt(idStr + ".farmCount", 0));
                try {
                    nation.setRegime(pl.sovereignty.model.PoliticalRegime.valueOf(section.getString(idStr + ".regime", "HEREDITARY_MONARCHY")));
                } catch (Exception ignored) {}
                try {
                    nation.setTechTier(pl.sovereignty.model.TechnologyTier.valueOf(section.getString(idStr + ".techTier", "TIER_1_PRIMITIVE")));
                } catch (Exception ignored) {}
                try {
                    nation.setWallTier(pl.sovereignty.model.WallTier.valueOf(section.getString(idStr + ".wallTier", "NONE")));
                } catch (Exception ignored) {}
                try {
                    nation.setVaultTier(pl.sovereignty.model.VaultTier.valueOf(section.getString(idStr + ".vaultTier", "TIER_1_WOODEN")));
                } catch (Exception ignored) {}
                nation.setVaultAutoSell(section.getBoolean(idStr + ".vaultAutoSell", true));
                nation.setMapColorHex(section.getString(idStr + ".mapColorHex", "#3388FF"));
                nation.setLastClaimTimestamp(section.getLong(idStr + ".lastClaimTimestamp", 0L));
                nation.setIndebted(section.getBoolean(idStr + ".isIndebted", false));
                nation.setDebtDays(section.getInt(idStr + ".debtDays", 0));
                nation.setRaidThreatChance(section.getDouble(idStr + ".raidThreatChance", 0.0));
                nation.setLastRaidThreatIncrementTime(section.getLong(idStr + ".lastRaidThreatIncrementTime", 0L));
                nation.setSiegeWindowStartHour(section.getInt(idStr + ".siegeWindowStartHour", 18));
                nation.setSiegeWindowEndHour(section.getInt(idStr + ".siegeWindowEndHour", 22));
                nation.setDeclaredWarCount(section.getInt(idStr + ".declaredWarCount", 0));
                String ovId = section.getString(idStr + ".overlordNationId", null);
                if (ovId != null && !ovId.isEmpty()) {
                    try {
                        nation.setOverlordNationId(UUID.fromString(ovId));
                    } catch (Exception ignored) {}
                }
                nation.setTributeDaysRemaining(section.getInt(idStr + ".tributeDaysRemaining", 0));

                List<String> relList = section.getStringList(idStr + ".relations");
                for (String rStr : relList) {
                    try {
                        String[] parts = rStr.split(":");
                        UUID otherId = UUID.fromString(parts[0]);
                        pl.sovereignty.model.DiplomaticRelationType relType = pl.sovereignty.model.DiplomaticRelationType.valueOf(parts[1]);
                        long exp = Long.parseLong(parts[2]);
                        long duration = exp > 0 ? (exp - System.currentTimeMillis()) : 0;
                        if (exp <= 0 || duration > 0) {
                            nation.setRelation(otherId, relType, duration > 0 ? duration : 0);
                        }
                    } catch (Exception ignored) {}
                }

                List<String> chunkList = section.getStringList(idStr + ".claimedChunks");
                for (String cStr : chunkList) {
                    try {
                        String[] parts = cStr.split(":");
                        String cWorld = parts[0];
                        int cX = Integer.parseInt(parts[1]);
                        int cZ = Integer.parseInt(parts[2]);
                        long cTime = parts.length > 3 ? Long.parseLong(parts[3]) : System.currentTimeMillis();
                        nation.addClaimedChunk(new pl.sovereignty.model.NationClaimChunk(cWorld, cX, cZ, id, cTime));
                    } catch (Exception ignored) {}
                }

                List<String> structList = section.getStringList(idStr + ".structures");
                for (String s : structList) {
                    nation.addStructure(UUID.fromString(s));
                }

                nationsById.put(id, nation);
                nationsByName.put(name.toLowerCase(), id);
            } catch (Exception e) {
                plugin.getLogger().warning("Błąd ładowania państwa " + idStr + ": " + e.getMessage());
            }
        }
    }

    public void broadcastToNation(Nation nation, String message) {
        if (nation == null || message == null) return;
        for (UUID memberUuid : nation.getMembers().keySet()) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}
