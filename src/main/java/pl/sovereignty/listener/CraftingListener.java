package pl.sovereignty.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.util.CustomItemRegistry;
import pl.sovereignty.util.RecipeRegistry;

import java.util.List;

public class CraftingListener implements Listener {
    private final Plugin plugin;
    private final CustomItemRegistry items;

    private static final String[] RECIPE_KEYS = new String[]{
            "recipe_barracks_core",
            "recipe_sawmill_totem",
            "recipe_mining_head",
            "recipe_crate_ram",
            "recipe_crate_ballista",
            "recipe_crate_mortar",
            "recipe_war_council_table",
            "recipe_engineering_wrench",
            "recipe_marshal_mace",
            "recipe_war_horn",
            "recipe_national_vault",
            "recipe_super_furnace",
            "recipe_short_shield"
    };

    public CraftingListener(Plugin plugin, CustomItemRegistry items) {
        this.plugin = plugin;
        this.items = items;
    }

    /**
     * Automatyczne odblokowanie receptur w Księdze Receptur gracza po wejściu na serwer.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (String keyStr : RECIPE_KEYS) {
            try {
                player.discoverRecipe(new NamespacedKey(plugin, keyStr));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Gwarancja pojawienia się wyniku craftingu w stole rzemieślniczym 3x3.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length != 9) return; // Wymaga siatki stołu 3x3

        ItemStack matched = matchCustomRecipe(matrix);
        if (matched != null) {
            inv.setResult(matched);
        }
    }

    private ItemStack matchCustomRecipe(ItemStack[] m) {
        // 1. Engineering Wrench: I I _ / _ C _ / _ S _
        if (isType(m[0], Material.IRON_INGOT) && isType(m[1], Material.IRON_INGOT) && isEmpty(m[2])
                && isEmpty(m[3]) && isType(m[4], Material.COPPER_INGOT) && isEmpty(m[5])
                && isEmpty(m[6]) && isType(m[7], Material.STICK) && isEmpty(m[8])) {
            return items.getEngineeringWrench();
        }

        // 2. Marshal Mace: G D G / _ B _ / _ B _
        if (isType(m[0], Material.GOLD_INGOT) && isType(m[1], Material.DIAMOND) && isType(m[2], Material.GOLD_INGOT)
                && isEmpty(m[3]) && isType(m[4], Material.BLAZE_ROD) && isEmpty(m[5])
                && isEmpty(m[6]) && isType(m[7], Material.BLAZE_ROD) && isEmpty(m[8])) {
            return items.getMarshalMace();
        }

        // 3. War Horn: _ H _ / _ I _ / _ G _
        if (isEmpty(m[0]) && isType(m[1], Material.GOAT_HORN) && isEmpty(m[2])
                && isEmpty(m[3]) && isType(m[4], Material.IRON_INGOT) && isEmpty(m[5])
                && isEmpty(m[6]) && isType(m[7], Material.GOLD_INGOT) && isEmpty(m[8])) {
            return items.getWarHorn();
        }

        // 4. Mining Head: P L P / C H C / I F I
        if (isType(m[0], Material.IRON_PICKAXE) && isType(m[1], Material.LEVER) && isType(m[2], Material.IRON_PICKAXE)
                && isType(m[3], Material.COBBLESTONE) && isType(m[4], Material.HOPPER) && isType(m[5], Material.COBBLESTONE)
                && isType(m[6], Material.IRON_INGOT) && isType(m[7], Material.BLAST_FURNACE) && isType(m[8], Material.IRON_INGOT)) {
            return items.getMiningHead();
        }

        // 5. Crate Mortar: C _ C / C D C / I R I
        if (isType(m[0], Material.COPPER_BLOCK) && isEmpty(m[1]) && isType(m[2], Material.COPPER_BLOCK)
                && isType(m[3], Material.COPPER_BLOCK) && isType(m[4], Material.DISPENSER) && isType(m[5], Material.COPPER_BLOCK)
                && isType(m[6], Material.IRON_INGOT) && isType(m[7], Material.REDSTONE_BLOCK) && isType(m[8], Material.IRON_INGOT)) {
            return items.getCrateMortar();
        }

        // 6. Crate Ram: P I P / S M S / P I P
        if (isPlank(m[0]) && isType(m[1], Material.IRON_INGOT) && isPlank(m[2])
                && isType(m[3], Material.PISTON) && isType(m[4], Material.MINECART) && isType(m[5], Material.PISTON)
                && isPlank(m[6]) && isType(m[7], Material.IRON_INGOT) && isPlank(m[8])) {
            return items.getCrateRam();
        }

        // 7. Crate Ballista: C X C / I F I / P P P
        if (isType(m[0], Material.CHAIN) && isType(m[1], Material.CROSSBOW) && isType(m[2], Material.CHAIN)
                && isType(m[3], Material.IRON_INGOT) && isFence(m[4]) && isType(m[5], Material.IRON_INGOT)
                && isPlank(m[6]) && isPlank(m[7]) && isPlank(m[8])) {
            return items.getCrateBallista();
        }

        // 8. Barracks Core: W H W / P B P / P C P
        if (isWool(m[0]) && isType(m[1], Material.IRON_HELMET) && isWool(m[2])
                && isPlank(m[3]) && isBed(m[4]) && isPlank(m[5])
                && isPlank(m[6]) && isType(m[7], Material.CHEST) && isPlank(m[8])) {
            return items.getBarracksCore();
        }

        // 9. Sawmill Totem: A S A / L C L / B B B
        if (isType(m[0], Material.IRON_AXE) && isSapling(m[1]) && isType(m[2], Material.IRON_AXE)
                && isLog(m[3]) && isType(m[4], Material.CHEST) && isLog(m[5])
                && isType(m[6], Material.COBBLESTONE) && isType(m[7], Material.COBBLESTONE) && isType(m[8], Material.COBBLESTONE)) {
            return items.getSawmillTotem();
        }

        // 10. War Council Table: B A B / I M I / P P P
        if (isBanner(m[0]) && isType(m[1], Material.GOLDEN_APPLE) && isBanner(m[2])
                && isType(m[3], Material.IRON_INGOT) && isType(m[4], Material.MAP) && isType(m[5], Material.IRON_INGOT)
                && isPlank(m[6]) && isPlank(m[7]) && isPlank(m[8])) {
            return items.getWarCouncilTable();
        }

        // 11. National Vault: I G I / G B G / I O I
        if (isType(m[0], Material.IRON_INGOT) && isType(m[1], Material.GOLD_INGOT) && isType(m[2], Material.IRON_INGOT)
                && isType(m[3], Material.GOLD_INGOT) && isType(m[4], Material.BARREL) && isType(m[5], Material.GOLD_INGOT)
                && isType(m[6], Material.IRON_INGOT) && isType(m[7], Material.IRON_BLOCK) && isType(m[8], Material.IRON_INGOT)) {
            return items.getNationalVault();
        }

        return null;
    }

    private boolean isEmpty(ItemStack is) {
        return is == null || is.getType() == Material.AIR;
    }

    private boolean isType(ItemStack is, Material mat) {
        return is != null && is.getType() == mat;
    }

    private boolean isPlank(ItemStack is) {
        return is != null && RecipeRegistry.ALL_PLANKS.contains(is.getType());
    }

    private boolean isWool(ItemStack is) {
        return is != null && RecipeRegistry.ALL_WOOL.contains(is.getType());
    }

    private boolean isBed(ItemStack is) {
        return is != null && RecipeRegistry.ALL_BEDS.contains(is.getType());
    }

    private boolean isSapling(ItemStack is) {
        return is != null && RecipeRegistry.ALL_SAPLINGS.contains(is.getType());
    }

    private boolean isLog(ItemStack is) {
        return is != null && RecipeRegistry.ALL_LOGS.contains(is.getType());
    }

    private boolean isFence(ItemStack is) {
        return is != null && RecipeRegistry.ALL_FENCES.contains(is.getType());
    }

    private boolean isBanner(ItemStack is) {
        return is != null && RecipeRegistry.ALL_BANNERS.contains(is.getType());
    }
}
