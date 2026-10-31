package pl.sovereignty.manager;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.*;

import java.util.*;

public class ChunkClaimManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final VaultEconomyManager economyManager;
    private WallManager wallManager;
    private MapManager mapManager;
    private WarManager warManager;
    private TerritoryManager territoryManager;

    public ChunkClaimManager(Plugin plugin, NationManager nationManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.economyManager = economyManager;
    }

    public void setWallManager(WallManager wallManager) {
        this.wallManager = wallManager;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void setWarManager(WarManager warManager) {
        this.warManager = warManager;
    }

    public void setTerritoryManager(TerritoryManager territoryManager) {
        this.territoryManager = territoryManager;
    }

    public void setSquaremapHook(SquaremapHook squaremapHook) {
        // Zgodność wsteczna
    }

    /**
     * Zredukowany koszt chunka:
     * Wzór: Koszt Chunka = 150 $ + (Liczba Posiadanych Chunków * 15 $)
     */
    public double calculateChunkCost(Nation nation) {
        int currentCount = nation.getClaimedChunks().size();
        return 150.0 + (currentCount * 15.0);
    }

    /**
     * Zwraca czas w sekundach pozostały do końca cooldownu asymilacji.
     */
    public long getRemainingCooldownSeconds(Nation nation) {
        long last = nation.getLastClaimTimestamp();
        if (last <= 0) return 0;

        int cooldownSeconds = nation.getRegime().getClaimCooldownSeconds(); // 900s (15 min) lub 600s w Militaryzmie
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        long remaining = cooldownSeconds - elapsedSeconds;
        return Math.max(0, remaining);
    }

    public String formatRemainingTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return mins + "m " + secs + "s";
    }

    public Nation getNationAtChunk(String world, int chunkX, int chunkZ) {
        for (Nation nation : nationManager.getAllNations()) {
            if (nation.hasClaimedChunk(world, chunkX, chunkZ)) {
                return nation;
            }
        }
        return null;
    }

    /**
     * Sprawdza zasadę ciągłości terytorialnej:
     * Nowo kupowany chunk musi bezpośrednio stykać się przynajmniej jedną krawędzią (N, S, E, W)
     * z dotychczasowym terytorium nacji (chyba że to pierwszy chunk państwa).
     */
    public boolean hasAdjacentClaim(Nation nation, String world, int chunkX, int chunkZ) {
        if (nation.getClaimedChunks().isEmpty()) {
            return true; // Pierwszy chunk stolicy
        }

        for (NationClaimChunk c : nation.getClaimedChunks()) {
            if (!c.getWorld().equalsIgnoreCase(world)) continue;
            // Sąsiad z lewej, prawej, góry, dołu
            if ((Math.abs(c.getX() - chunkX) == 1 && c.getZ() == chunkZ)
                    || (Math.abs(c.getZ() - chunkZ) == 1 && c.getX() == chunkX)) {
                return true;
            }
        }
        return false;
    }

    public boolean claimChunk(Player player, Nation nation) {
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // 1. Sprawdź czy ktoś już nie posiada tego chunka
        Nation existingOwner = getNationAtChunk(world.getName(), chunkX, chunkZ);
        if (existingOwner != null) {
            if (existingOwner.getId().equals(nation.getId())) {
                player.sendMessage(ChatColor.YELLOW + "Ten chunk należy już do Twojego państwa!");
            } else {
                player.sendMessage(ChatColor.RED + "Ten chunk należy już do obcego państwa: "
                        + existingOwner.getName() + " [" + existingOwner.getTag() + "]!");
            }
            return false;
        }

        // 1b. Claim-Blocking: Blokada roszczenia w buforze < 3 chunków od innego państwa (chyba że trwa wojna)
        int buffer = 3;
        for (Nation otherNation : nationManager.getAllNations()) {
            if (otherNation.getId().equals(nation.getId())) continue;
            boolean atWar = (warManager != null && warManager.isAtWar(nation.getId(), otherNation.getId()));
            if (atWar) continue;

            for (NationClaimChunk oc : otherNation.getClaimedChunks()) {
                if (!oc.getWorld().equalsIgnoreCase(world.getName())) continue;
                if (Math.abs(oc.getX() - chunkX) <= buffer && Math.abs(oc.getZ() - chunkZ) <= buffer) {
                    player.sendMessage(ChatColor.RED + "[CLAIM-BLOCKING] Nie możesz rościć chunka w strefie buforowej (<3 chunki) od państwa "
                            + otherNation.getName() + " [" + otherNation.getTag() + "] bez wypowiedzenia wojny!");
                    return false;
                }
            }
        }

        // 2. Sprawdź cooldown asymilacji (15 min / 10 min w Militaryzmie)
        long remainingCooldown = getRemainingCooldownSeconds(nation);
        if (remainingCooldown > 0) {
            player.sendMessage(ChatColor.RED + "Twoja administracja asymiluje nowy teren. Pozostały czas: "
                    + ChatColor.YELLOW + formatRemainingTime(remainingCooldown));
            return false;
        }

        // 3. Sprawdź ciągłość terytorialną (Zasada braku odizolowanych enklaw)
        if (!hasAdjacentClaim(nation, world.getName(), chunkX, chunkZ)) {
            player.sendMessage(ChatColor.RED + "[ZASADA CIĄGŁOŚCI] Nowy chunk musi bezpośrednio stykać się krawędzią z dotychczasowym terytorium nacji!");
            return false;
        }

        // 4. Oblicz koszt zakupu i sprawdź środki w Skarbcu Narodowym
        double cost = calculateChunkCost(nation);
        if (nation.getBankBalance() < cost) {
            player.sendMessage(ChatColor.RED + "W Skarbcu Narodowym brak środków na zakup chunka!");
            player.sendMessage(ChatColor.GRAY + "Wymagane: " + ChatColor.GOLD + economyManager.format(cost)
                    + ChatColor.GRAY + ", Stan skarbca: " + ChatColor.YELLOW + economyManager.format(nation.getBankBalance()));
            return false;
        }

        // 5. Pobranie opłaty z konta państwa
        nation.withdrawBank(cost);

        // 6. Rejestracja chunka
        NationClaimChunk newClaim = new NationClaimChunk(world.getName(), chunkX, chunkZ, nation.getId());
        nation.addClaimedChunk(newClaim);
        nation.setLastClaimTimestamp(System.currentTimeMillis());

        player.sendMessage(ChatColor.GREEN + "Pomyślnie skolonizowano chunk [" + chunkX + ", " + chunkZ + "]!");
        player.sendMessage(ChatColor.GRAY + "Pobrano ze Skarbca: " + ChatColor.GOLD + economyManager.format(cost)
                + ChatColor.GRAY + " (Nowa liczba chunków: " + nation.getClaimedChunks().size() + ")");

        // 7. Automatyczne przesuwanie muru (jeśli państwo posiada wybudowany mur)
        if (wallManager != null && nation.getWallTier() != WallTier.NONE) {
            wallManager.rebuildNationWalls(nation);
            player.sendMessage(ChatColor.AQUA + "[MURY] Odcinek muru został automatycznie zdemontowany wewnątrz i przesunięty na nową krawędź granicy!");
        }

        // 8. Aktualizacja Map WWW (Squaremap, Dynmap, BlueMap, Pl3xMap)
        if (mapManager != null) {
            mapManager.updateNation(nation);
        }

        nationManager.saveNations();
        return true;
    }

    public boolean unclaimChunk(Player player, Nation nation) {
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        NationClaimChunk target = null;
        for (NationClaimChunk c : nation.getClaimedChunks()) {
            if (c.getWorld().equalsIgnoreCase(world.getName()) && c.getX() == chunkX && c.getZ() == chunkZ) {
                target = c;
                break;
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Ten chunk nie należy do Twojego państwa!");
            return false;
        }

        // Blokada unclaimowania podczas wojny
        if (warManager != null && warManager.isAtWarWithAny(nation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz zrzekać się terytorium podczas trwania stanu wojny!");
            return false;
        }

        // Zakaz unclaimu jeśli państwo ma tylko 1 chunk lub chunk zawiera Bastion
        if (nation.getClaimedChunks().size() <= 1) {
            player.sendMessage(ChatColor.RED + "Nie możesz porzucić jedynego/stolicznego chunka swojego państwa!");
            return false;
        }

        nation.removeClaimedChunk(target);
        player.sendMessage(ChatColor.YELLOW + "Zrzeczono się praw do chunka [" + chunkX + ", " + chunkZ + "]!");

        // Przebudowa murów do nowego obrysu
        if (wallManager != null && nation.getWallTier() != WallTier.NONE) {
            wallManager.rebuildNationWalls(nation);
        }

        // Aktualizacja map WWW
        if (mapManager != null) {
            mapManager.updateNation(nation);
        }

        nationManager.saveNations();
        return true;
    }

    /**
     * Samoczynne unclaimowanie najbardziej oddalonego/skrajnego chunka od Serca Państwa (Bastion Core).
     */
    public NationClaimChunk autoUnclaimOutermost(Nation nation) {
        if (nation.getClaimedChunks().size() <= 1) {
            return null; // Zostawiamy ostatni bastion
        }

        int bastionCx = 0;
        int bastionCz = 0;
        boolean hasBastion = false;

        if (territoryManager != null) {
            TerritoryStructure bastion = territoryManager.getNationBastion(nation.getId());
            if (bastion != null && bastion.getLocation() != null) {
                bastionCx = bastion.getLocation().getBlockX() >> 4;
                bastionCz = bastion.getLocation().getBlockZ() >> 4;
                hasBastion = true;
            }
        }

        NationClaimChunk outermost = null;
        double maxDistanceSq = -1.0;

        for (NationClaimChunk c : nation.getClaimedChunks()) {
            if (hasBastion && c.getX() == bastionCx && c.getZ() == bastionCz) {
                continue; // Nigdy nie unclaimuj chunka ze stolicznym Bastionem dopóki są inne chunki
            }

            double distSq = hasBastion
                    ? (Math.pow(c.getX() - bastionCx, 2) + Math.pow(c.getZ() - bastionCz, 2))
                    : c.getClaimedAt();

            if (distSq > maxDistanceSq) {
                maxDistanceSq = distSq;
                outermost = c;
            }
        }

        if (outermost != null) {
            nation.removeClaimedChunk(outermost);
            if (wallManager != null && nation.getWallTier() != WallTier.NONE) {
                wallManager.rebuildNationWalls(nation);
            }
            if (mapManager != null) {
                mapManager.updateNation(nation);
            }
            nationManager.saveNations();
        }
        return outermost;
    }
}
