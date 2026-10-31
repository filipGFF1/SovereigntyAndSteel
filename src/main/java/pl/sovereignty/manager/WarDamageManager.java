package pl.sovereignty.manager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

public class WarDamageManager {
    private final Plugin plugin;
    private final RestorationManager restorationManager;

    public WarDamageManager(Plugin plugin, RestorationManager restorationManager) {
        this.plugin = plugin;
        this.restorationManager = restorationManager;
    }

    public void damageBlockInWar(Block block) {
        if (block == null || block.getType() == Material.AIR || block.getType() == Material.BEDROCK) {
            return;
        }

        // Zapisz stan w buforze rekonstrukcji
        restorationManager.recordBlockDamage(block);

        // Zniszcz blok
        block.setType(Material.AIR);
    }

    public RestorationManager getRestorationManager() {
        return restorationManager;
    }
}
