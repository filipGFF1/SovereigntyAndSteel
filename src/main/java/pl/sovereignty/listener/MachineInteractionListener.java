package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.manager.SiegeMachineManager;
import pl.sovereignty.manager.WarDamageManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.SiegeMachine;
import pl.sovereignty.util.CustomItemRegistry;

public class MachineInteractionListener implements Listener {
    private final SiegeMachineManager machineManager;
    private final NationManager nationManager;
    private final WarDamageManager warDamage;
    private final CustomItemRegistry items;

    public MachineInteractionListener(SiegeMachineManager machineManager, NationManager nationManager,
                                      WarDamageManager warDamage, CustomItemRegistry items) {
        this.machineManager = machineManager;
        this.nationManager = nationManager;
        this.warDamage = warDamage;
        this.items = items;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        SiegeMachine machine = machineManager.getMachineByInteraction(entity.getUniqueId());
        if (machine == null) return;

        event.setCancelled(true);
        ItemStack inHand = player.getInventory().getItemInMainHand();

        // Uderzenie kluczem inżynierskim
        if (inHand.hasItemMeta() && "engineering_wrench".equals(items.getCustomItemId(inHand))) {
            machineManager.hitWithWrench(player, machine);
            return;
        }

        // Aktywacja machiny (wystrzał / taran)
        machineManager.activateMachine(player, machine, warDamage);
    }

    @EventHandler
    public void onMachineDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        SiegeMachine machine = machineManager.getMachineByInteraction(entity.getUniqueId());
        if (machine == null) return;

        event.setCancelled(true);
        double damage = event.getFinalDamage();
        machine.setHealth(machine.getHealth() - damage);

        Location loc = machine.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.7f);
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);

        // Aktualizacja TextDisplay
        if (machine.getTextDisplayUuid() != null) {
            Entity textEnt = loc.getWorld().getEntity(machine.getTextDisplayUuid());
            if (textEnt instanceof TextDisplay td) {
                Nation nation = nationManager.getNation(machine.getNationId());
                String tag = nation != null ? nation.getTag() : "???";
                td.setText(ChatColor.translateAlternateColorCodes('&',
                        "&6&l" + machine.getType().getDisplayName() + " &8[" + tag + "]\n&c❤ "
                        + (int) machine.getHealth() + "/" + (int) machine.getType().getMaxHealth()));
            }
        }

        if (machine.getHealth() <= 0) {
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 1);
            machineManager.dismantleMachine(machine);
            if (event.getDamager() instanceof Player damager) {
                damager.sendMessage(ChatColor.RED + "Zniszczono machinę wroga: " + machine.getType().getDisplayName() + "!");
            }
        }
    }
}
