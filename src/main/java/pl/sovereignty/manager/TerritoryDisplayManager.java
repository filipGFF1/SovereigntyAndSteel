package pl.sovereignty.manager;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryDisplayManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final Map<UUID, UUID> playerCurrentTerritory = new ConcurrentHashMap<>();

    public TerritoryDisplayManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
    }

    /**
     * Weryfikuje czy gracz przekroczył granicę terytorialną (własne państwo, obce państwo lub Dzicz).
     * Wyświetla animowany Title na środku ekranu, dźwięk przekroczenia granicy oraz komunikat Action Bar.
     */
    public void checkPlayerTerritoryChange(Player player, Location to) {
        if (player == null || to == null) return;

        Nation currentNation = territoryManager.getNationAt(to);
        UUID currentNationId = (currentNation != null) ? currentNation.getId() : null;
        UUID previousNationId = playerCurrentTerritory.get(player.getUniqueId());

        boolean isInitial = !playerCurrentTerritory.containsKey(player.getUniqueId());

        if (isInitial || !Objects.equals(currentNationId, previousNationId)) {
            playerCurrentTerritory.put(player.getUniqueId(), currentNationId);
            notifyBorderCrossing(player, currentNation, isInitial);
        }
    }

    private void notifyBorderCrossing(Player player, Nation nation, boolean isInitial) {
        Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());

        if (nation == null) {
            // 1. Wejście w Dzicz (Ziemia Niczyja)
            String title = ChatColor.GRAY + "" + ChatColor.BOLD + "DZICZ";
            String subtitle = ChatColor.DARK_GRAY + "Ziemia Niczyja " + ChatColor.GRAY + "• Poza granicami państw";
            player.sendTitle(title, subtitle, 10, 45, 15);
            sendActionBar(player, "&8🏕 Wkroczono w strefę: &7Dzicz (Ziemia Niczyja)");

            if (!isInitial) {
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.7f, 0.8f);
            }
        } else if (playerNation != null && playerNation.getId().equals(nation.getId())) {
            // 2. Wejście na terytorium własnego państwa (Ojczyzna)
            String title = ChatColor.GREEN + "" + ChatColor.BOLD + "OJCZYZNA";
            String subtitle = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + nation.getName()
                    + " " + ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + nation.getTag() + ChatColor.DARK_GRAY + "]"
                    + ChatColor.GRAY + " • Witaj w domu!";
            player.sendTitle(title, subtitle, 10, 50, 15);
            sendActionBar(player, "&a✦ Wkroczono na terytorium: &2&l" + nation.getName() + " &8[&a" + nation.getTag() + "&8]");

            if (!isInitial) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
            }
        } else {
            // 3. Wejście na terytorium obcego państwa
            String title = ChatColor.RED + "" + ChatColor.BOLD + "OBCE TERYTORIUM";
            String subtitle = ChatColor.DARK_RED + "" + ChatColor.BOLD + nation.getName()
                    + " " + ChatColor.DARK_GRAY + "[" + ChatColor.RED + nation.getTag() + ChatColor.DARK_GRAY + "]"
                    + ChatColor.GRAY + " • Zachowaj ostrożność!";
            player.sendTitle(title, subtitle, 10, 50, 15);
            sendActionBar(player, "&c⚔ Wkroczono na terytorium: &4&l" + nation.getName() + " &8[&c" + nation.getTag() + "&8]");

            if (!isInitial) {
                player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.6f, 0.9f);
            }

            // ENTER_MESSAGE: Powiadomienie na Action Bar dla właścicieli o wejściu intruza
            for (UUID memberUuid : nation.getMembers().keySet()) {
                Player mPlayer = org.bukkit.Bukkit.getPlayer(memberUuid);
                if (mPlayer != null && mPlayer.isOnline()) {
                    sendActionBar(mPlayer, "&c&l[INTRUZ] &eGracz &f" + player.getName() + " &ewkroczył na terytorium Twojego państwa!");
                }
            }
        }
    }

    /**
     * Ciągła aktualizacja paska Action Bar dla gracza (co 2 sekundy).
     */
    public void updateActionBarForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        Nation currentNation = territoryManager.getNationAt(player.getLocation());
        Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());

        if (currentNation == null) {
            sendActionBar(player, "&8🏕 Obszar: &7Dzicz (Ziemia Niczyja)");
        } else if (playerNation != null && playerNation.getId().equals(currentNation.getId())) {
            sendActionBar(player, "&a✦ Obszar: &2&l" + currentNation.getName() + " &8[&a" + currentNation.getTag() + "&8] &7(Twoje Państwo)");
        } else {
            sendActionBar(player, "&c⚔ Obszar: &4&l" + currentNation.getName() + " &8[&c" + currentNation.getTag() + "&8] &7(Obce Państwo)");
        }
    }

    public void sendActionBar(Player player, String message) {
        try {
            String formatted = ChatColor.translateAlternateColorCodes('&', message);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
        } catch (Throwable ignored) {}
    }

    public void removePlayer(UUID uuid) {
        playerCurrentTerritory.remove(uuid);
    }
}
