package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import pl.sovereignty.model.RestorableBlock;
import pl.sovereignty.util.ParticleUtil;

import java.util.ArrayDeque;
import java.util.Deque;

public class RestorationManager {
    private final Plugin plugin;
    private final Deque<RestorableBlock> rollbackBuffer = new ArrayDeque<>();
    private BukkitTask restorationTask = null;

    public RestorationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void recordBlockDamage(Block block) {
        if (block.getType() == Material.AIR) return;
        rollbackBuffer.push(new RestorableBlock(block.getLocation(), block.getType(), block.getBlockData()));
    }

    public synchronized int getPendingRestorationsCount() {
        return rollbackBuffer.size();
    }

    /**
     * Uruchamia samoczynną rekonstrukcję terenu:
     * Plugin powoli odbudowuje zniszczone bloki z animacją pyłu i cząsteczek.
     */
    public synchronized void startGradualRestoration() {
        if (restorationTask != null && !restorationTask.isCancelled()) {
            return;
        }

        restorationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            synchronized (this) {
                if (rollbackBuffer.isEmpty()) {
                    restorationTask.cancel();
                    restorationTask = null;
                    Bukkit.broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&a&l[SAMOCZYNNA REGENERACJA] &7Wszystkie zniszczenia wojenne na wyspie zostały w pełni odbudowane!"));
                    return;
                }

                // Odbudowuj do 4 bloków na tick (80 bloków na sekundę)
                for (int i = 0; i < 4 && !rollbackBuffer.isEmpty(); i++) {
                    RestorableBlock entry = rollbackBuffer.pop();
                    Location loc = entry.getLocation();
                    Block block = loc.getBlock();

                    block.setType(entry.getOriginalMaterial(), false);
                    block.setBlockData(entry.getOriginalBlockData(), false);

                    ParticleUtil.spawnRestorationEffect(loc);
                }
            }
        }, 20L, 2L);
    }
}
