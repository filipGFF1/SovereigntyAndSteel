package pl.sovereignty.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.sovereignty.manager.TerritoryDisplayManager;

public class TerritoryDisplayListener implements Listener {
    private final TerritoryDisplayManager displayManager;

    public TerritoryDisplayListener(TerritoryDisplayManager displayManager) {
        this.displayManager = displayManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignorujemy obroty głowy (pitch/yaw) oraz skoki/schodki jeśli koordynaty X i Z się nie zmieniły
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        displayManager.checkPlayerTerritoryChange(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            displayManager.checkPlayerTerritoryChange(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        displayManager.checkPlayerTerritoryChange(player, player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnLocation() != null) {
            displayManager.checkPlayerTerritoryChange(event.getPlayer(), event.getRespawnLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        displayManager.removePlayer(event.getPlayer().getUniqueId());
    }
}
