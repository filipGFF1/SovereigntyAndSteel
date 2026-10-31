package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationRole;

public class ChatListener implements Listener {
    private final NationManager nationManager;

    public ChatListener(NationManager nationManager) {
        this.nationManager = nationManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());

        if (nation != null) {
            NationRole role = nation.getRole(player.getUniqueId());
            String roleName = role != null ? role.getDisplayName() : "Obywatel";
            String tag = nation.getTag();

            // Format: [TAG] [Rola] Nick: Wiadomość
            String format = ChatColor.translateAlternateColorCodes('&',
                    "&8[&b" + tag + "&8] &7[" + roleName + "&7] &f%1$s&8: &f%2$s");
            event.setFormat(format);
        } else {
            String format = ChatColor.translateAlternateColorCodes('&',
                    "&8[Koczownik] &7%1$s&8: &f%2$s");
            event.setFormat(format);
        }
    }
}
