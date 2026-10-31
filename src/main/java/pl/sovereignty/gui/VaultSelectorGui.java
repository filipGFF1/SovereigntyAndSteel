package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.VaultManager;
import pl.sovereignty.model.Nation;

import java.util.ArrayList;
import java.util.List;

public class VaultSelectorGui {
    public static final String TITLE = "§8Skarbiec Krajowy - Skrzynie";

    public static void open(Player player, Nation nation, VaultManager vaultManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        int unlockedPages = nation.getVaultTier().getPages();
        int[] chestSlots = new int[]{11, 12, 13, 14, 15};

        // 1. Sloty skrzyń 1 do 5
        for (int i = 0; i < 5; i++) {
            int pageNum = i + 1;
            boolean isUnlocked = pageNum <= unlockedPages;

            ItemStack item;
            if (isUnlocked) {
                item = new ItemStack(i == 0 ? Material.BARREL : Material.CHEST);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Skrzynia Skarbca #" + pageNum);
                    List<String> lore = new ArrayList<>();
                    Inventory pageInv = vaultManager.getVaultInventory(nation, i);
                    int used = vaultManager.getUsedSlots(pageInv);

                    lore.add(ChatColor.GRAY + "Pojemność: " + ChatColor.WHITE + "54 sloty");
                    lore.add(ChatColor.GRAY + "Zapełnienie: " + (used >= 54 ? ChatColor.RED : ChatColor.YELLOW) + used + ChatColor.GRAY + "/54 slotów");
                    if (nation.getVaultTier().hasAutoSell()) {
                        lore.add(ChatColor.GOLD + "⭐ SKRZYNIA OBJĘTA AUTO-SELL" + (nation.isVaultAutoSell() ? " (Aktywny)" : " (Wyłączony)"));
                    }
                    lore.add(" ");
                    lore.add(ChatColor.GREEN + "▶ Kliknij LPM, aby otworzyć tę skrzynię!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            } else {
                item = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.DARK_GRAY + "🔒 Skrzynia Skarbca #" + pageNum + " " + ChatColor.RED + "(Zablokowana)");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Wymaga rozbudowy skarbca do " + ChatColor.GOLD + "Poziomu " + pageNum + ChatColor.GRAY + ".");
                    lore.add(" ");
                    lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby przejść do ulepszeń!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(chestSlots[i], item);
        }

        // 2. Info (slot 4)
        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Informacje o Skarbcu Krajowym");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Kopalnia, tartak i farma automatycznie");
            lore.add(ChatColor.GRAY + "przesyłają urobek do Twoich skrzyń.");
            lore.add(ChatColor.GRAY + "Gdy Skrzynia #1 się zapełni, surowce");
            lore.add(ChatColor.GRAY + "wędrują płynnie do kolejnych skrzyń.");
            if (nation.getVaultTier().hasAutoSell()) {
                lore.add(" ");
                lore.add(ChatColor.AQUA + "Poziom V: Aktywny moduł Auto-Sell!");
            }
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inv.setItem(4, info);

        // 3. Przycisk ulepszenia (slot 22)
        ItemStack upgrade = new ItemStack(Material.ANVIL);
        ItemMeta uMeta = upgrade.getItemMeta();
        if (uMeta != null) {
            uMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Rozbuduj Pojemność Skarbca");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Aktualny poziom: " + ChatColor.GOLD + nation.getVaultTier().getDisplayName());
            lore.add(ChatColor.GRAY + "Łączna pojemność: " + ChatColor.AQUA + (unlockedPages * 54) + " slotów " + ChatColor.DARK_GRAY + "(" + unlockedPages + " skrzyń)");
            lore.add(ChatColor.GRAY + "Zajęte łącznie: " + ChatColor.WHITE + vaultManager.getTotalUsedSlots(nation) + ChatColor.GRAY + "/" + (unlockedPages * 54));
            lore.add(" ");
            lore.add(ChatColor.GREEN + "▶ Kliknij LPM, aby otworzyć menu ulepszeń!");
            uMeta.setLore(lore);
            upgrade.setItemMeta(uMeta);
        }
        inv.setItem(22, upgrade);

        // 4. Opcje Auto-Sell dla Tier 5
        if (nation.getVaultTier().hasAutoSell()) {
            // Przełącznik Auto-Sell (slot 24)
            ItemStack toggle = new ItemStack(nation.isVaultAutoSell() ? Material.HOPPER : Material.BARRIER);
            ItemMeta tMeta = toggle.getItemMeta();
            if (tMeta != null) {
                tMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Moduł Auto-Sell: "
                        + (nation.isVaultAutoSell() ? ChatColor.GREEN + "WŁĄCZONY" : ChatColor.RED + "WYŁĄCZONY"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Gdy włączony, nadmiar surowców ze skarbca");
                lore.add(ChatColor.GRAY + "jest automatycznie sprzedawany prosto");
                lore.add(ChatColor.GRAY + "do Skarbca Narodowego (Banku).");
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby przełączyć stan!");
                if (nation.isVaultAutoSell()) {
                    tMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    tMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                tMeta.setLore(lore);
                toggle.setItemMeta(tMeta);
            }
            inv.setItem(24, toggle);

            // Błyskawiczna sprzedaż wszystkich skrzyń skarbca (slot 20)
            ItemStack quickSell = new ItemStack(Material.GOLD_INGOT);
            ItemMeta qMeta = quickSell.getItemMeta();
            if (qMeta != null) {
                qMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Spienięż Cały Skarbiec (#1 - #" + unlockedPages + ")");
                List<String> lore = new ArrayList<>();
                int totalUsed = vaultManager.getTotalUsedSlots(nation);
                lore.add(ChatColor.GRAY + "Natychmiast sprzedaje surowce znajdujące");
                lore.add(ChatColor.GRAY + "się we wszystkich skrzyniach skarbca.");
                lore.add(ChatColor.GRAY + "Łącznie zajęte sloty: " + ChatColor.YELLOW + totalUsed + ChatColor.GRAY + "/" + (unlockedPages * 54));
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby sprzedać urobek ze wszystkich skrzyń!");
                qMeta.setLore(lore);
                quickSell.setItemMeta(qMeta);
            }
            inv.setItem(20, quickSell);
        }

        // Tło
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.7f, 1.2f);
    }
}
