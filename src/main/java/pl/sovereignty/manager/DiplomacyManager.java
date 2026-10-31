package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.DiplomaticRelationType;
import pl.sovereignty.model.Nation;

import java.util.*;

public class DiplomacyManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private DatabaseManager databaseManager;

    public static final long NAP_DURATION_MS = 3L * 24L * 3600L * 1000L; // 3 dni
    public static final long TRUCE_DURATION_MS = 48L * 3600L * 1000L; // 48 godzin

    public DiplomacyManager(Plugin plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean areAllies(UUID nationA, UUID nationB) {
        if (nationA == null || nationB == null || nationA.equals(nationB)) return false;
        Nation a = nationManager.getNation(nationA);
        Nation b = nationManager.getNation(nationB);
        return a != null && b != null && a.isAlly(nationB) && b.isAlly(nationA);
    }

    public boolean hasNAP(UUID nationA, UUID nationB) {
        if (nationA == null || nationB == null || nationA.equals(nationB)) return false;
        Nation a = nationManager.getNation(nationA);
        Nation b = nationManager.getNation(nationB);
        return a != null && b != null && (a.hasActiveNAP(nationB) || b.hasActiveNAP(nationA));
    }

    public boolean hasTruce(UUID nationA, UUID nationB) {
        if (nationA == null || nationB == null || nationA.equals(nationB)) return false;
        Nation a = nationManager.getNation(nationA);
        Nation b = nationManager.getNation(nationB);
        return a != null && b != null && (a.hasActiveTruce(nationB) || b.hasActiveTruce(nationA));
    }

    public boolean isRival(UUID nationA, UUID targetNationB) {
        if (nationA == null || targetNationB == null || nationA.equals(targetNationB)) return false;
        Nation a = nationManager.getNation(nationA);
        return a != null && a.isRival(targetNationB);
    }

    public void handleAllyCommand(Player player, Nation senderNation, Nation targetNation) {
        if (senderNation.getId().equals(targetNation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz zawrzeć sojuszu z samym sobą!");
            return;
        }

        if (areAllies(senderNation.getId(), targetNation.getId())) {
            player.sendMessage(ChatColor.YELLOW + "Wasze państwa są już w oficjalnym sojuszu!");
            return;
        }

        // Sprawdź czy drugie państwo już złożyło ofertę
        DiplomaticRelationType incoming = senderNation.getPendingRelationOffers().get(targetNation.getId());
        if (incoming == DiplomaticRelationType.ALLY) {
            // Obustronna zgoda!
            senderNation.setRelation(targetNation.getId(), DiplomaticRelationType.ALLY, 0);
            targetNation.setRelation(senderNation.getId(), DiplomaticRelationType.ALLY, 0);

            if (databaseManager != null) {
                databaseManager.saveRelationAsync(senderNation.getId(), targetNation.getId(), DiplomaticRelationType.ALLY, -1L);
                databaseManager.saveRelationAsync(targetNation.getId(), senderNation.getId(), DiplomaticRelationType.ALLY, -1L);
            }

            broadcastToNation(senderNation, "&a&l[DYPLOMACJA] &fZawarto oficjalny &aSOJUSZ &fz państwem &e" + targetNation.getName() + "&f!");
            broadcastToNation(targetNation, "&a&l[DYPLOMACJA] &fZawarto oficjalny &aSOJUSZ &fz państwem &e" + senderNation.getName() + "&f!");
            nationManager.saveNations();
            return;
        }

        // Złożenie oferty
        targetNation.getPendingRelationOffers().put(senderNation.getId(), DiplomaticRelationType.ALLY);
        player.sendMessage(ChatColor.GREEN + "Wysłano propozycję sojuszu do państwa " + targetNation.getName() + ".");
        broadcastToNation(targetNation, "&6&l[DYPLOMACJA] &ePaństwo &f" + senderNation.getName() + " &ezaproponowało &aSOJUSZ&e! Wpisz &f/panstwo relacja sojusz " + senderNation.getName() + "&e, aby zaakceptować.");
    }

    public void handleNAPCommand(Player player, Nation senderNation, Nation targetNation) {
        if (senderNation.getId().equals(targetNation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz podpisać paktu ze sobą!");
            return;
        }

        if (hasNAP(senderNation.getId(), targetNation.getId())) {
            player.sendMessage(ChatColor.YELLOW + "Wasze państwa mają już aktywny Pakt o Nieagresji!");
            return;
        }

        DiplomaticRelationType incoming = senderNation.getPendingRelationOffers().get(targetNation.getId());
        if (incoming == DiplomaticRelationType.NAP) {
            senderNation.setRelation(targetNation.getId(), DiplomaticRelationType.NAP, NAP_DURATION_MS);
            targetNation.setRelation(senderNation.getId(), DiplomaticRelationType.NAP, NAP_DURATION_MS);

            long expiry = System.currentTimeMillis() + NAP_DURATION_MS;
            if (databaseManager != null) {
                databaseManager.saveRelationAsync(senderNation.getId(), targetNation.getId(), DiplomaticRelationType.NAP, expiry);
                databaseManager.saveRelationAsync(targetNation.getId(), senderNation.getId(), DiplomaticRelationType.NAP, expiry);
            }

            broadcastToNation(senderNation, "&e&l[DYPLOMACJA] &fPodpisano &ePAKT O NIEAGRESJI &f(3 dni) z państwem &6" + targetNation.getName() + "&f!");
            broadcastToNation(targetNation, "&e&l[DYPLOMACJA] &fPodpisano &ePAKT O NIEAGRESJI &f(3 dni) z państwem &6" + senderNation.getName() + "&f!");
            nationManager.saveNations();
            return;
        }

        targetNation.getPendingRelationOffers().put(senderNation.getId(), DiplomaticRelationType.NAP);
        player.sendMessage(ChatColor.GREEN + "Wysłano ofertę Paktu o Nieagresji do państwa " + targetNation.getName() + ".");
        broadcastToNation(targetNation, "&6&l[DYPLOMACJA] &ePaństwo &f" + senderNation.getName() + " &ezaproponowało &ePAKT O NIEAGRESJI&e! Wpisz &f/panstwo relacja nap " + senderNation.getName() + "&e, aby zaakceptować.");
    }

    public void handleRivalCommand(Player player, Nation senderNation, Nation targetNation) {
        if (senderNation.getId().equals(targetNation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz uznać siebie za rywala!");
            return;
        }

        senderNation.setRelation(targetNation.getId(), DiplomaticRelationType.RIVAL, 0);
        if (databaseManager != null) {
            databaseManager.saveRelationAsync(senderNation.getId(), targetNation.getId(), DiplomaticRelationType.RIVAL, -1L);
        }

        broadcastToNation(senderNation, "&c&l[DYPLOMACJA] &fPaństwo &e" + targetNation.getName() + " &czostało oficjalnie ogłoszone &4RYWALEM / WROGIEM&c! (Bonusy z PKB za walkę, tańsza wojna).");
        broadcastToNation(targetNation, "&4&l[DYPLOMACJA] &cPaństwo &e" + senderNation.getName() + " &cogłosiło Was swoim oficjalnym Rywalem!");
        nationManager.saveNations();
    }

    public void handleNeutralCommand(Player player, Nation senderNation, Nation targetNation) {
        if (senderNation.getId().equals(targetNation.getId())) return;

        senderNation.setRelation(targetNation.getId(), DiplomaticRelationType.NEUTRAL, 0);
        targetNation.setRelation(senderNation.getId(), DiplomaticRelationType.NEUTRAL, 0);

        if (databaseManager != null) {
            databaseManager.deleteRelationAsync(senderNation.getId(), targetNation.getId());
        }

        broadcastToNation(senderNation, "&7&l[DYPLOMACJA] &fRelacje z państwem &e" + targetNation.getName() + " &fzostały zresetowane do &7NEUTRALNYCH&f.");
        broadcastToNation(targetNation, "&7&l[DYPLOMACJA] &fPaństwo &e" + senderNation.getName() + " &fzerwało szczególne stosunki (stan: &7NEUTRALNOŚĆ&f).");
        nationManager.saveNations();
    }

    public void enforceTruce(Nation nationA, Nation nationB, long durationMs) {
        if (nationA == null || nationB == null) return;
        nationA.setRelation(nationB.getId(), DiplomaticRelationType.TRUCE, durationMs);
        nationB.setRelation(nationA.getId(), DiplomaticRelationType.TRUCE, durationMs);

        long exp = System.currentTimeMillis() + durationMs;
        if (databaseManager != null) {
            databaseManager.saveRelationAsync(nationA.getId(), nationB.getId(), DiplomaticRelationType.TRUCE, exp);
            databaseManager.saveRelationAsync(nationB.getId(), nationA.getId(), DiplomaticRelationType.TRUCE, exp);
        }

        broadcastToNation(nationA, "&b&l[ROZEJM] &fWymuszony rozejm ochronny z &e" + nationB.getName() + " &fobowiązuje przez 48 godzin!");
        broadcastToNation(nationB, "&b&l[ROZEJM] &fWymuszony rozejm ochronny z &e" + nationA.getName() + " &fobowiązuje przez 48 godzin!");
        nationManager.saveNations();
    }

    private void broadcastToNation(Nation nation, String message) {
        if (nation == null) return;
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (UUID memberUuid : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
    }
}
