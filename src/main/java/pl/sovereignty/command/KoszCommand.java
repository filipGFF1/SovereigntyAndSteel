package pl.sovereignty.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.sovereignty.gui.TrashHolder;

public class KoszCommand implements CommandExecutor {
    public static final String TITLE = "§4§lKosz na Śmieci §8(Wyrzuć)";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą korzystać z kosza na śmieci!");
            return true;
        }

        openTrash(player);
        return true;
    }

    public static void openTrash(Player player) {
        TrashHolder holder = new TrashHolder();
        var inv = Bukkit.createInventory(holder, 36, TITLE);
        holder.setInventory(inv);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&cKosz&8] &7Wszystkie przedmioty włożone do tego okna zostaną &cbezpowrotnie usunięte &7po zamknięciu!"));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
        player.openInventory(inv);
    }
}
