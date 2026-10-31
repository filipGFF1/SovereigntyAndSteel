package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeBookGui {
    public static final String MAIN_TITLE = ChatColor.DARK_GRAY + "Spis Receptur: Sovereignty & Steel";
    public static final String DETAIL_PREFIX = ChatColor.DARK_BLUE + "Crafting 3x3: ";

    public static void openMainMenu(Player player, CustomItemRegistry items) {
        Inventory inv = Bukkit.createInventory(null, 36, MAIN_TITLE);

        inv.setItem(10, createMenuRecipeIcon(items.getBarracksCore(), "barracks_core"));
        inv.setItem(11, createMenuRecipeIcon(items.getSawmillTotem(), "sawmill_totem"));
        inv.setItem(12, createMenuRecipeIcon(items.getMiningHead(), "mining_head"));
        inv.setItem(13, createMenuRecipeIcon(items.getStateFarm(), "state_farm"));
        inv.setItem(14, createMenuRecipeIcon(items.getWarCouncilTable(), "war_council_table"));
        inv.setItem(15, createMenuRecipeIcon(items.getCrateRam(), "crate_ram"));
        inv.setItem(16, createMenuRecipeIcon(items.getCrateBallista(), "crate_ballista"));
        inv.setItem(19, createMenuRecipeIcon(items.getCrateMortar(), "crate_mortar"));
        inv.setItem(20, createMenuRecipeIcon(items.getEngineeringWrench(), "engineering_wrench"));
        inv.setItem(21, createMenuRecipeIcon(items.getNationalVault(), "national_vault"));
        inv.setItem(22, createMenuRecipeIcon(items.getMarshalMace(), "marshal_mace"));
        inv.setItem(23, createMenuRecipeIcon(items.getWarHorn(), "war_horn"));
        inv.setItem(24, createMenuRecipeIcon(items.getShortShield(), "short_shield"));

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

    private static ItemStack createMenuRecipeIcon(ItemStack result, String recipeId) {
        ItemStack clone = result.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "▶ Kliknij LPM, aby zobaczyć recepturę 3x3!");
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public static void openRecipeDetail(Player player, String recipeId, CustomItemRegistry items) {
        ItemStack result = switch (recipeId) {
            case "barracks_core" -> items.getBarracksCore();
            case "sawmill_totem" -> items.getSawmillTotem();
            case "mining_head" -> items.getMiningHead();
            case "crate_ram" -> items.getCrateRam();
            case "crate_ballista" -> items.getCrateBallista();
            case "crate_mortar" -> items.getCrateMortar();
            case "war_council_table" -> items.getWarCouncilTable();
            case "engineering_wrench" -> items.getEngineeringWrench();
            case "marshal_mace" -> items.getMarshalMace();
            case "war_horn" -> items.getWarHorn();
            case "national_vault" -> items.getNationalVault();
            case "state_farm" -> items.getStateFarm();
            case "short_shield" -> items.getShortShield();
            default -> null;
        };

        if (result == null) return;

        String title = DETAIL_PREFIX + ChatColor.stripColor(result.getItemMeta().getDisplayName());
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 45, title);

        // Wypełnienie tła
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Puste sloty siatki craftingu ustawiamy na jasnoszare
        ItemStack gridBorder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gbMeta = gridBorder.getItemMeta();
        if (gbMeta != null) {
            gbMeta.setDisplayName(ChatColor.DARK_GRAY + "Puste pole craftingu");
            gridBorder.setItemMeta(gbMeta);
        }

        int[] gridSlots = new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int s : gridSlots) {
            inv.setItem(s, gridBorder);
        }

        // Strzałka wyniku
        ItemStack arrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta aMeta = arrow.getItemMeta();
        if (aMeta != null) {
            aMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "➡ Wynik Craftingu");
            arrow.setItemMeta(aMeta);
        }
        inv.setItem(23, arrow);

        // Wynik
        inv.setItem(24, result);

        // Przycisk powrotu
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(ChatColor.GREEN + "⬅ Powrót do spisu receptur");
            back.setItemMeta(bMeta);
        }
        inv.setItem(40, back);

        // Uzupełnienie 9 pól siatki (3x3)
        ItemStack[] ingredients = getRecipeIngredients(recipeId);
        for (int i = 0; i < 9; i++) {
            if (ingredients[i] != null) {
                inv.setItem(gridSlots[i], ingredients[i]);
            }
        }

        player.openInventory(inv);
    }

    private static ItemStack[] getRecipeIngredients(String recipeId) {
        ItemStack[] grid = new ItemStack[9];
        switch (recipeId) {
            case "barracks_core" -> {
                // W H W / P B P / P C P
                grid[0] = createIngredient(Material.WHITE_WOOL, "&fDowolna Wełna");
                grid[1] = createIngredient(Material.IRON_HELMET, "&fŻelazny Hełm");
                grid[2] = createIngredient(Material.WHITE_WOOL, "&fDowolna Wełna");
                grid[3] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[4] = createIngredient(Material.WHITE_BED, "&cDowolne Łóżko");
                grid[5] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[6] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[7] = createIngredient(Material.CHEST, "&6Skrzynia");
                grid[8] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
            }
            case "sawmill_totem" -> {
                // A S A / L C L / B B B
                grid[0] = createIngredient(Material.IRON_AXE, "&fŻelazna Siekiera");
                grid[1] = createIngredient(Material.OAK_SAPLING, "&aDowolna Sadzonka");
                grid[2] = createIngredient(Material.IRON_AXE, "&fŻelazna Siekiera");
                grid[3] = createIngredient(Material.OAK_LOG, "&6Dowolna Kłoda");
                grid[4] = createIngredient(Material.CHEST, "&6Skrzynia");
                grid[5] = createIngredient(Material.OAK_LOG, "&6Dowolna Kłoda");
                grid[6] = createIngredient(Material.COBBLESTONE, "&7Bruk");
                grid[7] = createIngredient(Material.COBBLESTONE, "&7Bruk");
                grid[8] = createIngredient(Material.COBBLESTONE, "&7Bruk");
            }
            case "mining_head" -> {
                // P L P / C H C / I F I
                grid[0] = createIngredient(Material.IRON_PICKAXE, "&fŻelazny Kilof");
                grid[1] = createIngredient(Material.LEVER, "&7Dźwignia");
                grid[2] = createIngredient(Material.IRON_PICKAXE, "&fŻelazny Kilof");
                grid[3] = createIngredient(Material.COBBLESTONE, "&7Bruk");
                grid[4] = createIngredient(Material.HOPPER, "&7Lej (Hopper)");
                grid[5] = createIngredient(Material.COBBLESTONE, "&7Bruk");
                grid[6] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[7] = createIngredient(Material.BLAST_FURNACE, "&8Piec Hutniczy");
                grid[8] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
            }
            case "crate_ram" -> {
                // P I P / S M S / P I P
                grid[0] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[1] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[2] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[3] = createIngredient(Material.PISTON, "&7Tłok (Piston)");
                grid[4] = createIngredient(Material.MINECART, "&7Wagonik");
                grid[5] = createIngredient(Material.PISTON, "&7Tłok (Piston)");
                grid[6] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[7] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[8] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
            }
            case "crate_ballista" -> {
                // C X C / I F I / P P P
                grid[0] = createIngredient(Material.CHAIN, "&7Łańcuch");
                grid[1] = createIngredient(Material.CROSSBOW, "&6Kusza");
                grid[2] = createIngredient(Material.CHAIN, "&7Łańcuch");
                grid[3] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[4] = createIngredient(Material.OAK_FENCE, "&6Dowolny Płotek");
                grid[5] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[6] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[7] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[8] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
            }
            case "crate_mortar" -> {
                // C _ C / C D C / I R I
                grid[0] = createIngredient(Material.COPPER_BLOCK, "&6Blok Miedzi");
                grid[1] = null;
                grid[2] = createIngredient(Material.COPPER_BLOCK, "&6Blok Miedzi");
                grid[3] = createIngredient(Material.COPPER_BLOCK, "&6Blok Miedzi");
                grid[4] = createIngredient(Material.DISPENSER, "&7Dozownik (Dispenser)");
                grid[5] = createIngredient(Material.COPPER_BLOCK, "&6Blok Miedzi");
                grid[6] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[7] = createIngredient(Material.REDSTONE_BLOCK, "&cBlok Redstone");
                grid[8] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
            }
            case "war_council_table" -> {
                // B A B / I M I / P P P
                grid[0] = createIngredient(Material.WHITE_BANNER, "&fDowolny Sztandar");
                grid[1] = createIngredient(Material.GOLDEN_APPLE, "&eZłote Jabłko");
                grid[2] = createIngredient(Material.WHITE_BANNER, "&fDowolny Sztandar");
                grid[3] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[4] = createIngredient(Material.MAP, "&ePusta Mapa");
                grid[5] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[6] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[7] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[8] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
            }
            case "engineering_wrench" -> {
                // I I _ / _ C _ / _ S _
                grid[0] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[1] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[2] = null;
                grid[3] = null;
                grid[4] = createIngredient(Material.COPPER_INGOT, "&6Sztabka Miedzi");
                grid[5] = null;
                grid[6] = null;
                grid[7] = createIngredient(Material.STICK, "&7Patyk");
                grid[8] = null;
            }
            case "marshal_mace" -> {
                // G D G / _ B _ / _ B _
                grid[0] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[1] = createIngredient(Material.DIAMOND, "&bDiament");
                grid[2] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[3] = null;
                grid[4] = createIngredient(Material.BLAZE_ROD, "&ePłomienna Różdżka (Blaze Rod)");
                grid[5] = null;
                grid[6] = null;
                grid[7] = createIngredient(Material.BLAZE_ROD, "&ePłomienna Różdżka (Blaze Rod)");
                grid[8] = null;
            }
            case "war_horn" -> {
                // _ H _ / _ I _ / _ G _
                grid[0] = null;
                grid[1] = createIngredient(Material.GOAT_HORN, "&fKozi Róg");
                grid[2] = null;
                grid[3] = null;
                grid[4] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[5] = null;
                grid[6] = null;
                grid[7] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[8] = null;
            }
            case "national_vault" -> {
                // I G I / G B G / I O I
                grid[0] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[1] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[2] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[3] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[4] = createIngredient(Material.BARREL, "&6Beczka (Skarbiec)");
                grid[5] = createIngredient(Material.GOLD_INGOT, "&eSztabka Złota");
                grid[6] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[7] = createIngredient(Material.IRON_BLOCK, "&fBlok Żelaza");
                grid[8] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
            }
            case "state_farm" -> {
                // H W H / B C B / P P P
                grid[0] = createIngredient(Material.IRON_HOE, "&fŻelazna Motyka");
                grid[1] = createIngredient(Material.HAY_BLOCK, "&eSnop Siana");
                grid[2] = createIngredient(Material.IRON_HOE, "&fŻelazna Motyka");
                grid[3] = createIngredient(Material.WATER_BUCKET, "&bWiadro z Wodą");
                grid[4] = createIngredient(Material.BARREL, "&6Beczka / Spichlerz");
                grid[5] = createIngredient(Material.WATER_BUCKET, "&bWiadro z Wodą");
                grid[6] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[7] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[8] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
            }
            case "short_shield" -> {
                // P I P / _ P _ / _ _ _
                grid[0] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[1] = createIngredient(Material.IRON_INGOT, "&fSztabka Żelaza");
                grid[2] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[3] = null;
                grid[4] = createIngredient(Material.OAK_PLANKS, "&6Dowolne Deski");
                grid[5] = null;
                grid[6] = null;
                grid[7] = null;
                grid[8] = null;
            }
        }
        return grid;
    }

    private static ItemStack createIngredient(Material mat, String displayName) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Składnik stołu rzemieślniczego 3x3"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
