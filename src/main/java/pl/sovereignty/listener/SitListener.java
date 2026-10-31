package pl.sovereignty.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.sovereignty.manager.SeatManager;

public class SitListener implements Listener {
    private final SeatManager seatManager;

    public SitListener(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        if (player.isInsideVehicle() || seatManager.isSitting(player)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Jeśli gracz trzyma blok i klika by go postawić, nie siadajmy
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isBlock() && event.getBlockFace() != BlockFace.DOWN) {
            return;
        }

        // 1. Schodki (Stairs)
        if (block.getBlockData() instanceof Stairs stairs) {
            if (stairs.getHalf() != Stairs.Half.BOTTOM) return;
            if (!block.getRelative(BlockFace.UP).isPassable()) return;

            // Obliczenie kąta patrzenia dopasowanego do schodków
            float yaw = switch (stairs.getFacing()) {
                case NORTH -> 180.0f;
                case SOUTH -> 0.0f;
                case WEST -> 90.0f;
                case EAST -> 270.0f;
                default -> player.getLocation().getYaw();
            };

            Location sitLoc = new Location(block.getWorld(),
                    block.getX() + 0.5,
                    block.getY() - 0.25,
                    block.getZ() + 0.5,
                    yaw,
                    0.0f);

            if (seatManager.sit(player, sitLoc, block)) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. Dolna płyta (Bottom Slab)
        if (block.getBlockData() instanceof Slab slab) {
            if (slab.getType() != Slab.Type.BOTTOM) return;
            if (!block.getRelative(BlockFace.UP).isPassable()) return;

            Location sitLoc = new Location(block.getWorld(),
                    block.getX() + 0.5,
                    block.getY() - 0.25,
                    block.getZ() + 0.5,
                    player.getLocation().getYaw(),
                    0.0f);

            if (seatManager.sit(player, sitLoc, block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (seatManager.isSeatEntity(event.getDismounted())) {
            seatManager.handleDismount(player, event.getDismounted());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        seatManager.unsit(event.getPlayer(), false);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        seatManager.unsit(event.getEntity(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (seatManager.isSitting(event.getPlayer())) {
            seatManager.unsit(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (seatManager.isBlockOccupied(block)) {
            // Bezpieczne zwolnienie krzesła jeśli ktoś zniszczył blok
            seatManager.cleanupAll();
        }
    }
}
