package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.sovereignty.manager.SeatManager;

public class SitCommand implements CommandExecutor {
    private final SeatManager seatManager;

    public SitCommand(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą korzystać z komendy /sit!");
            return true;
        }

        handleSit(player, seatManager);
        return true;
    }

    public static void handleSit(Player player, SeatManager seatManager) {
        if (seatManager.isSitting(player) || player.isInsideVehicle()) {
            seatManager.unsit(player, true);
            return;
        }

        if (player.isGliding() || player.isSwimming() || player.isDead() || player.isFlying()) {
            player.sendMessage(ChatColor.RED + "Możesz usiąść tylko stojąc stabilnie na podłożu!");
            return;
        }

        // Siadanie na ziemi w bieżącej pozycji gracza (offset -0.70 dla małego stojaka)
        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() - 0.70);

        seatManager.sit(player, loc, null);
    }
}
