package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TerritoryCommand implements CommandExecutor {
    private final SovereigntyCommand sovereigntyCommand;

    public TerritoryCommand(SovereigntyCommand sovereigntyCommand) {
        this.sovereigntyCommand = sovereigntyCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą sprawdzać terytorium!");
            return true;
        }
        sovereigntyCommand.handleTerritory(player);
        return true;
    }
}
