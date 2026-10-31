package pl.sovereignty.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.sovereignty.command.KoszCommand;
import pl.sovereignty.command.SitCommand;
import pl.sovereignty.gui.ShopGui;
import pl.sovereignty.gui.ShopHolder;
import pl.sovereignty.gui.ShopItem;
import pl.sovereignty.gui.TrashHolder;
import pl.sovereignty.manager.EconomyManager;
import pl.sovereignty.manager.SeatManager;
import pl.sovereignty.manager.VaultEconomyManager;

import java.util.HashMap;
import java.util.List;

public class ShopListener implements Listener {
    private final VaultEconomyManager vaultEconomyManager;
    private final EconomyManager economyManager;
    private final SeatManager seatManager;

    public ShopListener(VaultEconomyManager vaultEconomyManager, EconomyManager economyManager, SeatManager seatManager) {
        this.vaultEconomyManager = vaultEconomyManager;
        this.economyManager = economyManager;
        this.seatManager = seatManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder shopHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        // 1. Menu główne sklepu
        if (shopHolder.isMainMenu()) {
            switch (slot) {
                case 11 -> ShopGui.openCategory(player, "building", vaultEconomyManager);
                case 12 -> ShopGui.openCategory(player, "farming", vaultEconomyManager);
                case 13 -> ShopGui.openCategory(player, "mining", vaultEconomyManager);
                case 14 -> ShopGui.openCategory(player, "military", vaultEconomyManager);
                case 15 -> ShopGui.openCategory(player, "sovereignty", vaultEconomyManager);
                case 21, 22 -> {
                    // Otwórz kosz
                    TrashHolder holder = new TrashHolder();
                    Inventory inv = Bukkit.createInventory(holder, 36, KoszCommand.TITLE);
                    holder.setInventory(inv);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&cKosz&8] &7Wszystkie przedmioty włożone do tego okna zostaną &cbezpowrotnie usunięte &7po zamknięciu!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.8f);
                    player.openInventory(inv);
                }
                case 23 -> {
                    // Siadanie
                    player.closeInventory();
                    if (seatManager != null) {
                        SitCommand.handleSit(player, seatManager);
                    } else {
                        player.performCommand("sit");
                    }
                }
                default -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            }
            return;
        }

        // 2. Podmenu kategorii
        String catId = shopHolder.getCategoryId();
        if (slot == 45) {
            // Przycisk powrotu do menu głównego
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            ShopGui.openMainMenu(player, vaultEconomyManager);
            return;
        }

        if (slot == 49 || slot == 53) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            return;
        }

        // Szukamy pasującego przedmiotu w kategorii
        List<ShopItem> items = ShopGui.getCategoryItems(catId);
        ShopItem selectedItem = null;
        for (ShopItem item : items) {
            if (item.getMaterial() == clicked.getType()) {
                selectedItem = item;
                break;
            }
        }

        if (selectedItem == null) return;

        ClickType click = event.getClick();
        boolean isBuy = click.isLeftClick();
        boolean isSell = click.isRightClick();
        boolean isShift = click.isShiftClick();

        if (isBuy) {
            handleBuy(player, selectedItem, isShift, catId);
        } else if (isSell) {
            handleSell(player, selectedItem, isShift, catId);
        }
    }

    private void handleBuy(Player player, ShopItem item, boolean isShift, String catId) {
        if (!item.canBuy()) {
            player.sendMessage(ChatColor.RED + "Tego przedmiotu nie można kupić w sklepie!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int amount = isShift ? 16 : 1;
        double cost = item.getBuyPrice() * amount;

        if (!vaultEconomyManager.hasPlayerBalance(player, cost)) {
            player.sendMessage(ChatColor.RED + "Brak wystarczających funduszy! Potrzebujesz: "
                    + ChatColor.YELLOW + vaultEconomyManager.format(cost)
                    + ChatColor.RED + " (Twoje saldo: " + ChatColor.GOLD
                    + vaultEconomyManager.format(vaultEconomyManager.getPlayerBalance(player)) + ChatColor.RED + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!vaultEconomyManager.chargePlayer(player, cost)) {
            player.sendMessage(ChatColor.RED + "Błąd transakcji finansowej!");
            return;
        }

        // Przekazanie 5% podatku do Władcy Handlu
        if (economyManager != null) {
            economyManager.processTradeTax(cost);
        }

        ItemStack bought = new ItemStack(item.getMaterial(), amount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(bought);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
            player.sendMessage(ChatColor.YELLOW + "Twój ekwipunek był pełny! Część przedmiotów upadła na ziemię.");
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Kupiono " + ChatColor.WHITE + amount + "x " + item.getDisplayName()
                + ChatColor.GREEN + " za " + ChatColor.GOLD + vaultEconomyManager.format(cost) + ChatColor.GREEN + "!");

        // Odświeżenie widoku ze zaktualizowanym portfelem
        ShopGui.openCategory(player, catId, vaultEconomyManager);
    }

    private void handleSell(Player player, ShopItem item, boolean isShift, String catId) {
        if (!item.canSell()) {
            player.sendMessage(ChatColor.RED + "Tego przedmiotu nie można sprzedać!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Material target = item.getMaterial();
        int available = 0;

        // Liczymy wyłącznie zwykłe przedmioty (bez unikalnych lore/nazw pluginowych)
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (isEligibleForSale(invItem, target)) {
                available += invItem.getAmount();
            }
        }

        if (available <= 0) {
            player.sendMessage(ChatColor.RED + "Nie posiadasz przedmiotu " + ChatColor.YELLOW + item.getDisplayName()
                    + ChatColor.RED + " w ekwipunku do sprzedaży!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int toSell = isShift ? available : 1;
        if (toSell > available) toSell = available;

        int remaining = toSell;
        for (int s = 0; s < player.getInventory().getSize(); s++) {
            ItemStack invItem = player.getInventory().getItem(s);
            if (isEligibleForSale(invItem, target)) {
                if (invItem.getAmount() <= remaining) {
                    remaining -= invItem.getAmount();
                    player.getInventory().setItem(s, null);
                } else {
                    invItem.setAmount(invItem.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }

        double payout = item.getSellPrice() * toSell;
        vaultEconomyManager.depositPlayer(player, payout);

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.6f);
        player.sendMessage(ChatColor.GREEN + "Sprzedano " + ChatColor.WHITE + toSell + "x " + item.getDisplayName()
                + ChatColor.GREEN + " za " + ChatColor.GOLD + "+" + vaultEconomyManager.format(payout)
                + ChatColor.GREEN + "! " + ChatColor.DARK_GRAY + "(niska cena skupu)");

        // Odświeżenie widoku
        ShopGui.openCategory(player, catId, vaultEconomyManager);
    }

    private boolean isEligibleForSale(ItemStack item, Material target) {
        if (item == null || item.getType() != target) return false;
        if (!item.hasItemMeta()) return true;
        var meta = item.getItemMeta();
        return (!meta.hasDisplayName() && !meta.hasLore());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TrashHolder) {
            int deleted = 0;
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    deleted += item.getAmount();
                }
            }

            event.getInventory().clear();

            if (deleted > 0 && event.getPlayer() instanceof Player player) {
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.7f, 1.2f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8[&cKosz&8] &7Trwale zniszczono &c" + deleted + " &7przedmiotów."));
            }
        }
    }
}
