package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.VaultEconomyManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;

import java.util.ArrayList;
import java.util.List;

public class PoliticalRegimeGui {
    public static final String TITLE = "§8Ustroje Polityczne Państwa";

    // 3 rzędy po 7 slotów = dokładnie 21 slotów dla 21 ustrojów
    private static final int[] SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(Player player, Nation nation, VaultEconomyManager economyManager) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        PoliticalRegime[] regimes = PoliticalRegime.values();

        for (int i = 0; i < regimes.length && i < SLOTS.length; i++) {
            PoliticalRegime regime = regimes[i];
            boolean isCurrent = nation.getRegime() == regime;

            ItemStack item = new ItemStack(regime.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = isCurrent ? "&a&l[AKTYWNY] " : "&e&l";
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', prefix + regime.getDisplayName()));

                List<String> lore = new ArrayList<>();
                lore.add("&7Doktryna, buffy i debuffy:");
                for (String l : regime.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                lore.add(" ");
                lore.add(ChatColor.GRAY + "Koszt zmiany ustroju: " + ChatColor.GOLD + economyManager.format(regime.getTransitionCost()));

                if (isCurrent) {
                    lore.add(" ");
                    lore.add(ChatColor.GREEN + "✔ To jest obecny ustrój Twojego państwa.");
                    lore.add(ChatColor.DARK_GREEN + "  Wszystkie buffy i debuffy są aktywne.");
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(" ");
                    lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby ogłosić ten ustrój w ratuszu!");
                }
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(SLOTS[i], item);
        }

        // Informacyjna księga na dole (slot 49)
        ItemStack infoBook = new ItemStack(Material.BOOK);
        ItemMeta bMeta = infoBook.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Doktryny & Ustroje Świata");
            List<String> bLore = new ArrayList<>();
            bLore.add(ChatColor.GRAY + "System oferuje 21 ustrojów od starożytności po czasy nowożytne:");
            bLore.add(ChatColor.GREEN + "• [BUFFY]: Stałe efekty mikstur na własnym terenie,");
            bLore.add(ChatColor.GREEN + "  bonusy bojowe, lifesteal, inwigilacja i tańsza ekspansja.");
            bLore.add(ChatColor.RED + "• [DEBUFFY]: Ograniczenia gospodarcze, pacyfizm PvP,");
            bLore.add(ChatColor.RED + "  wilczy apetyt, wyższy Upkeep lub wrażliwość na ciosy.");
            bLore.add(ChatColor.DARK_GRAY + "W stanie zadłużenia (Upkeep Debt) bonusy zostają wyłączone.");
            bMeta.setLore(bLore);
            infoBook.setItemMeta(bMeta);
        }
        inv.setItem(49, infoBook);

        // Wypełnienie tła
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    public static PoliticalRegime getRegimeFromSlot(int slot) {
        PoliticalRegime[] regimes = PoliticalRegime.values();
        for (int i = 0; i < SLOTS.length && i < regimes.length; i++) {
            if (SLOTS[i] == slot) {
                return regimes[i];
            }
        }
        return null;
    }
}
