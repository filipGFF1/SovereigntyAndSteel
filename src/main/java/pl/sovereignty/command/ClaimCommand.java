package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimCommand implements CommandExecutor {
    private final SovereigntyCommand sovereigntyCommand;

    public ClaimCommand(SovereigntyCommand sovereigntyCommand) {
        this.sovereigntyCommand = sovereigntyCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą claimować teren!");
            return true;
        }
        sovereigntyCommand.handleClaim(player);
        return true;
    }
}
