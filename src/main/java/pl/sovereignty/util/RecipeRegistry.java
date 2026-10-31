package pl.sovereignty.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class RecipeRegistry {
    private final Plugin plugin;
    private final CustomItemRegistry items;

    public static final List<Material> ALL_PLANKS = List.of(
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
            Material.CRIMSON_PLANKS, Material.WARPED_PLANKS
    );

    public static final List<Material> ALL_WOOL = List.of(
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL
    );

    public static final List<Material> ALL_BEDS = List.of(
            Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED,
            Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED,
            Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED,
            Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED, Material.BLACK_BED
    );

    public static final List<Material> ALL_SAPLINGS = List.of(
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
    );

    public static final List<Material> ALL_LOGS = List.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.CHERRY_LOG, Material.MANGROVE_LOG
    );

    public static final List<Material> ALL_FENCES = List.of(
            Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.BIRCH_FENCE,
            Material.JUNGLE_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE,
            Material.CHERRY_FENCE, Material.MANGROVE_FENCE, Material.BAMBOO_FENCE,
            Material.NETHER_BRICK_FENCE, Material.CRIMSON_FENCE, Material.WARPED_FENCE
    );

    public static final List<Material> ALL_BANNERS = List.of(
            Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER,
            Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER,
            Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER,
            Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
            Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER
    );

    public RecipeRegistry(Plugin plugin, CustomItemRegistry items) {
        this.plugin = plugin;
        this.items = items;
    }

    public void registerAll() {
        registerBarracksCore();
        registerSawmillTotem();
        registerMiningHead();
        registerCrateRam();
        registerCrateBallista();
        registerCrateMortar();
        registerWarCouncilTable();
        registerEngineeringWrench();
        registerMarshalMace();
        registerWarHorn();
        registerNationalVault();
        registerStateFarm();
        registerSuperFurnace();
        registerShortShield();
    }

    private void registerBarracksCore() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_barracks_core");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getBarracksCore());
            recipe.shape("WHW", "PBP", "PCP");
            recipe.setIngredient('W', new RecipeChoice.MaterialChoice(ALL_WOOL));
            recipe.setIngredient('H', Material.IRON_HELMET);
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            recipe.setIngredient('B', new RecipeChoice.MaterialChoice(ALL_BEDS));
            recipe.setIngredient('C', Material.CHEST);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury BarracksCore: " + e.getMessage());
        }
    }

    private void registerSawmillTotem() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_sawmill_totem");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getSawmillTotem());
            recipe.shape("ASA", "LCL", "BBB");
            recipe.setIngredient('A', Material.IRON_AXE);
            recipe.setIngredient('S', new RecipeChoice.MaterialChoice(ALL_SAPLINGS));
            recipe.setIngredient('L', new RecipeChoice.MaterialChoice(ALL_LOGS));
            recipe.setIngredient('C', Material.CHEST);
            recipe.setIngredient('B', Material.COBBLESTONE);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury SawmillTotem: " + e.getMessage());
        }
    }

    private void registerMiningHead() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_mining_head");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getMiningHead());
            recipe.shape("PLP", "CHC", "IFI");
            recipe.setIngredient('P', Material.IRON_PICKAXE);
            recipe.setIngredient('L', Material.LEVER);
            recipe.setIngredient('C', Material.COBBLESTONE);
            recipe.setIngredient('H', Material.HOPPER);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('F', Material.BLAST_FURNACE);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury MiningHead: " + e.getMessage());
        }
    }

    private void registerCrateRam() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_crate_ram");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getCrateRam());
            recipe.shape("PIP", "SMS", "PIP");
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('S', Material.PISTON);
            recipe.setIngredient('M', Material.MINECART);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury CrateRam: " + e.getMessage());
        }
    }

    private void registerCrateBallista() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_crate_ballista");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getCrateBallista());
            recipe.shape("CXC", "IFI", "PPP");
            recipe.setIngredient('C', Material.CHAIN);
            recipe.setIngredient('X', Material.CROSSBOW);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('F', new RecipeChoice.MaterialChoice(ALL_FENCES));
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury CrateBallista: " + e.getMessage());
        }
    }

    private void registerCrateMortar() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_crate_mortar");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getCrateMortar());
            recipe.shape("C C", "CDC", "IRI");
            recipe.setIngredient('C', Material.COPPER_BLOCK);
            recipe.setIngredient('D', Material.DISPENSER);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('R', Material.REDSTONE_BLOCK);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury CrateMortar: " + e.getMessage());
        }
    }

    private void registerWarCouncilTable() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_war_council_table");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getWarCouncilTable());
            recipe.shape("BAB", "IMI", "PPP");
            recipe.setIngredient('B', new RecipeChoice.MaterialChoice(ALL_BANNERS));
            recipe.setIngredient('A', Material.GOLDEN_APPLE);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('M', Material.MAP);
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury WarCouncilTable: " + e.getMessage());
        }
    }

    private void registerEngineeringWrench() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_engineering_wrench");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getEngineeringWrench());
            recipe.shape("II ", " C ", " S ");
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('C', Material.COPPER_INGOT);
            recipe.setIngredient('S', Material.STICK);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury EngineeringWrench: " + e.getMessage());
        }
    }

    private void registerMarshalMace() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_marshal_mace");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getMarshalMace());
            recipe.shape("GDG", " B ", " B ");
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('D', Material.DIAMOND);
            recipe.setIngredient('B', Material.BLAZE_ROD);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury MarshalMace: " + e.getMessage());
        }
    }

    private void registerWarHorn() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_war_horn");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getWarHorn());
            recipe.shape(" H ", " I ", " G ");
            recipe.setIngredient('H', Material.GOAT_HORN);
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('G', Material.GOLD_INGOT);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury WarHorn: " + e.getMessage());
        }
    }

    private void registerNationalVault() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_national_vault");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getNationalVault());
            recipe.shape("IGI", "GBG", "IOI");
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('G', Material.GOLD_INGOT);
            recipe.setIngredient('B', Material.BARREL);
            recipe.setIngredient('O', Material.IRON_BLOCK);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury NationalVault: " + e.getMessage());
        }
    }

    private void registerStateFarm() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_state_farm");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getStateFarm());
            recipe.shape("HWH", "BCB", "PPP");
            recipe.setIngredient('H', Material.IRON_HOE);
            recipe.setIngredient('W', Material.HAY_BLOCK);
            recipe.setIngredient('B', Material.WATER_BUCKET);
            recipe.setIngredient('C', new RecipeChoice.MaterialChoice(Material.BARREL, Material.CHEST));
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury StateFarm: " + e.getMessage());
        }
    }

    private void registerSuperFurnace() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_super_furnace");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getSuperFurnace());
            recipe.shape("IBI", "CFC", "SSS");
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('B', Material.BLAST_FURNACE);
            recipe.setIngredient('C', Material.COPPER_INGOT);
            recipe.setIngredient('F', Material.FURNACE);
            recipe.setIngredient('S', Material.SMOOTH_STONE);
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury SuperFurnace: " + e.getMessage());
        }
    }

    private void registerShortShield() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "recipe_short_shield");
            safeRemoveRecipe(key);
            ShapedRecipe recipe = new ShapedRecipe(key, items.getShortShield());
            recipe.shape("PIP", " P ");
            recipe.setIngredient('I', Material.IRON_INGOT);
            recipe.setIngredient('P', new RecipeChoice.MaterialChoice(ALL_PLANKS));
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().warning("Błąd rejestracji receptury ShortShield: " + e.getMessage());
        }
    }

    private void safeRemoveRecipe(NamespacedKey key) {
        try {
            Bukkit.removeRecipe(key);
        } catch (Throwable ignored) {}
    }
}
