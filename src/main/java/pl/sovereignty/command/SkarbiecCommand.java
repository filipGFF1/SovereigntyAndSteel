package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkarbiecCommand implements CommandExecutor, TabCompleter {
    private final SovereigntyCommand sovereigntyCommand;

    public SkarbiecCommand(SovereigntyCommand sovereigntyCommand) {
        this.sovereigntyCommand = sovereigntyCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą korzystać ze skarbca!");
            return true;
        }

        String[] fullArgs;
        if (args.length == 0) {
            fullArgs = new String[]{"skarbiec"};
        } else {
            fullArgs = new String[args.length + 1];
            fullArgs[0] = "skarbiec";
            System.arraycopy(args, 0, fullArgs, 1, args.length);
        }

        sovereigntyCommand.handleSkarbiec(player, fullArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2", "3", "4", "5", "ulepsz", "menu", "autosell", "bank");
        }
        return new ArrayList<>();
    }
}
