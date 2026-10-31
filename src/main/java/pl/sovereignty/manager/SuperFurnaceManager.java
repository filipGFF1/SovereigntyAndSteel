package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.gui.SuperFurnaceGui;
import pl.sovereignty.gui.SuperFurnaceHolder;
import pl.sovereignty.util.CustomItemRegistry;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SuperFurnaceManager implements Listener {
    private final Plugin plugin;
    private final CustomItemRegistry items;
    private final Set<Location> superFurnaces = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SuperFurnaceManager(Plugin plugin, CustomItemRegistry items) {
        this.plugin = plugin;
        this.items = items;
        loadSuperFurnaces();
    }

    public boolean isSuperFurnace(Location loc) {
        return superFurnaces.contains(loc);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customId = items.getCustomItemId(item);
        if ("super_furnace".equals(customId)) {
            Location loc = event.getBlockPlaced().getLocation();
            superFurnaces.add(loc);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&l[SUPER PIEC] &aPostawiono Super Piec Przemysłowy! Kliknij PPM, aby otworzyć potrójny panel hutniczy."));
            saveSuperFurnaces();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (superFurnaces.remove(loc)) {
            event.setDropItems(false);
            if (loc.getWorld() != null) {
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), items.getSuperFurnace());
            }
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Zdemontowano Super Piec Przemysłowy.");
            saveSuperFurnaces();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (superFurnaces.contains(block.getLocation())) {
            event.setCancelled(true);
            SuperFurnaceGui.open(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SuperFurnaceHolder)) return;

        int rawSlot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();

        // Kliknięcie w górny inwentarz pieca
        if (rawSlot >= 0 && rawSlot < 27) {
            if (rawSlot == SuperFurnaceGui.SLOT_ACTION) {
                event.setCancelled(true);
                SuperFurnaceGui.handleSmelt(player, event.getInventory());
                return;
            }

            // Zablokuj klikanie w szklane panele dekoracyjne
            if (!SuperFurnaceGui.INTERACTABLE_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SuperFurnaceHolder)) return;

        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        // Bezpieczny zwrot surowców z pieca po zamknięciu GUI
        int[] returnSlots = {
                SuperFurnaceGui.SLOT_IN_1, SuperFurnaceGui.SLOT_IN_2, SuperFurnaceGui.SLOT_IN_3,
                SuperFurnaceGui.SLOT_FUEL,
                SuperFurnaceGui.SLOT_OUT_1, SuperFurnaceGui.SLOT_OUT_2, SuperFurnaceGui.SLOT_OUT_3
        };

        for (int slot : returnSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack rem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rem);
                }
                inv.setItem(slot, null);
            }
        }
    }

    public void saveSuperFurnaces() {
        File file = new File(plugin.getDataFolder(), "super_furnaces.yml");
        FileConfiguration config = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (Location loc : superFurnaces) {
            if (loc.getWorld() != null) {
                list.add(loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
            }
        }
        config.set("furnaces", list);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException ignored) {}
    }

    public void loadSuperFurnaces() {
        File file = new File(plugin.getDataFolder(), "super_furnaces.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> list = config.getStringList("furnaces");
        superFurnaces.clear();
        for (String s : list) {
            try {
                String[] parts = s.split(":");
                World w = Bukkit.getWorld(parts[0]);
                if (w != null) {
                    superFurnaces.add(new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                }
            } catch (Exception ignored) {}
        }
    }
}
