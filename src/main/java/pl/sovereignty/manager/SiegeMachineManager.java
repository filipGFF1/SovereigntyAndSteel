package pl.sovereignty.manager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.SiegeMachine;
import pl.sovereignty.model.SiegeMachineType;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.*;

public class SiegeMachineManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final CustomItemRegistry items;
    private final Map<UUID, SiegeMachine> machinesById = new HashMap<>();
    private final Map<UUID, UUID> entityToMachineId = new HashMap<>();

    public SiegeMachineManager(Plugin plugin, NationManager nationManager, CustomItemRegistry items) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.items = items;
    }

    public SiegeMachine deployMachine(Player player, Nation nation, SiegeMachineType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        UUID id = UUID.randomUUID();
        SiegeMachine machine = new SiegeMachine(id, nation.getId(), type, loc);

        // Tworzenie bytu interakcji (Interaction entity)
        Interaction interaction = (Interaction) world.spawnEntity(loc, EntityType.INTERACTION);
        interaction.setInteractionWidth(1.5f);
        interaction.setInteractionHeight(1.5f);
        interaction.setResponsive(true);
        machine.setInteractionEntityUuid(interaction.getUniqueId());
        entityToMachineId.put(interaction.getUniqueId(), id);

        // Tworzenie bytu wyświetlania blokowego (BlockDisplay)
        BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(type.getIcon().createBlockData());
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f, 0.0f, -0.5f),
                new AxisAngle4f(),
                new Vector3f(1.0f, 1.0f, 1.0f),
                new AxisAngle4f()
        ));
        machine.setDisplayEntityUuid(display.getUniqueId());

        // Pasek życia / nazwa (TextDisplay)
        TextDisplay textDisplay = (TextDisplay) world.spawnEntity(loc.clone().add(0, 1.6, 0), EntityType.TEXT_DISPLAY);
        textDisplay.setText(ChatColor.translateAlternateColorCodes('&',
                "&6&l" + type.getDisplayName() + " &8[" + nation.getTag() + "]\n&c❤ " + (int) machine.getHealth() + "/" + (int) type.getMaxHealth()));
        textDisplay.setBillboard(Display.Billboard.CENTER);
        machine.setTextDisplayUuid(textDisplay.getUniqueId());

        machinesById.put(id, machine);

        world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.8f);
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
        player.sendMessage(ChatColor.GREEN + "Pomyślnie rozstawiono machinę: " + ChatColor.GOLD + type.getDisplayName() + "!");

        return machine;
    }

    public SiegeMachine getMachineByInteraction(UUID interactionUuid) {
        UUID machineId = entityToMachineId.get(interactionUuid);
        return machineId != null ? machinesById.get(machineId) : null;
    }

    public SiegeMachine getMachine(UUID id) {
        return machinesById.get(id);
    }

    public Collection<SiegeMachine> getAllMachines() {
        return machinesById.values();
    }

    public int getNationMachineCount(UUID nationId) {
        int count = 0;
        for (SiegeMachine m : machinesById.values()) {
            if (m.getNationId().equals(nationId)) count++;
        }
        return count;
    }

    /**
     * Uderzenie Kluczem Inżynierskim w celu zwinięcia machiny z powrotem do skrzyni.
     */
    public void hitWithWrench(Player player, SiegeMachine machine) {
        int hits = machine.incrementWrenchHits();
        Location loc = machine.getLocation();

        if (hits < 3) {
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);
            loc.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e[Klucz Inżynierski] Demontaż machiny &6" + machine.getType().getDisplayName()
                    + " &e[&f" + hits + "&e/3]..."));
        } else {
            // Zwinięcie machiny
            dismantleMachine(machine);

            ItemStack crate = switch (machine.getType()) {
                case BATTERING_RAM -> items.getCrateRam();
                case BALLISTA -> items.getCrateBallista();
                case MORTAR -> items.getCrateMortar();
            };

            loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);
            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.05);
            player.getInventory().addItem(crate);
            player.sendMessage(ChatColor.GREEN + "Machina została pomyślnie złożona do skrzyni montażowej!");
        }
    }

    public void dismantleMachine(SiegeMachine machine) {
        World world = machine.getLocation().getWorld();
        if (world != null) {
            if (machine.getInteractionEntityUuid() != null) {
                Entity e = world.getEntity(machine.getInteractionEntityUuid());
                if (e != null) e.remove();
                entityToMachineId.remove(machine.getInteractionEntityUuid());
            }
            if (machine.getDisplayEntityUuid() != null) {
                Entity e = world.getEntity(machine.getDisplayEntityUuid());
                if (e != null) e.remove();
            }
            if (machine.getTextDisplayUuid() != null) {
                Entity e = world.getEntity(machine.getTextDisplayUuid());
                if (e != null) e.remove();
            }
        }
        machinesById.remove(machine.getId());
    }

    /**
     * Obsługa strzału lub ataku machiny.
     */
    public void activateMachine(Player operator, SiegeMachine machine, WarDamageManager warDamage) {
        long now = System.currentTimeMillis();
        switch (machine.getType()) {
            case BATTERING_RAM -> {
                if (now - machine.getLastFiredTime() < 3000L) {
                    operator.sendMessage(ChatColor.RED + "Taran ładuje impet do kolejnego uderzenia!");
                    return;
                }
                machine.setLastFiredTime(now);
                ramGateBash(operator, machine, warDamage);
            }
            case BALLISTA -> {
                if (now - machine.getLastFiredTime() < 2500L) {
                    operator.sendMessage(ChatColor.RED + "Balista jest przeładowywana!");
                    return;
                }
                machine.setLastFiredTime(now);
                fireBallista(operator, machine);
            }
            case MORTAR -> {
                if (now - machine.getLastFiredTime() < 5000L) {
                    operator.sendMessage(ChatColor.RED + "Moździerz stygnie po ostatnim wystrzale!");
                    return;
                }
                machine.setLastFiredTime(now);
                fireMortar(operator, machine, warDamage);
            }
        }
    }

    private void ramGateBash(Player operator, SiegeMachine machine, WarDamageManager warDamage) {
        Location loc = machine.getLocation();
        Vector dir = operator.getLocation().getDirection().setY(0).normalize();
        Location front = loc.clone().add(dir.multiply(1.5));

        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.7f);
        loc.getWorld().spawnParticle(Particle.CRIT, front, 30, 0.4, 0.4, 0.4, 0.2);

        // Wyważanie wrót, drzwi, furt i palisad
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block b = front.clone().add(dx, dy, dz).getBlock();
                    Material mat = b.getType();
                    if (mat.name().contains("DOOR") || mat.name().contains("GATE") || mat.name().contains("TRAPDOOR") || mat.name().contains("FENCE")) {
                        warDamage.damageBlockInWar(b);
                    }
                }
            }
        }
        operator.sendMessage(ChatColor.GOLD + "[TARAN] Potężne uderzenie taranu w przeszkodę!");
    }

    private void fireBallista(Player operator, SiegeMachine machine) {
        Location loc = machine.getLocation().add(0, 1.2, 0);
        Vector dir = operator.getLocation().getDirection().normalize();

        Arrow arrow = loc.getWorld().spawnArrow(loc.add(dir.clone().multiply(0.5)), dir, 3.5f, 0.5f);
        arrow.setShooter(operator);
        arrow.setDamage(25.0);
        arrow.setPierceLevel(3); // Przebija do 3 wrogów w linii!
        arrow.setCritical(true);

        loc.getWorld().playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 1.5f, 0.8f);
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.2, 0.2, 0.2, 0);
        operator.sendMessage(ChatColor.GREEN + "[BALISTA] Wystrzelono bełt przebijający!");
    }

    private void fireMortar(Player operator, SiegeMachine machine, WarDamageManager warDamage) {
        Location loc = machine.getLocation().add(0, 1.5, 0);
        Vector dir = operator.getLocation().getDirection().normalize();
        // Wystrzał stromotorowy
        Vector velocity = dir.multiply(1.2).setY(1.1);

        Fireball bomb = (Fireball) loc.getWorld().spawnEntity(loc, EntityType.FIREBALL);
        bomb.setShooter(operator);
        bomb.setDirection(velocity);
        bomb.setIsIncendiary(false);
        bomb.setYield(3.5f); // Siła wybuchu

        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2);
        operator.sendMessage(ChatColor.GOLD + "[MOŹDZIERZ] Wystrzelono kulę artyleryjską w wysokiej paraboli!");
    }
}
