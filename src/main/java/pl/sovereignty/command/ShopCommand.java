package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.sovereignty.gui.ShopGui;
import pl.sovereignty.manager.VaultEconomyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final VaultEconomyManager economyManager;

    public ShopCommand(VaultEconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą korzystać ze sklepu!");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            String cat = switch (sub) {
                case "budownictwo", "bloki", "building" -> "building";
                case "rolnictwo", "jedzenie", "farming" -> "farming";
                case "gornictwo", "surowce", "mining" -> "mining";
                case "militaria", "wojsko", "military" -> "military";
                case "technologia", "narzedzia", "sovereignty" -> "sovereignty";
                default -> null;
            };

            if (cat != null) {
                ShopGui.openCategory(player, cat, economyManager);
                return true;
            }
        }

        ShopGui.openMainMenu(player, economyManager);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("budownictwo", "rolnictwo", "gornictwo", "militaria", "technologia");
        }
        return new ArrayList<>();
    }
}
