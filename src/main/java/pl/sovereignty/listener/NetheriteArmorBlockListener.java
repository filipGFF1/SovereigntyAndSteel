package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class NetheriteArmorBlockListener implements Listener {

    /**
     * Sprawdza, czy przedmiot to netherytowa zbroja.
     * Narzędzia (miecz, kilof, siekiera, łopata, motyka) oraz sztabki i ulepszenia POZOSTAJĄ DOZWOLONE.
     */
    public static boolean isNetheriteArmor(Material mat) {
        if (mat == null) return false;
        return mat == Material.NETHERITE_HELMET
                || mat == Material.NETHERITE_CHESTPLATE
                || mat == Material.NETHERITE_LEGGINGS
                || mat == Material.NETHERITE_BOOTS;
    }

    /**
     * 1. Blokowanie podglądu ulepszenia w stole kowalskim (Smithing Table).
     */
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result != null && isNetheriteArmor(result.getType())) {
            event.setResult(null);
        }
    }

    /**
     * 2. Blokowanie odebrania zrobionej zbroi ze stołu kowalskiego.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && isNetheriteArmor(result.getType())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(ChatColor.RED + "[BLOKADA] Wytwarzanie zbroi z netherytu jest całkowicie zablokowane na tym serwerze!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.9f);
            }
        }
    }

    /**
     * 3. Blokowanie zakładania zbroi prawym przyciskiem myszy oraz jej usuwanie z ręki.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.hasItem() && isNetheriteArmor(event.getItem().getType())) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                event.getItem().setAmount(0);
                Player player = event.getPlayer();
                player.sendMessage(ChatColor.RED + "[BLOKADA] Netherytowe zbroje są zablokowane i zostały usunięte z Twojego ekwipunku!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            }
        }
    }

    /**
     * 4. Usuwanie i blokowanie kliknięcia na netherytową zbroję w ekwipunku / skrzyni.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current != null && isNetheriteArmor(current.getType())) {
            event.setCurrentItem(null);
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "[BLOKADA] Wykryto i usunięto zablokowaną część netherytowej zbroi!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && isNetheriteArmor(cursor.getType())) {
            event.getView().setCursor(null);
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "[BLOKADA] Wykryto i usunięto zablokowaną część netherytowej zbroi!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        }
    }

    /**
     * 5. Blokowanie przeciągania (drag) w sloty pancerza.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getOldCursor() != null && isNetheriteArmor(event.getOldCursor().getType())) {
            event.setCancelled(true);
            event.getView().setCursor(null);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(ChatColor.RED + "[BLOKADA] Netherytowe zbroje są zablokowane!");
            }
        }
    }

    /**
     * 6. Blokowanie zakładania przez Dispenser (dozownik).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (isNetheriteArmor(event.getItem().getType())) {
            event.setCancelled(true);
        }
    }

    /**
     * 7. Blokowanie podnoszenia z ziemi i natychmiastowe usunięcie encji.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (isNetheriteArmor(event.getItem().getItemStack().getType())) {
            event.getItem().remove();
            event.setCancelled(true);
            if (event.getEntity() instanceof Player player) {
                player.sendMessage(ChatColor.RED + "[BLOKADA] Zbroje z netherytu są zablokowane i zostały zniszczone!");
            }
        }
    }

    /**
     * 8. Blokowanie pojawiania się jako drop w świecie (skrzynie, moby itp.).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (isNetheriteArmor(event.getEntity().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    /**
     * 9. Czyszczenie ekwipunku gracza przy dołączeniu na serwer.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        purgePlayerArmor(event.getPlayer());
    }

    /**
     * 10. Czyszczenie ekwipunku gracza przy odrodzeniu.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        purgePlayerArmor(event.getPlayer());
    }

    /**
     * Skuteczne czyszczenie wszystkich slotów ekwipunku i slotów pancerza z netherytowej zbroi.
     */
    public static void purgePlayerArmor(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inv = player.getInventory();
        boolean removed = false;

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isNetheriteArmor(armor[i].getType())) {
                armor[i] = null;
                removed = true;
            }
        }
        if (removed) {
            inv.setArmorContents(armor);
        }

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null && isNetheriteArmor(storage[i].getType())) {
                storage[i] = null;
                removed = true;
            }
        }
        if (removed) {
            inv.setStorageContents(storage);
        }

        ItemStack offhand = inv.getItemInOffHand();
        if (offhand != null && isNetheriteArmor(offhand.getType())) {
            inv.setItemInOffHand(null);
            removed = true;
        }

        if (removed) {
            player.sendMessage(ChatColor.RED + "[BLOKADA] Z Twojego ekwipunku usunięto zablokowaną zbroję z netherytu!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        }
    }
}
