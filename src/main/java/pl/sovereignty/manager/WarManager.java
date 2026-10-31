package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.NationRole;
import pl.sovereignty.model.WarState;

import java.util.*;

public class WarManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final RestorationManager restorationManager;
    private final List<WarState> activeWars = new ArrayList<>();

    private DiplomacyManager diplomacyManager;
    private ChunkClaimManager chunkClaimManager;
    private MapManager mapManager;

    public WarManager(Plugin plugin, NationManager nationManager, RestorationManager restorationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.restorationManager = restorationManager;
    }

    public void setDiplomacyManager(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;
    }

    public void setChunkClaimManager(ChunkClaimManager chunkClaimManager) {
        this.chunkClaimManager = chunkClaimManager;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    /**
     * Sprawdza warunki i wypowiada wojnę.
     * Zwraca null w przypadku sukcesu lub komunikat błędu dla gracza.
     */
    public String canDeclareWar(Nation attacker, Nation defender) {
        if (attacker.getId().equals(defender.getId())) {
            return "Nie możesz wypowiedzieć wojny własnemu państwu!";
        }
        if (isAtWar(attacker.getId(), defender.getId())) {
            return "Wasze państwa są już w stanie wojny!";
        }

        // 1. Sprawdź relacje dyplomatyczne (Pakt o Nieagresji oraz Rozejm)
        if (diplomacyManager != null) {
            if (diplomacyManager.hasNAP(attacker.getId(), defender.getId())) {
                return "Nie możesz zaatakować tego państwa! Obowiązuje Pakt o Nieagresji (NAP).";
            }
            if (diplomacyManager.hasTruce(attacker.getId(), defender.getId())) {
                return "Nie możesz zaatakować tego państwa! Trwa 48-godzinny okres ochronnego Rozejmu (TRUCE).";
            }
        }

        // 2. Tarcza Początkowa (Peace Shield) - ochrona nowych państw przez 48h
        if (defender.isPeaceShieldActive()) {
            double leftH = defender.getPeaceShieldRemainingHours();
            return "Państwo " + defender.getName() + " posiada aktywną Tarczę Początkową (ochrona nowych nacji). Pozostało: " + String.format("%.1f", leftH) + "h.";
        }

        // 3. Wymóg Aktywności Obrońców (Ochrona przed rajdami offline)
        // Wojnę można rozpocząć tylko, gdy online jest min. 2 graczy lub Lider/Oficer
        int onlineCount = 0;
        boolean leaderOrOfficerOnline = false;
        for (Map.Entry<UUID, NationRole> entry : defender.getMembers().entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                onlineCount++;
                if (entry.getValue() == NationRole.LEADER || entry.getValue() == NationRole.OFFICER) {
                    leaderOrOfficerOnline = true;
                }
            }
        }

        if (onlineCount < 2 && !leaderOrOfficerOnline) {
            return "Nie możesz wypowiedzieć wojny! Obrońcy nie spełniają wymogu aktywności (wymagane min. 2 graczy lub Lider/Oficer online).";
        }

        return null; // Sukces
    }

    public boolean declareWar(Nation attacker, Nation defender) {
        String error = canDeclareWar(attacker, defender);
        if (error != null) return false;

        // Agresor traci własną Tarczę Początkową w momencie ataku
        attacker.incrementDeclaredWars();

        WarState war = new WarState(attacker.getId(), defender.getId());
        activeWars.add(war);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4&l[WYPOWIEDZENIE WOJNY] &cPaństwo &f" + attacker.getName() + " &cwypowiedziało wojnę państwu &f"
                        + defender.getName() + "&c!\n&eRozpoczęła się faza mobilizacji i przygotowań do oblężenia (5 minut)!\n"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.5f, 0.8f);
        }

        // Zaplanuj start fazy oblężenia za 5 minut (6000 ticków)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!war.isConcluded()) {
                war.setSiegeActive(true);
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        "\n&4&l[FAZA OBLĘŻENIA] &cWojna pomiędzy &f" + attacker.getName() + " &ca &f"
                                + defender.getName() + " &cprzeszła w fazę aktywnego szturmu!\n"
                                + "&eMachiny oblężnicze i pociski mogą teraz kruszyć mury i wrota twierdzy!\n"));
            }
        }, 6000L);

        nationManager.saveNations();
        return true;
    }

    public boolean isAtWar(UUID nationA, UUID nationB) {
        for (WarState w : activeWars) {
            if (!w.isConcluded() && w.isParticipant(nationA) && w.isParticipant(nationB)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtWarWithAny(UUID nationId) {
        for (WarState w : activeWars) {
            if (!w.isConcluded() && w.isParticipant(nationId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSiegeActive(UUID nationA, UUID nationB) {
        for (WarState w : activeWars) {
            if (!w.isConcluded() && w.isSiegeActive() && w.isParticipant(nationA) && w.isParticipant(nationB)) {
                return true;
            }
        }
        return false;
    }

    public void endWar(WarState war, UUID winnerNationId) {
        war.setConcluded(true);
        activeWars.remove(war);

        Nation winner = nationManager.getNation(winnerNationId);
        Nation loser = nationManager.getNation(war.getAttackerNationId().equals(winnerNationId) ? war.getDefenderNationId() : war.getAttackerNationId());

        if (winner != null && loser != null) {
            winner.addMilitaryScore(50);

            // Wymuś 48h Rozejmu po zakończeniu wojny
            if (diplomacyManager != null) {
                diplomacyManager.enforceTruce(winner, loser, DiplomacyManager.TRUCE_DURATION_MS);
            }

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l[KONIEC WOJNY] &eZwycięstwo nacji &f" + winner.getName() + "&e! Państwo &7"
                            + loser.getName() + " &euznało wyższość rywala!\n"));
        }

        restorationManager.startGradualRestoration();
        nationManager.saveNations();
    }

    /**
     * Alternatywne zakończenie wojny: Kapitulacja i Reparacje
     * - Przejęcie do 25% terytorium pokonanego (chunki graniczne)
     * - Jednorazowa kontrybucja 70% zasobów z konta
     * - Status wasala / trybutariusza (10% dziennego dochodu przez 7 dni)
     * - 48h rozejm
     */
    public boolean handleCapitulation(Player player, Nation surrenderingNation) {
        WarState activeWar = null;
        for (WarState w : activeWars) {
            if (!w.isConcluded() && w.isParticipant(surrenderingNation.getId())) {
                activeWar = w;
                break;
            }
        }

        if (activeWar == null) {
            player.sendMessage(ChatColor.RED + "Twoje państwo nie bierze obecnie udziału w żadnej aktywnej wojnie!");
            return false;
        }

        UUID winnerId = activeWar.getAttackerNationId().equals(surrenderingNation.getId())
                ? activeWar.getDefenderNationId()
                : activeWar.getAttackerNationId();
        Nation winnerNation = nationManager.getNation(winnerId);

        if (winnerNation == null) {
            player.sendMessage(ChatColor.RED + "Błąd: nie odnaleziono przeciwnej nacji.");
            return false;
        }

        // 1. Przejęcie do 25% terytorium pokonanego (chunki skrajne)
        int initialChunks = surrenderingNation.getClaimedChunks().size();
        int chunksToCede = Math.min(initialChunks - 1, (int) Math.ceil(initialChunks * 0.25));
        int cededCount = 0;

        if (chunkClaimManager != null && chunksToCede > 0) {
            for (int i = 0; i < chunksToCede; i++) {
                NationClaimChunk lost = chunkClaimManager.autoUnclaimOutermost(surrenderingNation);
                if (lost != null) {
                    // Przekaż chunk zwycięzcy
                    NationClaimChunk gained = new NationClaimChunk(lost.getWorld(), lost.getX(), lost.getZ(), winnerNation.getId(), System.currentTimeMillis());
                    winnerNation.addClaimedChunk(gained);
                    cededCount++;
                }
            }
        }

        // 2. Kontrybucja ze skarbca (70% stanu konta w Vault)
        double reparations = surrenderingNation.getBankBalance() * 0.70;
        if (reparations > 0) {
            surrenderingNation.withdrawBank(reparations);
            winnerNation.depositBank(reparations);
        }

        // 3. Status Wasala / Trybutariusza (10% dochodu przez 7 dni)
        surrenderingNation.setOverlordNationId(winnerNation.getId());
        surrenderingNation.setTributeDaysRemaining(7);

        // 4. Zakończ wojnę i nałóż 48h TRUCE
        activeWar.setConcluded(true);
        activeWars.remove(activeWar);

        if (diplomacyManager != null) {
            diplomacyManager.enforceTruce(surrenderingNation, winnerNation, DiplomacyManager.TRUCE_DURATION_MS);
        }

        if (mapManager != null) {
            mapManager.updateNation(surrenderingNation);
            mapManager.updateNation(winnerNation);
        }

        restorationManager.startGradualRestoration();
        nationManager.saveNations();

        // 5. Globalny komunikat
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&e&l==============================================\n"
                        + "&e&l[KAPITULACJA I POKÓJ] &cPaństwo &f" + surrenderingNation.getName() + " &cpodpisało akt kapitulacji przed &f" + winnerNation.getName() + "&c!\n"
                        + "&6Warunki traktatu pokojowego:\n"
                        + "&7- Przekazanie &e" + cededCount + " &7chunków granicznych zwycięzcy\n"
                        + "&7- Kontrybucja wojenna: &6" + String.format("%.2f", reparations) + " $\n"
                        + "&7- Status trybutariusza na okres &e7 dni &7(10% dziennego dochodu)\n"
                        + "&b- Wymuszony Rozejm (TRUCE) na okres 48 godzin!\n"
                        + "&e&l==============================================\n"));

        return true;
    }

    public WarState getActiveWar(UUID nationA, UUID nationB) {
        for (WarState w : activeWars) {
            if (!w.isConcluded() && w.isParticipant(nationA) && w.isParticipant(nationB)) {
                return w;
            }
        }
        return null;
    }

    public void concludeWarsForNation(UUID nationId) {
        List<WarState> toRemove = new ArrayList<>();
        for (WarState w : activeWars) {
            if (w.isParticipant(nationId)) {
                w.setConcluded(true);
                toRemove.add(w);
            }
        }
        activeWars.removeAll(toRemove);
        if (!toRemove.isEmpty()) {
            restorationManager.startGradualRestoration();
        }
    }

    public List<WarState> getActiveWars() {
        return activeWars;
    }
}
