package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.LeaderboardManager;
import pl.sovereignty.model.Nation;

import java.util.ArrayList;
import java.util.List;

public class TopLeaderboardGui {
    public static final String TITLE = ChatColor.DARK_BLUE + "Rankingi Wyspy - Top Nacji";

    public static void open(Player player, LeaderboardManager leaderboard) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        // Top GDP
        inv.setItem(10, createCategoryItem(Material.GOLD_BLOCK, "&6&lTop 10 PKB (GDP)",
                "&7Najbogatsze gospodarki wyspy.", formatGdpList(leaderboard.getTopGdp(10))));

        // Top Military
        inv.setItem(12, createCategoryItem(Material.NETHERITE_SWORD, "&c&lTop Potęgi Militarnej",
                "&7Poziom armii, liczba machin i wygrane starcia.", formatMilitaryList(leaderboard.getTopMilitary(10))));

        // Top Pirates
        inv.setItem(14, createCategoryItem(Material.TRIDENT, "&9&lTop Pogromców Piratów",
                "&7Liczba odpartych najazdów morskich barbarzyńców.", formatPirateList(leaderboard.getTopPirateSlayers(10))));

        // Top Welfare
        inv.setItem(16, createCategoryItem(Material.BREAD, "&a&lTop Dobrobytu (Welfare)",
                "&7Zadowolenie mieszkańców: pełne silosy i niskie podatki.", formatWelfareList(leaderboard.getTopWelfare(10))));

        // Ozdoby
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    private static ItemStack createCategoryItem(Material mat, String name, String subtitle, List<String> lines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', subtitle));
            lore.add(" ");
            for (String l : lines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<String> formatGdpList(List<Nation> list) {
        List<String> out = new ArrayList<>();
        if (list.isEmpty()) {
            out.add("&8Brak danych o państwach.");
            return out;
        }
        int rank = 1;
        for (Nation n : list) {
            out.add("&e#" + rank + " &f" + n.getName() + " &8[" + n.getTag() + "] &7- PKB: &a" + n.getGdp());
            rank++;
        }
        return out;
    }

    private static List<String> formatMilitaryList(List<Nation> list) {
        List<String> out = new ArrayList<>();
        if (list.isEmpty()) {
            out.add("&8Brak danych o państwach.");
            return out;
        }
        int rank = 1;
        for (Nation n : list) {
            out.add("&e#" + rank + " &f" + n.getName() + " &8[" + n.getTag() + "] &7- Punkty: &c" + n.getMilitaryScore());
            rank++;
        }
        return out;
    }

    private static List<String> formatPirateList(List<Nation> list) {
        List<String> out = new ArrayList<>();
        if (list.isEmpty()) {
            out.add("&8Brak danych o państwach.");
            return out;
        }
        int rank = 1;
        for (Nation n : list) {
            out.add("&e#" + rank + " &f" + n.getName() + " &8[" + n.getTag() + "] &7- Pokonanych rajadów: &b" + n.getPiratesDefeated());
            rank++;
        }
        return out;
    }

    private static List<String> formatWelfareList(List<Nation> list) {
        List<String> out = new ArrayList<>();
        if (list.isEmpty()) {
            out.add("&8Brak danych o państwach.");
            return out;
        }
        int rank = 1;
        for (Nation n : list) {
            out.add("&e#" + rank + " &f" + n.getName() + " &8[" + n.getTag() + "] &7- Wskaźnik: &a" + (int) n.getWelfareScore() + " pkt");
            rank++;
        }
        return out;
    }
}
