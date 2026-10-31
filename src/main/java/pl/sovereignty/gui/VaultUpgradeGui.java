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
import pl.sovereignty.model.VaultTier;

import java.util.ArrayList;
import java.util.List;

public class VaultUpgradeGui {
    public static final String TITLE = "§8Rozbudowa Skarbca Krajowego";

    public static double getDiscountedCost(Nation nation, VaultTier tier) {
        double cost = tier.getCost();
        if (nation.getRegime() == PoliticalRegime.MERCHANT_REPUBLIC) {
            cost *= 0.80; // -20% w Republice Handlowej
        } else if (nation.getRegime() == PoliticalRegime.HEREDITARY_MONARCHY) {
            cost *= 0.90; // -10% w Monarchii Dziedzicznej
        } else if (nation.getRegime() == PoliticalRegime.TECHNOCRACY) {
            cost *= 0.90; // -10% w Technokracji
        }
        return cost;
    }

    public static void open(Player player, Nation nation, VaultEconomyManager economyManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        VaultTier current = nation.getVaultTier();
        VaultTier[] tiers = VaultTier.values();
        int[] slots = new int[]{11, 12, 13, 14, 15};

        for (int i = 0; i < tiers.length && i < slots.length; i++) {
            VaultTier t = tiers[i];
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
                    lore.add(ChatColor.YELLOW + "KOSZT ROZBUDOWY: " + ChatColor.GOLD + economyManager.format(cost));
                    if (nation.getRegime() == PoliticalRegime.MERCHANT_REPUBLIC) {
                        lore.add(ChatColor.AQUA + "Bonus Republiki Handlowej: zniżka 20% na skarbiec!");
                    } else if (nation.getRegime() == PoliticalRegime.HEREDITARY_MONARCHY) {
                        lore.add(ChatColor.AQUA + "Bonus Monarchii: zniżka 10% na skarbiec królewski!");
                    } else if (nation.getRegime() == PoliticalRegime.TECHNOCRACY) {
                        lore.add(ChatColor.AQUA + "Bonus Technokracji: zniżka 10% na skarbiec!");
                    }
                    lore.add(" ");
                    lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby sfinansować z budżetu państwa!");
                } else {
                    lore.add(ChatColor.RED + "STATUS: ZABLOKOWANE");
                    lore.add(ChatColor.GRAY + "Wymaga odblokowania poprzedniego poziomu skarbca.");
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        // Przycisk powrotu (slot 22)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ChatColor.RED + "◀ Powrót do Skrzyń Skarbca");
            back.setItemMeta(bMeta);
        }
        inv.setItem(22, back);

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
