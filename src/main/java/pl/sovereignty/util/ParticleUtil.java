package pl.sovereignty.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ParticleUtil {

    public static void spawnDrumRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.DUST, new Location(world, x, center.getY() + 0.2, z), 1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(220, 40, 40), 1.5f));
            world.spawnParticle(Particle.CRIT, new Location(world, x, center.getY() + 0.3, z), 1, 0, 0, 0, 0.02);
        }
    }

    public static void spawnCommandBeam(Location from, Location to) {
        World world = from.getWorld();
        if (world == null || to.getWorld() == null || !world.equals(to.getWorld())) return;

        Vector dir = to.toVector().subtract(from.toVector());
        double distance = from.distance(to);
        int steps = (int) (distance * 2);
        if (steps <= 0) return;

        Vector stepVector = dir.clone().multiply(1.0 / steps);
        Location current = from.clone().add(0, 1.0, 0);

        for (int i = 0; i < steps; i++) {
            current.add(stepVector);
            world.spawnParticle(Particle.END_ROD, current, 1, 0, 0, 0, 0);
            if (i % 4 == 0) {
                world.spawnParticle(Particle.FLAME, current, 1, 0, 0, 0, 0.01);
            }
        }
    }

    public static void spawnLastStandZone(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;

        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, new Location(world, x, center.getY() + 0.1, z), 2, 0.1, 0.2, 0.1, 0.02);
            world.spawnParticle(Particle.DUST, new Location(world, x, center.getY() + 0.1, z), 1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.8f));
        }
    }

    public static void spawnRestorationEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.BLOCK, location.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3,
                org.bukkit.Material.STONE.createBlockData());
        world.spawnParticle(Particle.REVERSE_PORTAL, location.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05);
        world.playSound(location, Sound.BLOCK_STONE_PLACE, 0.8f, 1.2f);
    }
}
