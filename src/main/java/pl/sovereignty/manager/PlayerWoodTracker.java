package pl.sovereignty.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menedżer ochrony drewna postawionego przez graczy.
 * Zapobiega ścinaniu przez Tartaki konstrukcji budowlanych, domów, belek oraz
 * jakichkolwiek bloków drewna postawionych przez graczy.
 */
public class PlayerWoodTracker implements Listener {

    public static final String METADATA_KEY = "sovereignty_placed_wood";

    private final Plugin plugin;
    private final Set<String> playerPlacedWood = ConcurrentHashMap.newKeySet();

    public PlayerWoodTracker(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    public static String toKey(Block block) {
        if (block == null || block.getWorld() == null) return "";
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    public static String toKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public boolean isWood(Material mat) {
        if (mat == null) return false;
        return mat.name().endsWith("_LOG")
                || mat.name().endsWith("_WOOD")
                || Tag.LOGS.isTagged(mat)
                || mat == Material.BAMBOO
                || mat == Material.BAMBOO_BLOCK;
    }

    public boolean isPlantableSoil(Material mat) {
        if (mat == null) return false;
        return mat == Material.DIRT
                || mat == Material.GRASS_BLOCK
                || mat == Material.COARSE_DIRT
                || mat == Material.PODZOL
                || mat == Material.ROOTED_DIRT
                || mat == Material.MUD
                || mat == Material.MYCELIUM
                || mat == Material.MOSS_BLOCK;
    }

    public boolean isPlayerPlaced(Block block) {
        if (block == null) return false;
        if (block.hasMetadata(METADATA_KEY)) return true;
        return playerPlacedWood.contains(toKey(block));
    }

    /**
     * Sprawdza, czy blok sąsiaduje z typowymi elementami budowlanymi gracza
     * (deski, schodki, płyty, drzwi, płotki, szyby, pochodnie, skrzynie, kamień).
     */
    public boolean isBuildingSurrounding(Block block) {
        if (block == null) return false;
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            Block adj = block.getRelative(face);
            Material m = adj.getType();
            if (m.isAir()) continue;
            String name = m.name();
            if (name.contains("PLANKS")
                    || name.contains("SLAB")
                    || name.contains("STAIRS")
                    || name.contains("DOOR")
                    || name.contains("TRAPDOOR")
                    || name.contains("FENCE")
                    || name.contains("GATE")
                    || name.contains("WALL")
                    || name.contains("GLASS")
                    || name.contains("TORCH")
                    || name.contains("LANTERN")
                    || name.contains("CHEST")
                    || name.contains("BARREL")
                    || name.contains("BRICK")
                    || name.contains("COBBLE")
                    || name.contains("STONE_")
                    || name.contains("BED")
                    || name.contains("CARPET")
                    || name.contains("WOOL")
                    || name.contains("CONCRETE")
                    || m == Material.CRAFTING_TABLE
                    || m == Material.FURNACE
                    || m == Material.BLAST_FURNACE
                    || m == Material.SMOKER
                    || m == Material.ANVIL
                    || m == Material.HOPPER) {
                return true;
            }
        }
        return false;
    }

    /**
     * Weryfikuje, czy blok pnia jest częścią naturalnego, rosnącego drzewa,
     * a nie elementem domostwa lub konstrukcji gracza.
     */
    public boolean isNaturalTree(Block woodBlock) {
        if (woodBlock == null) return false;

        // 1. Sprawdź, czy blok został zarejestrowany jako postawiony przez gracza
        if (isPlayerPlaced(woodBlock)) return false;

        // 2. Ciosane drewno oraz bloki bambusa to zawsze elementy budowlane
        Material woodMat = woodBlock.getType();
        if (woodMat == Material.BAMBOO_BLOCK || woodMat.name().contains("STRIPPED_")) {
            return false;
        }

        // 3. Sprawdź, czy bezpośrednio nie styka się z elementami architektonicznymi
        if (isBuildingSurrounding(woodBlock)) {
            return false;
        }

        // 4. Zejdź w dół pnia do samej podstawy – drzewo musi rosnąć na naturalnej glebie
        Block base = woodBlock;
        int downSteps = 0;
        while (downSteps < 35) {
            Block below = base.getRelative(BlockFace.DOWN);
            Material belowType = below.getType();
            if (belowType.name().endsWith("_LOG") || belowType.name().endsWith("_WOOD") || Tag.LOGS.isTagged(belowType)) {
                if (isPlayerPlaced(below) || isBuildingSurrounding(below)) return false;
                base = below;
                downSteps++;
            } else {
                break;
            }
        }

        Block ground = base.getRelative(BlockFace.DOWN);
        if (!isPlantableSoil(ground.getType())) {
            return false;
        }

        // 5. Wyjdź w górę pnia – drzewo musi mieć koronę liści
        Block top = woodBlock;
        int upSteps = 0;
        while (upSteps < 35) {
            Block above = top.getRelative(BlockFace.UP);
            Material aboveType = above.getType();
            if (aboveType.name().endsWith("_LOG") || aboveType.name().endsWith("_WOOD") || Tag.LOGS.isTagged(aboveType)) {
                if (isPlayerPlaced(above) || isBuildingSurrounding(above)) return false;
                top = above;
                upSteps++;
            } else {
                break;
            }
        }

        // 6. Sprawdź obecność liści w koronie (promień 2 klocków poziomo, 3 pionowo od czubka pnia)
        int leafCount = 0;
        boolean hasNaturalLeaves = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    Block b = top.getRelative(dx, dy, dz);
                    if (Tag.LEAVES.isTagged(b.getType())) {
                        leafCount++;
                        if (b.getBlockData() instanceof Leaves leavesData) {
                            if (!leavesData.isPersistent()) {
                                hasNaturalLeaves = true;
                            }
                        }
                    }
                }
            }
        }

        // Prawdziwe drzewo ma liście w koronie, z czego przynajmniej część to liście naturalne (niepostawione nożycami)
        return leafCount >= 2 && hasNaturalLeaves;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (isWood(block.getType())) {
            block.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            playerPlacedWood.add(toKey(block));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isWood(block.getType()) || playerPlacedWood.contains(toKey(block))) {
            block.removeMetadata(METADATA_KEY, plugin);
            playerPlacedWood.remove(toKey(block));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        // Gdy drzewo rośnie z sadzonki, jego bloki stają się naturalne
        for (BlockState state : event.getBlocks()) {
            Block b = state.getBlock();
            b.removeMetadata(METADATA_KEY, plugin);
            playerPlacedWood.remove(toKey(b));
        }
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "player_wood.yml");
        FileConfiguration config = new YamlConfiguration();
        config.set("placed_wood", new ArrayList<>(playerPlacedWood));
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException ignored) {}
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "player_wood.yml");
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> list = config.getStringList("placed_wood");
        playerPlacedWood.clear();
        playerPlacedWood.addAll(list);
    }
}
