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
import pl.sovereignty.manager.WallManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;
import pl.sovereignty.model.WallTier;

import java.util.ArrayList;
import java.util.List;

public class WallMenuGui {
    public static final String TITLE = "§8Zarządzanie Murami Obronnymi";

    public static void open(Player player, Nation nation, WallManager wallManager, VaultEconomyManager economyManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        int perimeter = wallManager.calculatePerimeterMeters(nation);
        WallTier current = nation.getWallTier();

        // 1. Info o obwodzie (slot 4)
        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Obwód Granic Państwa");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Długość obwodu: " + ChatColor.WHITE + perimeter + " m.b.");
            lore.add(ChatColor.GRAY + "Aktualne umocnienia: " + ChatColor.YELLOW + current.getDisplayName());
            if (nation.getRegime() == PoliticalRegime.HEREDITARY_MONARCHY) {
                lore.add(ChatColor.AQUA + "Bonus Monarchii: zniżka 10% na mury!");
            }
            if (nation.getRegime() == PoliticalRegime.CORSAIR_SYNDICATE) {
                lore.add(ChatColor.RED + "Minus Syndykatu Korsarskiego: mury kosztują 200%!");
            }
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inv.setItem(4, info);

        // 2. Opcje murów: Tier 1 (slot 11), Tier 2 (slot 13), Tier 3 (slot 15)
        WallTier[] tiers = new WallTier[]{WallTier.TIER_1_WOODEN_PALISADE, WallTier.TIER_2_STONE_WALL, WallTier.TIER_3_COPPER_DEEPSLATE};
        int[] slots = new int[]{11, 13, 15};

        for (int i = 0; i < tiers.length; i++) {
            WallTier tier = tiers[i];
            boolean isCurrent = current == tier;
            double cost = wallManager.calculateWallCost(nation, tier);

            ItemStack item = new ItemStack(tier.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        (isCurrent ? "&a[AKTYWNY] " : "&e") + tier.getDisplayName()));

                List<String> lore = new ArrayList<>();
                for (String l : tier.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                lore.add(" ");
                lore.add(ChatColor.GRAY + "Całkowity koszt budowy obwodu: " + ChatColor.GOLD + economyManager.format(cost));

                if (isCurrent) {
                    lore.add(ChatColor.GREEN + "✔ Twój obecny typ fortyfikacji.");
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby wybudować ten mur!");
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i], item);
        }

        // 3. Demontaż muru (slot 22)
        if (current != WallTier.NONE) {
            ItemStack remove = new ItemStack(Material.BARRIER);
            ItemMeta rMeta = remove.getItemMeta();
            if (rMeta != null) {
                rMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Zdemontuj Fortyfikacje");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Usuwa wszystkie mury obronne i przywraca otwarte granice.");
                lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby rozebrać mury.");
                rMeta.setLore(lore);
                remove.setItemMeta(rMeta);
            }
            inv.setItem(22, remove);
        }

        // Tło
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
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, 0.7f, 1.0f);
    }
}
