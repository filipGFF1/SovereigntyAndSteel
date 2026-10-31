package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class BankCommand implements CommandExecutor, TabCompleter {
    private final SovereigntyCommand sovereigntyCommand;

    public BankCommand(SovereigntyCommand sovereigntyCommand) {
        this.sovereigntyCommand = sovereigntyCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą korzystać z banku!");
            return true;
        }

        if (args.length == 0) {
            sovereigntyCommand.handleBank(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("wplac")) {
            sovereigntyCommand.handleDeposit(player, args);
        } else if (sub.equals("wyplac")) {
            sovereigntyCommand.handleWithdraw(player, args);
        } else {
            sovereigntyCommand.handleBank(player);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wplac", "wyplac");
        }
        if (args.length == 2) {
            return Arrays.asList("100", "500", "1000", "5000");
        }
        return null;
    }
}
