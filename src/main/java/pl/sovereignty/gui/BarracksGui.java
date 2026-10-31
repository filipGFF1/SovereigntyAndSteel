package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.MilitaryManager;
import pl.sovereignty.model.MilitaryUnitType;
import pl.sovereignty.model.Nation;

import java.util.Arrays;

public class BarracksGui {
    public static final String TITLE = ChatColor.DARK_RED + "Koszary Wojskowe - Pobór";

    public static void open(Player player, Nation nation, MilitaryManager militaryManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        int current = militaryManager.getCurrentArmyCount(nation.getId());
        int max = militaryManager.getMaxArmyLimit(nation.getId());
        double inflation = nation.getInflationFactor();

        // Status armii
        ItemStack status = new ItemStack(Material.BOOK);
        ItemMeta sMeta = status.getItemMeta();
        if (sMeta != null) {
            sMeta.setDisplayName(ChatColor.GOLD + "Stan Armii Państwa: " + nation.getName());
            sMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Liczebność: " + ChatColor.YELLOW + current + "/" + max + " NPC",
                    ChatColor.GRAY + "Zapas żywności: " + (nation.isStarving() ? ChatColor.RED + "GŁÓD (0)" : ChatColor.GREEN + String.valueOf((int) nation.getFoodStockpile())),
                    ChatColor.GRAY + "Wskaźnik inflacji: " + ChatColor.YELLOW + (int)(inflation * 100) + "%"
            ));
            status.setItemMeta(sMeta);
        }
        inv.setItem(4, status);

        // Rekrut 1: Kohorta Tarczowników
        int shieldFood = (int) Math.ceil(5 * inflation);
        inv.setItem(11, createRecruitIcon(Material.SHIELD,
                "&f&lKohorta Tarczowników",
                MilitaryUnitType.SHIELDBEARER.getDescription(),
                "&7Koszt: &e" + shieldFood + " chleba/żywności"));

        // Rekrut 2: Kompania Kuszników
        int crossFood = (int) Math.ceil(6 * inflation);
        inv.setItem(13, createRecruitIcon(Material.CROSSBOW,
                "&f&lKompania Kuszników",
                MilitaryUnitType.CROSSBOWMAN.getDescription(),
                "&7Koszt: &e" + crossFood + " chleba/żywności"));

        // Rekrut 3: Saperzy
        int sapperFood = (int) Math.ceil(4 * inflation);
        inv.setItem(15, createRecruitIcon(Material.IRON_PICKAXE,
                "&f&lOddział Saperów",
                MilitaryUnitType.SAPPER.getDescription(),
                "&7Koszt: &e" + sapperFood + " chleba/żywności"));

        // Wypełnienie
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
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

    private static ItemStack createRecruitIcon(Material mat, String name, String desc, String cost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&7" + desc),
                    " ",
                    ChatColor.translateAlternateColorCodes('&', cost),
                    ChatColor.GREEN + "Kliknij LPM, aby powołać do armii!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
