package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.sovereignty.manager.MilitaryManager;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.manager.PaoloManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.util.CustomItemRegistry;

public class MilitaryCommandListener implements Listener {
    private final MilitaryManager militaryManager;
    private final PaoloManager paoloManager;
    private final NationManager nationManager;
    private final CustomItemRegistry items;

    public MilitaryCommandListener(MilitaryManager militaryManager, PaoloManager paoloManager, NationManager nationManager, CustomItemRegistry items) {
        this.militaryManager = militaryManager;
        this.paoloManager = paoloManager;
        this.nationManager = nationManager;
        this.items = items;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!hand.hasItemMeta()) return;
        String itemId = items.getCustomItemId(hand);
        if (itemId == null) return;

        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());

        // Róg Bojowy: zmiana szyku
        if ("war_horn".equals(itemId) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby używać Rogu Bojowego!");
                return;
            }
            militaryManager.cycleFormation(player, nation);
            return;
        }

        // Buława Marszałkowska: marsz na cel lub zbiórka przy dowódcy
        if ("marshal_mace".equals(itemId)) {
            // Reagujemy zarówno na PPM jak i LPM (LPM zawsze wysyła pakiet na serwer nawet przy kliknięciu w powietrze)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR
                    || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
                event.setCancelled(true);

                if (nation == null) {
                    player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby dowodzić wojskiem!");
                    return;
                }

                if (player.isSneaking()) {
                    militaryManager.orderRally(player, nation);
                    return;
                }

                org.bukkit.util.RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(
                        player.getEyeLocation(),
                        player.getEyeLocation().getDirection(),
                        65.0,
                        org.bukkit.FluidCollisionMode.NEVER,
                        true
                );

                Location targetLoc = null;
                if (rayTrace != null && rayTrace.getHitBlock() != null) {
                    targetLoc = rayTrace.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
                } else if (event.getClickedBlock() != null) {
                    targetLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
                } else {
                    Block tb = player.getTargetBlockExact(50);
                    if (tb != null) {
                        targetLoc = tb.getLocation().add(0.5, 1.0, 0.5);
                    }
                }

                if (targetLoc == null) {
                    player.sendMessage(ChatColor.YELLOW + "[BUŁAWA] Wyceluj w grunt/blok w zasięgu 65 kratek (lub Shift+LPM/PPM, aby wezwać wojsko do siebie)!");
                    return;
                }

                militaryManager.orderMarch(player, nation, targetLoc);
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!hand.hasItemMeta()) return;
        String itemId = items.getCustomItemId(hand);
        if ("marshal_mace".equals(itemId) && event.getRightClicked() instanceof LivingEntity target) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby wydawać rozkazy wojsku!");
                return;
            }
            militaryManager.orderAttackTarget(player, nation, target);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.hasItemMeta() && "marshal_mace".equals(items.getCustomItemId(hand))) {
                if (event.getEntity() instanceof LivingEntity target) {
                    Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
                    if (nation != null) {
                        militaryManager.orderAttackTarget(player, nation, target);
                    }
                }
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEntityCombust(org.bukkit.event.entity.EntityCombustEvent event) {
        if (event instanceof org.bukkit.event.entity.EntityCombustByBlockEvent || event instanceof org.bukkit.event.entity.EntityCombustByEntityEvent) {
            return;
        }
        // Blokada spalania od słońca (daylight burn)
        org.bukkit.entity.Entity entity = event.getEntity();
        if (entity.getPersistentDataContainer().has(militaryManager.KEY_SOLDIER_NATION, org.bukkit.persistence.PersistentDataType.STRING)
                || entity.getPersistentDataContainer().has(paoloManager.KEY_PAOLO_NATION, org.bukkit.persistence.PersistentDataType.STRING)
                || militaryManager.getUnit(entity.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
