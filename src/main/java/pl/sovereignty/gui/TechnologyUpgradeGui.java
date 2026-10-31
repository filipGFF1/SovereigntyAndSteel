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
import pl.sovereignty.manager.VaultEconomyManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;
import pl.sovereignty.model.TechnologyTier;

import java.util.ArrayList;
import java.util.List;

public class TechnologyUpgradeGui {
    public static final String TITLE = "§8Drzewko Technologiczne Państwa";

    public static double getDiscountedCost(Nation nation, TechnologyTier tier) {
        double cost = tier.getCost();
        if (nation.getRegime() == PoliticalRegime.TECHNOCRACY) {
            cost *= 0.80; // -20% w Technokracji
        } else if (nation.getRegime() == PoliticalRegime.MERITOCRACY) {
            cost *= 0.85; // -15% w Merytokracji
        }
        return cost;
    }

    public static void open(Player player, Nation nation, VaultEconomyManager economyManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        TechnologyTier current = nation.getTechTier();
        TechnologyTier[] tiers = TechnologyTier.values();
        int[] slots = new int[]{11, 12, 13, 14, 15};

        for (int i = 0; i < tiers.length && i < slots.length; i++) {
            TechnologyTier t = tiers[i];
            boolean isUnlocked = t.getLevel() <= current.getLevel();
            boolean isNext = t == current.getNext();

            ItemStack item = new ItemStack(t.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = isUnlocked ? "&a✔ " : (isNext ? "&e▶ " : "&8🔒 ");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', prefix + t.getDisplayName()));

                List<String> lore = new ArrayList<>();
                for (String l : t.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }

                lore.add(" ");
                if (isUnlocked) {
                    lore.add(ChatColor.GREEN + "STATUS: ODBLOKOWANO");
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else if (isNext) {
                    double cost = getDiscountedCost(nation, t);
                    lore.add(ChatColor.YELLOW + "KOSZT UPGRADE'U: " + ChatColor.GOLD + economyManager.format(cost));
                    if (nation.getRegime() == PoliticalRegime.TECHNOCRACY) {
                        lore.add(ChatColor.AQUA + "Bonus Technokracji: zniżka 20%!");
                    } else if (nation.getRegime() == PoliticalRegime.MERITOCRACY) {
                        lore.add(ChatColor.AQUA + "Bonus Merytokracji: zniżka 15%!");
                    }
                    lore.add(" ");
                    lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby sfinansować z budżetu państwa!");
                } else {
                    lore.add(ChatColor.RED + "STATUS: ZABLOKOWANE");
                    lore.add(ChatColor.GRAY + "Wymaga odblokowania poprzedniego poziomu.");
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
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
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
    }
}
