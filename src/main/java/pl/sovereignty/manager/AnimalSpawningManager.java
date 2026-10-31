package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import pl.sovereignty.SovereigntyPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * System odradzania dzikiej zwierzyny na wyspach i terytoriach państwowych.
 * Zapobiega wymieraniu zwierząt na serwerze i utrzymuje żywy ekosystem.
 */
public class AnimalSpawningManager {

    private final SovereigntyPlugin plugin;
    private final Random random = new Random();

    private static final List<EntityType> COMMON_ANIMALS = List.of(
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.PIG,
            EntityType.CHICKEN
    );

    private static final List<EntityType> RARE_ANIMALS = List.of(
            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.RABBIT,
            EntityType.LLAMA
    );

    public AnimalSpawningManager(SovereigntyPlugin plugin) {
        this.plugin = plugin;
    }

    public void runAnimalSpawningCycle() {
        if (!plugin.getConfig().getBoolean("wildlife.boost_animal_spawns", true)) {
            return;
        }

        int maxNearby = plugin.getConfig().getInt("wildlife.max_nearby_animals", 8);

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            if (world.getEnvironment() != World.Environment.NORMAL) continue;

            // Sprawdzamy liczbę zwierząt w promieniu 32 bloków od gracza
            Collection<Animals> nearbyAnimals = world.getNearbyEntitiesByType(Animals.class, player.getLocation(), 32.0);
            if (nearbyAnimals.size() >= maxNearby) {
                continue; // Wokół gracza jest już wystarczająca liczba zwierząt (np. zagroda)
            }

            // Losujemy pozycję w odległości 14 - 30 bloków od gracza
            int angle = random.nextInt(360);
            double dist = 14 + random.nextDouble() * 16;
            double rad = Math.toRadians(angle);
            int x = (int) (player.getLocation().getX() + Math.cos(rad) * dist);
            int z = (int) (player.getLocation().getZ() + Math.sin(rad) * dist);

            // Pobieramy najwyższy blok w tym punkcie
            int highestY = world.getHighestBlockYAt(x, z);
            Block groundBlock = world.getBlockAt(x, highestY - 1, z);
            Block airBlock = world.getBlockAt(x, highestY, z);
            Block airAbove = world.getBlockAt(x, highestY + 1, z);

            // Naturalne warunki do życia zwierzyny: trawa, wolna przestrzeń, światło dzienne
            if (groundBlock.getType() == Material.GRASS_BLOCK
                    && airBlock.getType() == Material.AIR
                    && airAbove.getType() == Material.AIR
                    && groundBlock.getLightFromSky() >= 9) {

                Location spawnLoc = new Location(world, x + 0.5, highestY, z + 0.5);

                // Losujemy typ zwierzęcia (90% na zwykłe krowy/owce/świnie/kury, 10% na konie/osły/lamy)
                EntityType chosenType = (random.nextInt(10) == 0)
                        ? RARE_ANIMALS.get(random.nextInt(RARE_ANIMALS.size()))
                        : COMMON_ANIMALS.get(random.nextInt(COMMON_ANIMALS.size()));

                // Spawnowanie małego stada (2-3 sztuki)
                int packSize = 2 + random.nextInt(2);
                for (int i = 0; i < packSize; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 3;
                    double offsetZ = (random.nextDouble() - 0.5) * 3;
                    Location animalLoc = spawnLoc.clone().add(offsetX, 0, offsetZ);
                    world.spawnEntity(animalLoc, chosenType);
                }
            }
        }
    }
}
