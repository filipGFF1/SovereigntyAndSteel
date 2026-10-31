package pl.sovereignty.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Umożliwia wydobywanie spawnerów za pomocą narzędzia z enchantem Silk Touch (Jedwabny Dotyk)
 * w każdym miejscu, w którym gracz ma uprawnienia do niszczenia bloków (dziczyzna, własny claim itp.).
 * Zachowuje dokładny typ moba przypisany do spawnera.
 */
public class SpawnerSilkTouchListener implements Listener {

    private final Plugin plugin;
    private final NamespacedKey spawnerTypeKey;

    public SpawnerSilkTouchListener(Plugin plugin) {
        this.plugin = plugin;
        this.spawnerTypeKey = new NamespacedKey(plugin, "spawner_entity_type");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        // Odczyt typu moba ze spawnera
        EntityType spawnedType = EntityType.PIG;
        if (block.getState() instanceof CreatureSpawner spawner) {
            EntityType type = spawner.getSpawnedType();
            if (type != null) {
                spawnedType = type;
            }
        }

        // Zablokuj standardowe zachowanie (brak expa i domyślnego dropu)
        event.setExpToDrop(0);
        event.setDropItems(false);

        // Utworzenie przedmiotu spawnera z zapisanym typem moba
        ItemStack spawnerItem = createSpawnerItem(spawnedType);

        // Upuszczenie przedmiotu w świecie
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        if (block.getWorld() != null) {
            block.getWorld().dropItemNaturally(dropLoc, spawnerItem);
        }

        player.sendMessage(ChatColor.GREEN + "Pomyślnie wydobyto spawner ("
                + ChatColor.GOLD + getMobDisplayName(spawnedType)
                + ChatColor.GREEN + ") za pomocą Silk Touch!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType() != Material.SPAWNER) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        EntityType entityType = null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.getPersistentDataContainer().has(spawnerTypeKey, PersistentDataType.STRING)) {
                String typeStr = meta.getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
                if (typeStr != null) {
                    try {
                        entityType = EntityType.valueOf(typeStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (entityType == null && meta instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof CreatureSpawner cs) {
                    entityType = cs.getSpawnedType();
                }
            }
        }

        if (entityType != null) {
            final EntityType finalType = entityType;
            if (placedBlock.getState() instanceof CreatureSpawner placedSpawner) {
                placedSpawner.setSpawnedType(finalType);
                placedSpawner.update(true, false);
            }

            // Oraz 1 tick opóźnienia, aby zapewnić pełne załadowanie TileEntity w silniku
            final Location loc = placedBlock.getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (loc.getBlock().getState() instanceof CreatureSpawner delayedSpawner) {
                    delayedSpawner.setSpawnedType(finalType);
                    delayedSpawner.update(true, false);
                }
            });
        }
    }

    private ItemStack createSpawnerItem(EntityType entityType) {
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String mobName = getMobDisplayName(entityType);
            meta.setDisplayName(ChatColor.YELLOW + "Spawner (" + ChatColor.GOLD + mobName + ChatColor.YELLOW + ")");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Typ moba: " + ChatColor.WHITE + mobName,
                    ChatColor.DARK_GRAY + "Wydobyto za pomocą: Jedwabny Dotyk (Silk Touch)"
            ));

            meta.getPersistentDataContainer().set(spawnerTypeKey, PersistentDataType.STRING, entityType.name());

            if (meta instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof CreatureSpawner cs) {
                    cs.setSpawnedType(entityType);
                    bsm.setBlockState(cs);
                }
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getMobDisplayName(EntityType type) {
        if (type == null) return "Świnia";
        switch (type) {
            case ZOMBIE: return "Zombie";
            case SKELETON: return "Szkielet";
            case SPIDER: return "Pająk";
            case CAVE_SPIDER: return "Pająk jaskiniowy";
            case BLAZE: return "Płomyk";
            case CREEPER: return "Creeper";
            case ENDERMAN: return "Enderman";
            case SLIME: return "Szlam";
            case MAGMA_CUBE: return "Kostka magmy";
            case SILVERFISH: return "Rybik cukrowy";
            case WITCH: return "Wiedźma";
            case WITHER_SKELETON: return "Witherowy szkielet";
            case PIG: return "Świnia";
            case COW: return "Krowa";
            case SHEEP: return "Owca";
            case CHICKEN: return "Kurczak";
            case IRON_GOLEM: return "Żelazny golem";
            case VILLAGER: return "Osadnik";
            case DROWNED: return "Utopiec";
            case PILLAGER: return "Rozbójnik";
            case RAVAGER: return "Dewastator";
            case EVOKER: return "Przywoływacz";
            case VINDICATOR: return "Obrońca";
            case GUARDIAN: return "Strażnik morski";
            case ELDER_GUARDIAN: return "Starszy strażnik";
            case PHANTOM: return "Fantom";
            case SHULKER: return "Shulker";
            case HOGLIN: return "Hoglin";
            case PIGLIN: return "Piglin";
            case PIGLIN_BRUTE: return "Okrutny piglin";
            case ZOMBIFIED_PIGLIN: return "Zombifikowany piglin";
            case BREEZE: return "Bryza";
            case BOGGED: return "Bagnisko";
            default:
                String name = type.name().toLowerCase().replace('_', ' ');
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
