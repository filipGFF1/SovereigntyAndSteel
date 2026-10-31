package pl.sovereignty.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.sovereignty.manager.DiplomacyManager;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.model.DiplomaticRelationType;
import pl.sovereignty.model.Nation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AllyChatCommand implements CommandExecutor {
    private final NationManager nationManager;
    private final DiplomacyManager diplomacyManager;

    public AllyChatCommand(NationManager nationManager, DiplomacyManager diplomacyManager) {
        this.nationManager = nationManager;
        this.diplomacyManager = diplomacyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracz w grze może korzystać z czatu sojuszniczego!");
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
        String formatted = ChatColor.translateAlternateColorCodes('&',
                "&8[&aCZAT SOJUSZU&8] &7[&e" + nation.getTag() + "&7] &a" + player.getName() + "&8: &f" + message);

        Set<UUID> recipients = new HashSet<>(nation.getMembers().keySet());

        // Dołącz członków wszystkich państw będących w sojuszu (ALLY)
        for (Nation other : nationManager.getAllNations()) {
            if (diplomacyManager.areAllies(nation.getId(), other.getId())) {
                recipients.addAll(other.getMembers().keySet());
            }
        }

        for (UUID rUuid : recipients) {
            Player target = Bukkit.getPlayer(rUuid);
            if (target != null && target.isOnline()) {
                target.sendMessage(formatted);
            }
        }

        return true;
    }
}
