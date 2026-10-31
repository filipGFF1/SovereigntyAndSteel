package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.TerritoryStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpkeepManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final VaultEconomyManager economyManager;
    private final ChunkClaimManager chunkClaimManager;

    private TerritoryManager territoryManager;
    private MilitaryManager militaryManager;
    private MapManager mapManager;

    public static final double BASE_UPKEEP_PER_CHUNK = 5.0; // 5 $ za chunk

    public UpkeepManager(Plugin plugin, NationManager nationManager, VaultEconomyManager economyManager, ChunkClaimManager chunkClaimManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.economyManager = economyManager;
        this.chunkClaimManager = chunkClaimManager;
    }

    public void setTerritoryManager(TerritoryManager territoryManager) {
        this.territoryManager = territoryManager;
    }

    public void setMilitaryManager(MilitaryManager militaryManager) {
        this.militaryManager = militaryManager;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    /**
     * Oblicza dzienny koszt utrzymania terytorium państwa (Upkeep).
     */
    public double calculateDailyUpkeep(Nation nation) {
        int chunkCount = nation.getClaimedChunks().size();
        if (chunkCount == 0) return 0.0;

        double base = chunkCount * BASE_UPKEEP_PER_CHUNK;
        double multiplier = nation.getRegime().getUpkeepMultiplier();
        return base * multiplier;
    }

    /**
     * Główny cykl pobierania podatku gruntowego o północy / co 24 godziny w grze.
     */
    public void runDailyUpkeepCycle() {
        List<Nation> allNations = new ArrayList<>(nationManager.getAllNations());

        for (Nation nation : allNations) {
            // 0. Obsługa Wasalizacji i Trybutu (10% dziennego dochodu zwycięzcy przez 7 dni)
            if (nation.getOverlordNationId() != null && nation.getTributeDaysRemaining() > 0) {
                Nation overlord = nationManager.getNation(nation.getOverlordNationId());
                if (overlord != null) {
                    double tribute = nation.getBankBalance() * 0.10;
                    if (tribute > 0) {
                        nation.withdrawBank(tribute);
                        overlord.depositBank(tribute);
                        broadcastToNation(nation, "&e[TRYBUT WASALNY] Przekazano 10% dochodu (&6" + economyManager.format(tribute)
                                + "&e) suzerenowi: &f" + overlord.getName() + " &8(Pozostało dni: " + nation.getTributeDaysRemaining() + ")");
                        broadcastToNation(overlord, "&6[TRYBUT WASALNY] Otrzymano trybut &e" + economyManager.format(tribute)
                                + " &6od wasala: &f" + nation.getName() + "!");
                    }
                }
                nation.setTributeDaysRemaining(nation.getTributeDaysRemaining() - 1);
                if (nation.getTributeDaysRemaining() <= 0) {
                    nation.setOverlordNationId(null);
                    broadcastToNation(nation, "&a[SUZERENITET] Zakończył się 7-dniowy okres płacenia trybutu lennego!");
                }
            }

            if (nation.getClaimedChunks().isEmpty()) continue;

            double upkeep = calculateDailyUpkeep(nation);

            if (nation.getBankBalance() >= upkeep) {
                // Sukces: pobranie opłaty
                nation.withdrawBank(upkeep);
                if (nation.isIndebted()) {
                    nation.setIndebted(false);
                    nation.setDebtDays(0);
                    broadcastToNation(nation, "&a[UPKEEP] Spłacono zaległe opłaty gruntowe! Państwo wyszło ze stanu zadłużenia, produkcja i morale armii przywrócone!");
                } else {
                    broadcastToNation(nation, "&6[UPKEEP] Pobrano opłatę dzienną za terytorium (" + nation.getClaimedChunks().size()
                            + " chunków): &e" + economyManager.format(upkeep) + "&6. Stan konta: &f" + economyManager.format(nation.getBankBalance()));
                }
            } else {
                // Brak środków: wejście w zadłużenie (Decay System)
                nation.setIndebted(true);
                int days = nation.getDebtDays() + 1;
                nation.setDebtDays(days);

                broadcastToNation(nation, "&c&l[UPKEEP] BRAK ŚRODKÓW W SKARBCU NARODOWYM! &e(Wymagane: " + economyManager.format(upkeep) + ")");

                if (days == 1) {
                    // Dzień 1 (Zadłużenie): Wstrzymanie pasywnej i manualnej produkcji
                    broadcastToNation(nation, "&c&l[KRYZYS GOSPODARCZY - DZIEŃ 1] &cPaństwo weszło w stan zadłużenia! Wstrzymano pracę farm, tartaków i kopalń!");
                } else if (days == 2) {
                    // Dzień 2 (Kryzys Wojskowy): Żołnierze NPC i Paolo dezerterują / tracą 50% siły
                    broadcastToNation(nation, "&4&l[KRYZYS WOJSKOWY - DZIEŃ 2] &cMorale armii załamało się! Wojsko cierpi z powodu braku żołdu (osłabienie i dezercje)!");
                    if (militaryManager != null) {
                        militaryManager.applyDebtMilitaryCrisis(nation.getId());
                    }
                } else {
                    // Dzień 3+ (Utrata Terenu - Land Decay)
                    if (nation.getClaimedChunks().size() > 1) {
                        NationClaimChunk lostChunk = chunkClaimManager.autoUnclaimOutermost(nation);
                        if (lostChunk != null) {
                            broadcastToNation(nation, "&4&l[LAND DECAY - DZIEŃ " + days + "] &cZ powodu braku środków skrajny chunk ["
                                    + lostChunk.getX() + ", " + lostChunk.getZ() + "] odpadł od granic państwa!");
                        }
                    } else {
                        // BANKRUCTWO OSTATECZNE: Skurczenie do samego Bastionu i brak spłaty
                        handleFinalBankruptcy(nation);
                    }
                }
            }
        }
        nationManager.saveNations();
    }

    private void handleFinalBankruptcy(Nation nation) {
        String nationName = nation.getName();
        Location bastionLoc = null;

        if (territoryManager != null) {
            TerritoryStructure bastion = territoryManager.getNationBastion(nation.getId());
            if (bastion != null) {
                bastionLoc = bastion.getLocation();
            }
        }

        if (bastionLoc != null && bastionLoc.getWorld() != null) {
            bastionLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, bastionLoc.clone().add(0.5, 0.5, 0.5), 5);
            bastionLoc.getWorld().playSound(bastionLoc, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.7f);
            bastionLoc.getWorld().playSound(bastionLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        }

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4&l==============================================\n"
                        + "&4&l[BANKRUCTWO OSTATECZNE] &cPaństwo &f" + nationName + " &czbankrutowało i nie było w stanie utrzymać stolicy!\n"
                        + "&cSerce Państwa (Rdzeń Bastionu) pękło, a państwo samoczynnie przestało istnieć!\n"
                        + "&4&l==============================================\n"));

        if (territoryManager != null) {
            territoryManager.removeAllStructuresForNation(nation.getId());
        }
        if (mapManager != null) {
            mapManager.removeNation(nation);
        }
        nationManager.disbandNation(nation);
    }

    private void broadcastToNation(Nation nation, String message) {
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (UUID member : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
    }
}
