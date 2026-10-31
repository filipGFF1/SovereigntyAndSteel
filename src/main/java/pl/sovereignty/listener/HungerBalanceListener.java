package pl.sovereignty.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import pl.sovereignty.SovereigntyPlugin;

/**
 * Balansuje zużycie głodu na serwerze.
 * W nowoczesnym Minecrafcie (1.11 - 1.21) sprint i skakanie zużywają znikome ilości głodu,
 * co sprawia, że jedzenie traci na znaczeniu. Ten listener przywraca dynamiczny survival.
 */
public class HungerBalanceListener implements Listener {

    private final SovereigntyPlugin plugin;

    public HungerBalanceListener(SovereigntyPlugin plugin) {
        this.plugin = plugin;
        startPassiveHungerTask();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExhaustion(EntityExhaustionEvent event) {
        if (!plugin.getConfig().getBoolean("hunger.faster_hunger", true)) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                return;
            }

            // Ignorujemy regenerację zdrowia (REGEN), bo MC już domyślnie zabiera ogromne 6.0 wyczerpania za 1 HP
            if (event.getExhaustionReason() == EntityExhaustionEvent.ExhaustionReason.REGEN) {
                return;
            }

            double multiplier = plugin.getConfig().getDouble("hunger.exhaustion_multiplier", 2.5);
            if (multiplier > 1.0) {
                event.setExhaustion((float) (event.getExhaustion() * multiplier));
            }
        }
    }

    private void startPassiveHungerTask() {
        int intervalSeconds = plugin.getConfig().getInt("hunger.passive_drain_seconds", 90);
        if (intervalSeconds <= 0) return;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("hunger.faster_hunger", true)) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    if (player.getFoodLevel() > 0) {
                        // Dodajemy 1.2 wyczerpania (przy 4.0 wyczerpania spada 1 punkt nasycenia lub głodu)
                        player.setExhaustion(player.getExhaustion() + 1.2f);
                    }
                }
            }
        }, 20L * intervalSeconds, 20L * intervalSeconds);
    }
}
