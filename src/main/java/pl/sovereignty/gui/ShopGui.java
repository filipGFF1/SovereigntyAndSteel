package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.VaultEconomyManager;

import java.util.*;

public class ShopGui {
    public static final String MAIN_TITLE = "§8§lRynek Handlowy §8• Sklep";
    public static final String CATEGORY_PREFIX = "§8Sklep: ";

    private static final Map<String, List<ShopItem>> CATEGORIES = new LinkedHashMap<>();

    static {
        // 1. Budownictwo (z uwzględnieniem żwiru i bardzo niskiej ceny skupu)
        List<ShopItem> building = new ArrayList<>();
        building.add(new ShopItem(Material.GRAVEL, "Żwir (Gravel)", 1.50, 0.05));
        building.add(new ShopItem(Material.COBBLESTONE, "Bruk (Cobblestone)", 1.00, 0.05));
        building.add(new ShopItem(Material.STONE, "Kamień (Stone)", 1.50, 0.05));
        building.add(new ShopItem(Material.STONE_BRICKS, "Kamienne Cegły", 2.00, 0.08));
        building.add(new ShopItem(Material.DIRT, "Ziemia (Dirt)", 0.80, 0.03));
        building.add(new ShopItem(Material.SAND, "Piasek (Sand)", 1.50, 0.05));
        building.add(new ShopItem(Material.GLASS, "Szkło (Glass)", 2.00, 0.06));
        building.add(new ShopItem(Material.OAK_PLANKS, "Deski Dębowe", 0.40, 0.01));
        building.add(new ShopItem(Material.SPRUCE_PLANKS, "Deski Świerkowe", 0.40, 0.01));
        building.add(new ShopItem(Material.BIRCH_PLANKS, "Deski Brzozowe", 0.40, 0.01));
        building.add(new ShopItem(Material.BAMBOO_PLANKS, "Deski Bambusowe", 0.40, 0.01));
        building.add(new ShopItem(Material.OAK_LOG, "Pień Dębu", 1.00, 0.03));
        building.add(new ShopItem(Material.SPRUCE_LOG, "Pień Świerku", 1.00, 0.03));
        building.add(new ShopItem(Material.BIRCH_LOG, "Pień Brzozy", 1.00, 0.03));
        building.add(new ShopItem(Material.BRICKS, "Cegły", 3.00, 0.10));
        building.add(new ShopItem(Material.TORCH, "Pochodnia", 1.00, 0.05));
        building.add(new ShopItem(Material.LANTERN, "Latarnia", 5.00, 0.25));
        building.add(new ShopItem(Material.FURNACE, "Piec (Furnace)", 4.00, 0.15));
        building.add(new ShopItem(Material.BLAST_FURNACE, "Piec Hutniczy (Blast Furnace)", 20.00, 0.80));
        building.add(new ShopItem(Material.SMOKER, "Wędzarnia (Smoker)", 12.00, 0.50));
        building.add(new ShopItem(Material.CRAFTING_TABLE, "Stół Rzemieślniczy", 2.00, 0.05));
        building.add(new ShopItem(Material.ANVIL, "Kowadło (Anvil)", 45.00, 2.00));
        building.add(new ShopItem(Material.STONECUTTER, "Przecinarka (Stonecutter)", 6.00, 0.20));
        building.add(new ShopItem(Material.GRINDSTONE, "Kamień Szlifierski", 8.00, 0.30));
        building.add(new ShopItem(Material.CHEST, "Skrzynia (Chest)", 3.00, 0.10));
        building.add(new ShopItem(Material.BARREL, "Beczka (Barrel)", 3.00, 0.10));
        CATEGORIES.put("building", building);

        // 2. Rolnictwo i Żywność
        List<ShopItem> farming = new ArrayList<>();
        farming.add(new ShopItem(Material.WHEAT, "Pszenica", 2.00, 0.12));
        farming.add(new ShopItem(Material.CARROT, "Marchewka", 2.00, 0.12));
        farming.add(new ShopItem(Material.POTATO, "Ziemniak", 2.00, 0.12));
        farming.add(new ShopItem(Material.BEETROOT, "Burak", 2.00, 0.10));
        farming.add(new ShopItem(Material.SUGAR_CANE, "Trzcina Cukrowa", 3.00, 0.15));
        farming.add(new ShopItem(Material.BAMBOO, "Bambus", 1.00, 0.02));
        farming.add(new ShopItem(Material.SWEET_BERRIES, "Słodkie Jagody", 2.00, 0.10));
        farming.add(new ShopItem(Material.PUMPKIN, "Dynia", 4.00, 0.20));
        farming.add(new ShopItem(Material.MELON, "Arbuz", 4.00, 0.20));
        farming.add(new ShopItem(Material.BREAD, "Chleb", 4.00, 0.25));
        farming.add(new ShopItem(Material.COOKED_BEEF, "Pieczona Wołowina", 8.00, 0.40));
        farming.add(new ShopItem(Material.COOKED_PORKCHOP, "Pieczona Wieprzowina", 8.00, 0.40));
        farming.add(new ShopItem(Material.GOLDEN_CARROT, "Złota Marchewka", 20.00, 1.20));
        farming.add(new ShopItem(Material.BONE_MEAL, "Mączka Kostna", 2.50, 0.10));
        farming.add(new ShopItem(Material.APPLE, "Jabłko", 4.00, 0.15));
        farming.add(new ShopItem(Material.OAK_SAPLING, "Sadzonka Dębu", 0.80, 0.02));
        farming.add(new ShopItem(Material.SPRUCE_SAPLING, "Sadzonka Świerku", 0.80, 0.02));
        farming.add(new ShopItem(Material.BIRCH_SAPLING, "Sadzonka Brzozy", 0.80, 0.02));
        CATEGORIES.put("farming", farming);

        // 3. Górnictwo i Surowce (również żwir, krzemień, metale)
        List<ShopItem> mining = new ArrayList<>();
        mining.add(new ShopItem(Material.GRAVEL, "Żwir (Gravel)", 1.50, 0.05));
        mining.add(new ShopItem(Material.FLINT, "Krzemień (Flint)", 2.50, 0.10));
        mining.add(new ShopItem(Material.COAL, "Węgiel", 2.70, 0.15));
        mining.add(new ShopItem(Material.RAW_COPPER, "Surowa Miedź", 5.00, 0.30));
        mining.add(new ShopItem(Material.COPPER_INGOT, "Sztabka Miedzi", 6.00, 0.35));
        mining.add(new ShopItem(Material.RAW_IRON, "Surowe Żelazo", 6.50, 0.40));
        mining.add(new ShopItem(Material.IRON_INGOT, "Sztabka Żelaza", 8.00, 0.50));
        mining.add(new ShopItem(Material.RAW_GOLD, "Surowe Złoto", 13.50, 0.80));
        mining.add(new ShopItem(Material.GOLD_INGOT, "Sztabka Złota", 16.50, 1.00));
        mining.add(new ShopItem(Material.REDSTONE, "Czerwony Kamień", 6.00, 0.35));
        mining.add(new ShopItem(Material.LAPIS_LAZULI, "Lapis Lazuli", 4.00, 0.22));
        mining.add(new ShopItem(Material.AMETHYST_SHARD, "Odłamek Ametystu", 10.00, 0.50));
        mining.add(new ShopItem(Material.DIAMOND, "Diament", 80.00, 5.00));
        mining.add(new ShopItem(Material.EMERALD, "Szmaragd", 60.00, 4.00));
        mining.add(new ShopItem(Material.OBSIDIAN, "Obsydian", 35.00, 1.80));
        CATEGORIES.put("mining", mining);

        // 4. Militaria i Ekwipunek
        List<ShopItem> military = new ArrayList<>();
        military.add(new ShopItem(Material.ARROW, "Strzała", 1.50, 0.08));
        military.add(new ShopItem(Material.BOW, "Łuk", 18.00, 1.00));
        military.add(new ShopItem(Material.CROSSBOW, "Kusza", 25.00, 1.50));
        military.add(new ShopItem(Material.IRON_SWORD, "Żelazny Miecz", 25.00, 1.20));
        military.add(new ShopItem(Material.SHIELD, "Tarcza", 25.00, 1.20));
        military.add(new ShopItem(Material.GUNPOWDER, "Proch Strzelniczy", 12.00, 0.60));
        military.add(new ShopItem(Material.TNT, "Dynamit (TNT)", 50.00, 2.50));
        military.add(new ShopItem(Material.FIREWORK_ROCKET, "Fajerwerka", 6.00, 0.30));
        military.add(new ShopItem(Material.LEATHER, "Skóra", 6.00, 0.30));
        military.add(new ShopItem(Material.FEATHER, "Pióro", 2.00, 0.10));
        military.add(new ShopItem(Material.STRING, "Nić", 3.00, 0.15));
        CATEGORIES.put("military", military);

        // 5. Technologia i Specjalne Narzędzia
        List<ShopItem> sovereignty = new ArrayList<>();
        sovereignty.add(new ShopItem(Material.SADDLE, "Siodło", 150.00, 10.00));
        sovereignty.add(new ShopItem(Material.NAME_TAG, "Znacznik (Name Tag)", 100.00, 8.00));
        sovereignty.add(new ShopItem(Material.COMPASS, "Kompas", 30.00, 2.00));
        sovereignty.add(new ShopItem(Material.CLOCK, "Zegar", 50.00, 3.00));
        sovereignty.add(new ShopItem(Material.SPYGLASS, "Luneta", 60.00, 4.00));
        sovereignty.add(new ShopItem(Material.LEAD, "Smycz", 15.00, 1.00));
        sovereignty.add(new ShopItem(Material.ENDER_PEARL, "Perła Endu", 80.00, 5.00));
        sovereignty.add(new ShopItem(Material.EXPERIENCE_BOTTLE, "Butelka z Doświadczeniem", 40.00, 2.00));
        sovereignty.add(new ShopItem(Material.HOPPER, "Lej (Hopper)", 25.00, 1.20));
        sovereignty.add(new ShopItem(Material.BREWING_STAND, "Statyw Alchemiczny", 40.00, 2.00));
        sovereignty.add(new ShopItem(Material.CAULDRON, "Kocioł (Cauldron)", 15.00, 0.70));
        sovereignty.add(new ShopItem(Material.SMITHING_TABLE, "Stół Kowalski", 10.00, 0.50));
        sovereignty.add(new ShopItem(Material.DISPENSER, "Dozownik (Dispenser)", 15.00, 0.60));
        sovereignty.add(new ShopItem(Material.CAMPFIRE, "Ognisko (Campfire)", 6.00, 0.25));
        sovereignty.add(new ShopItem(Material.SOUL_CAMPFIRE, "Ognisko Dusz", 10.00, 0.40));
        CATEGORIES.put("sovereignty", sovereignty);
    }

    public static List<ShopItem> getCategoryItems(String categoryId) {
        return CATEGORIES.getOrDefault(categoryId, Collections.emptyList());
    }

    public static String getCategoryDisplayName(String categoryId) {
        return switch (categoryId) {
            case "building" -> "Budownictwo i Bloki";
            case "farming" -> "Rolnictwo i Żywność";
            case "mining" -> "Górnictwo i Surowce";
            case "military" -> "Militaria i Uzbrojenie";
            case "sovereignty" -> "Technologia i Narzędzia";
            default -> "Kategoria";
        };
    }

    /**
     * Otwiera Menu Główne Sklepu z kategoriami
     */
    public static void openMainMenu(Player player, VaultEconomyManager economyManager) {
        ShopHolder holder = new ShopHolder(null);
        Inventory inv = Bukkit.createInventory(holder, 27, MAIN_TITLE);
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Portfel gracza (slot 4)
        double balance = economyManager.getPlayerBalance(player);
        inv.setItem(4, createItem(Material.GOLD_INGOT, "§6§lTwój Portfel",
                "§7Aktualne saldo: §a" + economyManager.format(balance),
                "§7Finanse zintegrowane z silnikiem Vault."));

        // Kategorie: 11, 12, 13, 14, 15
        inv.setItem(11, createItem(Material.STONE_BRICKS, "§e§lBudownictwo i Bloki",
                "§7Kamień, bruk, żwir (gravel), drewno, szkło...",
                "§8» Kliknij, aby przejść do oferty"));

        inv.setItem(12, createItem(Material.GOLDEN_CARROT, "§a§lRolnictwo i Żywność",
                "§7Plony, warzywa, chleb, pieczone mięsa...",
                "§8» Kliknij, aby przejść do oferty"));

        inv.setItem(13, createItem(Material.DIAMOND_PICKAXE, "§b§lGórnictwo i Surowce",
                "§7Żwir (gravel), krzemień, węgiel, żelazo, diamenty...",
                "§8» Kliknij, aby przejść do oferty"));

        inv.setItem(14, createItem(Material.IRON_SWORD, "§c§lMilitaria i Uzbrojenie",
                "§7Strzały, proch, tarcze, broń, TNT...",
                "§8» Kliknij, aby przejść do oferty"));

        inv.setItem(15, createItem(Material.NETHER_STAR, "§d§lTechnologia i Narzędzia",
                "§7Narzędzia, perły endu, siodła, smycze...",
                "§8» Kliknij, aby przejść do oferty"));

        // Skrót do kosza (slot 21)
        inv.setItem(21, createItem(Material.CAULDRON, "§4§lKosz na Odpady",
                "§7Szybkie wyrzucanie zbędnych rzeczy.",
                "§8» Kliknij, aby otworzyć kosz (/kosz)"));

        // Skrót do siadania (slot 23)
        inv.setItem(23, createItem(Material.OAK_STAIRS, "§6§lSiadanie (/sit)",
                "§7Usiądź wygodnie na ziemi lub schodkach.",
                "§8» Kliknij, aby usiąść"));

        player.openInventory(inv);
    }

    /**
     * Otwiera podmenu wybranej kategorii przedmiotów
     */
    public static void openCategory(Player player, String categoryId, VaultEconomyManager economyManager) {
        List<ShopItem> items = getCategoryItems(categoryId);
        String catName = getCategoryDisplayName(categoryId);

        ShopHolder holder = new ShopHolder(categoryId);
        Inventory inv = Bukkit.createInventory(holder, 54, CATEGORY_PREFIX + catName);
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        // Ramka górna i dolna
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        // Ramki boczne
        for (int r = 1; r < 5; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Sloty na przedmioty: 10-16, 19-25, 28-34, 37-43
        int[] itemSlots = new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < items.size() && i < itemSlots.length; i++) {
            ShopItem item = items.get(i);
            int slot = itemSlots[i];

            List<String> lore = new ArrayList<>();
            lore.add("§7Cena kupna: §a" + economyManager.format(item.getBuyPrice()) + " §7(za 1 szt.)");
            lore.add("§7Cena skupu: §c" + economyManager.format(item.getSellPrice()) + " §8(niska cena skupu)");
            lore.add("§8---------------------------------");
            lore.add("§eLPM §7- Kup §f1x §7(§a" + economyManager.format(item.getBuyPrice()) + "§7)");
            lore.add("§eShift + LPM §7- Kup §f16x §7(§a" + economyManager.format(item.getBuyPrice() * 16) + "§7)");
            lore.add("§6PPM §7- Sprzedaj §f1x §7(§c+" + economyManager.format(item.getSellPrice()) + "§7)");
            lore.add("§6Shift + PPM §7- Sprzedaj §fwszystko z eq §7(§c+" + economyManager.format(item.getSellPrice()) + " / szt.§7)");

            inv.setItem(slot, createItem(item.getMaterial(), "§e§l" + item.getDisplayName(), lore.toArray(new String[0])));
        }

        // Przyciski dolne
        // Slot 45: Powrót
        inv.setItem(45, createItem(Material.ARROW, "§e§l« Powrót do menu",
                "§7Wróć do wyboru kategorii sklepu."));

        // Slot 49: Portfel gracza
        double balance = economyManager.getPlayerBalance(player);
        inv.setItem(49, createItem(Material.GOLD_INGOT, "§6§lTwój Portfel",
                "§7Aktualne saldo: §a" + economyManager.format(balance)));

        // Slot 53: Podatek handlowy
        inv.setItem(53, createItem(Material.MAP, "§b§lPodatek Handlowy Wyspy",
                "§75% wartości każdego zakupu trafia do",
                "§7Skarbca aktualnego Władcy Handlu (Trade Sovereign)!"));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
