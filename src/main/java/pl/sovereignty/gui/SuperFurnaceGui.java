package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SuperFurnaceGui {
    public static final int SLOT_IN_1 = 10;
    public static final int SLOT_IN_2 = 11;
    public static final int SLOT_IN_3 = 12;

    public static final int SLOT_FUEL = 21;
    public static final int SLOT_ACTION = 22;

    public static final int SLOT_OUT_1 = 14;
    public static final int SLOT_OUT_2 = 15;
    public static final int SLOT_OUT_3 = 16;

    public static final Set<Integer> INTERACTABLE_SLOTS = Set.of(
            SLOT_IN_1, SLOT_IN_2, SLOT_IN_3,
            SLOT_FUEL, SLOT_ACTION,
            SLOT_OUT_1, SLOT_OUT_2, SLOT_OUT_3
    );

    public static void open(Player player) {
        SuperFurnaceHolder holder = new SuperFurnaceHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.translateAlternateColorCodes('&', "&8» &6&lSuper Piec Przemysłowy"));
        holder.setInventory(inv);

        ItemStack deco = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack inputBorder = createItem(Material.ORANGE_STAINED_GLASS_PANE, "&6&lWsad Surowców (Wymagany ten sam typ!)");
        ItemStack outputBorder = createItem(Material.YELLOW_STAINED_GLASS_PANE, "&e&lWynik Przetopu (Potrójny Urobek)");

        for (int i = 0; i < 27; i++) {
            if (!INTERACTABLE_SLOTS.contains(i)) {
                inv.setItem(i, deco);
            }
        }

        inv.setItem(1, inputBorder);
        inv.setItem(2, inputBorder);
        inv.setItem(3, inputBorder);

        inv.setItem(5, outputBorder);
        inv.setItem(6, outputBorder);
        inv.setItem(7, outputBorder);

        ItemStack button = createItem(Material.CAMPFIRE, "&6&lRozpocznij Przetapianie 3x", Arrays.asList(
                "&7Włóż &e3 sztuki tego samego surowca &7do slotów po lewej,",
                "&7uzupełnij paliwo poniżej i kliknij, aby błyskawicznie",
                "&7przetopić wszystkie 3 sztuki naraz!",
                "",
                "&a» Kliknij, aby przetopić «"
        ));
        inv.setItem(SLOT_ACTION, button);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    public static void handleSmelt(Player player, Inventory inv) {
        ItemStack in1 = inv.getItem(SLOT_IN_1);
        ItemStack in2 = inv.getItem(SLOT_IN_2);
        ItemStack in3 = inv.getItem(SLOT_IN_3);

        if (isEmpty(in1) || isEmpty(in2) || isEmpty(in3)) {
            player.sendMessage(ChatColor.RED + "[SUPER PIEC] Wymagane są surowce we wszystkich 3 slotach wsadowych!");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
            return;
        }

        // Rygorystyczny warunek: 3 rzeczy tego samego rodzaju!
        if (in1.getType() != in2.getType() || in2.getType() != in3.getType()) {
            player.sendMessage(ChatColor.RED + "[SUPER PIEC] Błąd hutniczy! Wszystkie 3 sloty muszą zawierać surowiec TEGO SAMEGO RODZAJU!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, 0.8f, 0.8f);
            return;
        }

        ItemStack resultSample = getSmeltResult(in1.getType());
        if (resultSample == null) {
            player.sendMessage(ChatColor.RED + "[SUPER PIEC] Tego przedmiotu nie można przetopić w piecu hutniczym!");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.0f);
            return;
        }

        ItemStack fuel = inv.getItem(SLOT_FUEL);
        if (isEmpty(fuel) || !isFuel(fuel.getType())) {
            player.sendMessage(ChatColor.RED + "[SUPER PIEC] Włóż odpowiednie paliwo (węgiel, kęs węgla, wiadro lawy) do dolnego slotu!");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.0f);
            return;
        }

        // Sprawdź czy sloty wyjściowe mogą pomieścić urobek
        if (!canFitOutput(inv, SLOT_OUT_1, resultSample)
                || !canFitOutput(inv, SLOT_OUT_2, resultSample)
                || !canFitOutput(inv, SLOT_OUT_3, resultSample)) {
            player.sendMessage(ChatColor.RED + "[SUPER PIEC] Sloty wyjściowe są pełne! Odbierz przetopiony urobek.");
            return;
        }

        // Zużyj wsad
        consumeOne(inv, SLOT_IN_1);
        consumeOne(inv, SLOT_IN_2);
        consumeOne(inv, SLOT_IN_3);

        // Zużyj paliwo
        if (fuel.getType() == Material.LAVA_BUCKET) {
            inv.setItem(SLOT_FUEL, new ItemStack(Material.BUCKET));
        } else {
            consumeOne(inv, SLOT_FUEL);
        }

        // Dodaj potrójny wynik!
        addOutput(inv, SLOT_OUT_1, resultSample.clone());
        addOutput(inv, SLOT_OUT_2, resultSample.clone());
        addOutput(inv, SLOT_OUT_3, resultSample.clone());

        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
        player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3, 0.05);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6&l[SUPER PIEC] &aPomyślnie przetopiono &e3x " + in1.getType().name()
                        + " &aw &f3x " + resultSample.getType().name() + "&a!"));
    }

    private static void consumeOne(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null) return;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            inv.setItem(slot, null);
        }
    }

    private static boolean canFitOutput(Inventory inv, int slot, ItemStack toAdd) {
        ItemStack current = inv.getItem(slot);
        if (isEmpty(current)) return true;
        if (current.isSimilar(toAdd)) {
            return current.getAmount() + toAdd.getAmount() <= current.getMaxStackSize();
        }
        return false;
    }

    private static void addOutput(Inventory inv, int slot, ItemStack toAdd) {
        ItemStack current = inv.getItem(slot);
        if (isEmpty(current)) {
            inv.setItem(slot, toAdd);
        } else if (current.isSimilar(toAdd)) {
            current.setAmount(current.getAmount() + toAdd.getAmount());
        }
    }

    public static boolean isFuel(Material mat) {
        String name = mat.name();
        return mat == Material.COAL || mat == Material.CHARCOAL || mat == Material.COAL_BLOCK
                || mat == Material.LAVA_BUCKET || mat == Material.BLAZE_ROD
                || name.endsWith("_LOG") || name.endsWith("_PLANKS") || mat == Material.BAMBOO_BLOCK;
    }

    public static ItemStack getSmeltResult(Material input) {
        // Sprawdź podstawowe i najczęstsze rudy / surowce
        return switch (input) {
            case RAW_IRON, IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case RAW_COPPER, COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT);
            case RAW_GOLD, GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
            case COBBLESTONE -> new ItemStack(Material.STONE);
            case COBBLED_DEEPSLATE -> new ItemStack(Material.DEEPSLATE);
            case STONE -> new ItemStack(Material.SMOOTH_STONE);
            case SAND, RED_SAND -> new ItemStack(Material.GLASS);
            case CLAY_BALL -> new ItemStack(Material.BRICK);
            case NETHERRACK -> new ItemStack(Material.NETHER_BRICK);
            case KELP -> new ItemStack(Material.DRIED_KELP);
            case WET_SPONGE -> new ItemStack(Material.SPONGE);
            case PORKCHOP -> new ItemStack(Material.COOKED_PORKCHOP);
            case BEEF -> new ItemStack(Material.COOKED_BEEF);
            case CHICKEN -> new ItemStack(Material.COOKED_CHICKEN);
            case MUTTON -> new ItemStack(Material.COOKED_MUTTON);
            case COD -> new ItemStack(Material.COOKED_COD);
            case SALMON -> new ItemStack(Material.COOKED_SALMON);
            case POTATO -> new ItemStack(Material.BAKED_POTATO);
            default -> {
                // Sprawdzenie ogólnych receptur piecowych
                Iterator<Recipe> it = Bukkit.recipeIterator();
                while (it.hasNext()) {
                    Recipe r = it.next();
                    if (r instanceof FurnaceRecipe fr) {
                        if (fr.getInputChoice().test(new ItemStack(input))) {
                            yield fr.getResult().clone();
                        }
                    }
                }
                yield null;
            }
        };
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }
}
