package pl.sovereignty.manager;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagManager implements Listener {
    public static final long COMBAT_DURATION_MS = 15_000L; // 15 sekund

    private final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

    public void tagPlayer(UUID playerUuid) {
        combatExpiry.put(playerUuid, System.currentTimeMillis() + COMBAT_DURATION_MS);
    }

    public boolean isInCombat(UUID playerUuid) {
        Long exp = combatExpiry.get(playerUuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            combatExpiry.remove(playerUuid);
            return false;
        }
        return true;
    }

    public long getRemainingCombatSeconds(UUID playerUuid) {
        Long exp = combatExpiry.get(playerUuid);
        if (exp == null) return 0;
        long rem = exp - System.currentTimeMillis();
        return Math.max(0, rem / 1000 + 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            tagPlayer(victim.getUniqueId());
            tagPlayer(attacker.getUniqueId());
        }
    }
}
