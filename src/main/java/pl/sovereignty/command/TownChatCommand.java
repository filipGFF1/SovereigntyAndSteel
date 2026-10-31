package pl.sovereignty.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationRole;

import java.util.UUID;

public class TownChatCommand implements CommandExecutor {
    private final NationManager nationManager;

    public TownChatCommand(NationManager nationManager) {
        this.nationManager = nationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracz w grze może korzystać z czatu państwa!");
            return true;
        }

        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnego państwa!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Użycie: /" + label + " <wiadomość>");
            return true;
        }

        String message = String.join(" ", args);
        NationRole role = nation.getRole(player.getUniqueId());
        String roleName = role != null ? role.getDisplayName() : "Obywatel";

        String formatted = ChatColor.translateAlternateColorCodes('&',
                "&8[&6CZAT PAŃSTWA&8] &7[" + roleName + "&7] &f" + player.getName() + "&8: &e" + message);

        for (UUID memberUuid : nation.getMembers().keySet()) {
            Player target = Bukkit.getPlayer(memberUuid);
            if (target != null && target.isOnline()) {
                target.sendMessage(formatted);
            }
        }
        return true;
    }
}
