package pl.sovereignty.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class MobDropBalanceListener implements Listener {
    private final Random random = new Random();

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        EntityType type = event.getEntityType();

        // 1. Wither Skeleton - czaszka zbalansowana na 1 na 100 (1% szansy)
        if (type == EntityType.WITHER_SKELETON) {
            // Usuń domyślny drop czaszki
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.WITHER_SKELETON_SKULL);

            // Dokładnie 1 na 100 (1% szansy)
            if (random.nextInt(100) == 0) {
                event.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
            }
        }

        // 2. Blaze - nerf dropu prętów (rod) o 2.5x (zamiast 50% jest 20%)
        else if (type == EntityType.BLAZE) {
            // Usuń domyślne pręty i ewentualny proszek
            event.getDrops().removeIf(item -> item != null && (item.getType() == Material.BLAZE_ROD || item.getType() == Material.BLAZE_POWDER));

            Player killer = event.getEntity().getKiller();
            int lootingLevel = 0;
            if (killer != null && killer.getInventory().getItemInMainHand() != null) {
                try {
                    lootingLevel = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
                } catch (Exception ignored) {}
            }

            // Bazowa szansa znerfiona 2.5x (50% / 2.5 = 20%). Looting dodaje +4% za poziom (max 32% przy Looting III)
            int chance = 20 + (lootingLevel * 4);
            if (random.nextInt(100) < chance) {
                event.getDrops().add(new ItemStack(Material.BLAZE_ROD, 1));
            }
        }
    }

    /**
     * Nerf wytwarzania Blaze Powder z Blaze Rod:
     * Daje 1 proszek zamiast 2 z jednego pręta (nerf 2x z craftingu, łącznie z rod daje 5x rzadszy proszek).
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.BLAZE_POWDER) {
            event.getInventory().setResult(new ItemStack(Material.BLAZE_POWDER, 1));
        }
    }
}
