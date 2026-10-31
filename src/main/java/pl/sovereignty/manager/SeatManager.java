package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeatManager {
    public static final String SEAT_TAG = "sovereignty_seat";

    private final Plugin plugin;
    private final Map<UUID, UUID> playerToStandMap = new ConcurrentHashMap<>();
    private final Map<Block, UUID> blockToPlayerMap = new ConcurrentHashMap<>();

    public SeatManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSitting(Player player) {
        return playerToStandMap.containsKey(player.getUniqueId());
    }

    public boolean isSeatEntity(Entity entity) {
        if (entity == null) return false;
        return entity.getScoreboardTags().contains(SEAT_TAG) || playerToStandMap.containsValue(entity.getUniqueId());
    }

    public boolean isBlockOccupied(Block block) {
        if (block == null) return false;
        UUID occupierId = blockToPlayerMap.get(block);
        if (occupierId == null) return false;

        Player player = Bukkit.getPlayer(occupierId);
        if (player == null || !player.isOnline() || !isSitting(player)) {
            blockToPlayerMap.remove(block);
            return false;
        }
        return true;
    }

    /**
     * Sadza gracza na wyznaczonej pozycji
     */
    public boolean sit(Player player, Location location, Block chairBlock) {
        if (player == null || !player.isOnline()) return false;

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        if (isSitting(player)) {
            unsit(player, false);
        }

        if (chairBlock != null && isBlockOccupied(chairBlock)) {
            player.sendMessage(ChatColor.RED + "To miejsce jest już zajęte!");
            return false;
        }

        World world = location.getWorld();
        if (world == null) return false;

        ArmorStand stand = world.spawn(location, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setSmall(true);
            s.setMarker(false); // Marker MUSI być false, aby byt mógł być ujeżdżany przez gracza!
            s.setBasePlate(false);
            s.setArms(false);
            s.setPersistent(false);
            s.setCollidable(false);
            s.addScoreboardTag(SEAT_TAG);
        });

        boolean mounted = stand.addPassenger(player);
        if (!mounted) {
            stand.remove();
            player.sendMessage(ChatColor.RED + "Nie udało się usiąść w tym miejscu!");
            return false;
        }

        playerToStandMap.put(player.getUniqueId(), stand.getUniqueId());
        if (chairBlock != null) {
            blockToPlayerMap.put(chairBlock, player.getUniqueId());
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Usiadłeś. Naciśnij &eSHIFT&7, aby wstać."));
        player.playSound(location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.6f, 1.2f);
        return true;
    }

    /**
     * Wstanie z miejsca
     */
    public void unsit(Player player, boolean safeTeleport) {
        if (player == null) return;
        UUID standId = playerToStandMap.remove(player.getUniqueId());
        blockToPlayerMap.values().removeIf(id -> id.equals(player.getUniqueId()));

        if (standId != null) {
            Entity stand = Bukkit.getEntity(standId);
            if (stand != null) {
                stand.eject();
                stand.remove();
            }
        }

        if (player.isOnline()) {
            if (player.getVehicle() != null) {
                player.leaveVehicle();
            }
            if (safeTeleport) {
                Location current = player.getLocation();
                Location safeLoc = current.clone().add(0, 0.85, 0);
                if (safeLoc.getBlock().isPassable()) {
                    player.teleport(safeLoc);
                }
            }
        }
    }

    public void handleDismount(Player player, Entity seatEntity) {
        if (player == null) return;
        playerToStandMap.remove(player.getUniqueId());
        blockToPlayerMap.values().removeIf(id -> id.equals(player.getUniqueId()));

        if (seatEntity != null && isSeatEntity(seatEntity)) {
            seatEntity.eject();
            seatEntity.remove();
        }

        Location current = player.getLocation();
        Location safeLoc = current.clone().add(0, 0.85, 0);
        if (safeLoc.getBlock().isPassable()) {
            player.teleport(safeLoc);
        }
        player.sendMessage(ChatColor.GRAY + "Wstałeś z miejsca.");
    }

    /**
     * Czyści wszystkie stojaki krzeseł przy wyłączaniu serwera lub przeładowaniu
     */
    public void cleanupAll() {
        for (UUID standId : playerToStandMap.values()) {
            Entity stand = Bukkit.getEntity(standId);
            if (stand != null) {
                stand.eject();
                stand.remove();
            }
        }
        playerToStandMap.clear();
        blockToPlayerMap.clear();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                if (entity.getScoreboardTags().contains(SEAT_TAG)) {
                    entity.eject();
                    entity.remove();
                }
            }
        }
    }
}
