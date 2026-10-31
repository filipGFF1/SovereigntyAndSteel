package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.PirateRaid;
import pl.sovereignty.model.StructureType;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.*;

public class PirateRaidManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final CustomItemRegistry items;
    private final Map<UUID, PirateRaid> activeRaids = new HashMap<>();
    private final Random random = new Random();

    public PirateRaidManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager, CustomItemRegistry items) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.items = items;
    }

    public PirateRaid triggerPirateRaid(Nation targetNation, Location shoreLocation) {
        return triggerPirateRaid(targetNation, shoreLocation, false);
    }

    public PirateRaid triggerPirateRaid(Nation targetNation, Location shoreLocation, boolean megaOp) {
        UUID raidId = UUID.randomUUID();
        Location beaconLoc = shoreLocation.clone().add(2, 0, 2);

        PirateRaid raid = new PirateRaid(raidId, targetNation.getId(), shoreLocation, beaconLoc, megaOp);
        activeRaids.put(raidId, raid);

        World world = shoreLocation.getWorld();
        if (world == null) return null;

        if (megaOp) {
            spawnMegaOpRaid(targetNation, shoreLocation, raid, world);
        } else {
            spawnStandardRaid(targetNation, shoreLocation, raid, world);
        }

        // Pokieruj piratów w stronę najbliższej odsłoniętej struktury produkcyjnej
        TerritoryStructure targetSector = findVulnerableSector(targetNation);
        if (targetSector != null) {
            for (UUID pUuid : raid.getPirateUuids()) {
                Entity e = Bukkit.getEntity(pUuid);
                if (e instanceof Mob mob) {
                    mob.getPathfinder().moveTo(targetSector.getLocation(), megaOp ? 1.25 : 1.15);
                }
            }
        }

        return raid;
    }

    private void spawnStandardRaid(Nation targetNation, Location shoreLocation, PirateRaid raid, World world) {
        // Spawnowanie Kapitana Piratów
        Drowned captain = (Drowned) world.spawnEntity(shoreLocation, EntityType.DROWNED);
        captain.setCustomName(ChatColor.translateAlternateColorCodes('&', "&4&lKapitan Piratów: Czarnobrody"));
        captain.setCustomNameVisible(true);
        captain.setRemoveWhenFarAway(false);
        if (captain.getAttribute(Attribute.MAX_HEALTH) != null) {
            captain.getAttribute(Attribute.MAX_HEALTH).setBaseValue(80.0);
        }
        captain.setHealth(80.0);

        EntityEquipment cEquip = captain.getEquipment();
        if (cEquip != null) {
            cEquip.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            cEquip.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            cEquip.setItemInMainHand(new ItemStack(Material.TRIDENT));
            cEquip.setHelmetDropChance(0.0f);
            cEquip.setChestplateDropChance(0.0f);
            cEquip.setLeggingsDropChance(0.0f);
            cEquip.setBootsDropChance(0.0f);
            cEquip.setItemInMainHandDropChance(0.0f);
            cEquip.setItemInOffHandDropChance(0.0f);
        }
        raid.getPirateUuids().add(captain.getUniqueId());

        // Spawnowanie 4 Łupieżców Pirackich
        for (int i = 0; i < 4; i++) {
            Location offsetLoc = shoreLocation.clone().add(Math.cos(i) * 3, 0, Math.sin(i) * 3);
            Drowned raider = (Drowned) world.spawnEntity(offsetLoc, EntityType.DROWNED);
            raider.setCustomName(ChatColor.translateAlternateColorCodes('&', "&cPiracki Łupieżca"));
            raider.setCustomNameVisible(true);
            raider.setRemoveWhenFarAway(false);

            EntityEquipment rEquip = raider.getEquipment();
            if (rEquip != null) {
                rEquip.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                rEquip.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                rEquip.setHelmetDropChance(0.0f);
                rEquip.setItemInMainHandDropChance(0.0f);
            }
            raid.getPirateUuids().add(raider.getUniqueId());
        }

        // Alert dla nacji
        broadcastToNation(targetNation, "\n&4&l[DESANT PIRATÓW Z OCEANU!]\n"
                + "&cEskadra szalup pirackich wylądowała na wybrzeżu Waszego państwa!\n"
                + "&ePiraci omijają bramy twierdzy i ruszają plądrować farmy, tartaki i kopalnie!\n"
                + "&6Wybór państwa: &fStawcie opór zbrojny lub złóżcie Haracz na plaży (&e16 Złota, 32 Chleba&f) komendą &e/panstwo haracz&f!\n");

        for (UUID member : targetNation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.5f, 0.7f);
        }
    }

    private void spawnMegaOpRaid(Nation targetNation, Location shoreLocation, PirateRaid raid, World world) {
        // 1. Spawnowanie Bossa: Admirał Karmazynowej Floty
        Drowned admiral = (Drowned) world.spawnEntity(shoreLocation, EntityType.DROWNED);
        admiral.setCustomName(ChatColor.translateAlternateColorCodes('&', "&4&l☠ ADMIRAŁ KARMAZYNOWEJ FLOTY: KRAKENLORD ☠"));
        admiral.setCustomNameVisible(true);
        admiral.setRemoveWhenFarAway(false);
        if (admiral.getAttribute(Attribute.MAX_HEALTH) != null) {
            admiral.getAttribute(Attribute.MAX_HEALTH).setBaseValue(350.0);
        }
        admiral.setHealth(350.0);
        if (admiral.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            admiral.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(12.0);
        }
        if (admiral.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            admiral.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        }

        // Boss efekty
        admiral.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 24000, 1, false, false));
        admiral.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 24000, 1, false, false));
        admiral.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 24000, 0, false, false));
        admiral.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 24000, 0, false, false));
        admiral.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 24000, 0, false, false));

        EntityEquipment aEquip = admiral.getEquipment();
        if (aEquip != null) {
            ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
            helmet.addEnchantment(Enchantment.UNBREAKING, 3);
            ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
            chest.addEnchantment(Enchantment.UNBREAKING, 3);
            ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
            legs.addEnchantment(Enchantment.UNBREAKING, 3);
            ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
            boots.addEnchantment(Enchantment.UNBREAKING, 3);

            ItemStack trident = new ItemStack(Material.TRIDENT);
            trident.addEnchantment(Enchantment.SHARPNESS, 5);
            trident.addEnchantment(Enchantment.UNBREAKING, 3);

            ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);

            aEquip.setHelmet(helmet);
            aEquip.setChestplate(chest);
            aEquip.setLeggings(legs);
            aEquip.setBoots(boots);
            aEquip.setItemInMainHand(trident);
            aEquip.setItemInOffHand(totem);

            aEquip.setHelmetDropChance(0.0f);
            aEquip.setChestplateDropChance(0.0f);
            aEquip.setLeggingsDropChance(0.0f);
            aEquip.setBootsDropChance(0.0f);
            aEquip.setItemInMainHandDropChance(0.0f);
            aEquip.setItemInOffHandDropChance(0.0f);
        }
        raid.getPirateUuids().add(admiral.getUniqueId());

        // 2. 4x Elitarni Korsarze Zguby
        for (int i = 0; i < 4; i++) {
            Location offsetLoc = shoreLocation.clone().add(Math.cos(i * 1.5) * 4, 0, Math.sin(i * 1.5) * 4);
            Drowned corsair = (Drowned) world.spawnEntity(offsetLoc, EntityType.DROWNED);
            corsair.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lElitarny Korsarz Zguby"));
            corsair.setCustomNameVisible(true);
            corsair.setRemoveWhenFarAway(false);
            if (corsair.getAttribute(Attribute.MAX_HEALTH) != null) {
                corsair.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100.0);
            }
            corsair.setHealth(100.0);
            corsair.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 24000, 0, false, false));
            corsair.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 24000, 0, false, false));

            EntityEquipment cEquip = corsair.getEquipment();
            if (cEquip != null) {
                cEquip.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                cEquip.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                cEquip.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                cEquip.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                cEquip.setItemInMainHand(new ItemStack(Material.DIAMOND_AXE));
                cEquip.setItemInOffHand(new ItemStack(Material.SHIELD));

                cEquip.setHelmetDropChance(0.0f);
                cEquip.setChestplateDropChance(0.0f);
                cEquip.setLeggingsDropChance(0.0f);
                cEquip.setBootsDropChance(0.0f);
                cEquip.setItemInMainHandDropChance(0.0f);
                cEquip.setItemInOffHandDropChance(0.0f);
            }
            raid.getPirateUuids().add(corsair.getUniqueId());
        }

        // 3. 3x Piraccy Kanonierzy Głębina
        for (int i = 0; i < 3; i++) {
            Location offsetLoc = shoreLocation.clone().add(Math.sin(i * 2.0) * 5, 0, Math.cos(i * 2.0) * 5);
            Drowned gunner = (Drowned) world.spawnEntity(offsetLoc, EntityType.DROWNED);
            gunner.setCustomName(ChatColor.translateAlternateColorCodes('&', "&6&lPiracki Kanonier Głębina"));
            gunner.setCustomNameVisible(true);
            gunner.setRemoveWhenFarAway(false);
            if (gunner.getAttribute(Attribute.MAX_HEALTH) != null) {
                gunner.getAttribute(Attribute.MAX_HEALTH).setBaseValue(60.0);
            }
            gunner.setHealth(60.0);
            gunner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 24000, 0, false, false));

            EntityEquipment gEquip = gunner.getEquipment();
            if (gEquip != null) {
                gEquip.setHelmet(new ItemStack(Material.IRON_HELMET));
                gEquip.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                gEquip.setItemInMainHand(new ItemStack(Material.TRIDENT));

                gEquip.setHelmetDropChance(0.0f);
                gEquip.setChestplateDropChance(0.0f);
                gEquip.setItemInMainHandDropChance(0.0f);
            }
            raid.getPirateUuids().add(gunner.getUniqueId());
        }

        // 4. 2x Morskie Behemoty
        for (int i = 0; i < 2; i++) {
            Location offsetLoc = shoreLocation.clone().add((i == 0 ? 3 : -3), 0, (i == 0 ? -3 : 3));
            Drowned behemoth = (Drowned) world.spawnEntity(offsetLoc, EntityType.DROWNED);
            behemoth.setCustomName(ChatColor.translateAlternateColorCodes('&', "&4&lMorski Behemot"));
            behemoth.setCustomNameVisible(true);
            behemoth.setRemoveWhenFarAway(false);
            if (behemoth.getAttribute(Attribute.MAX_HEALTH) != null) {
                behemoth.getAttribute(Attribute.MAX_HEALTH).setBaseValue(120.0);
            }
            behemoth.setHealth(120.0);
            if (behemoth.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
                behemoth.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.9);
            }

            EntityEquipment bEquip = behemoth.getEquipment();
            if (bEquip != null) {
                bEquip.setHelmet(new ItemStack(Material.IRON_HELMET));
                bEquip.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                bEquip.setItemInMainHand(new ItemStack(Material.IRON_AXE));

                bEquip.setHelmetDropChance(0.0f);
                bEquip.setChestplateDropChance(0.0f);
                bEquip.setItemInMainHandDropChance(0.0f);
            }
            raid.getPirateUuids().add(behemoth.getUniqueId());
        }

        // Globalny alert na cały serwer
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4&l☠☠☠ [LEGENDARNY NAJAZD: KARMAZYNOWA FLOTA - MEGA OP RAID!] ☠☠☠\n"
                + "&cZ otchłani oceanu wyłoniła się flotylla Admirała Krakenlorda!\n"
                + "&cOkręty uderzyły w wybrzeże państwa &f" + targetNation.getName() + " &8[" + targetNation.getTag() + "]&c!\n"
                + "&eTo najpotężniejszy najazd w historii wyspy! Przetrwają tylko zjednoczeni wojownicy!\n"
                + "&4&l☠☠☠==================================================☠☠☠\n"));

        broadcastToNation(targetNation, "\n&4&l[ALARM OBRONY NARODOWEJ! MEGA OP RAID!]\n"
                + "&cAdmirał żąda kapitulacji państwa! Przygotujcie broń lub złóżcie potężny Haracz na plaży:\n"
                + "&6Wymóg Haraczu: &e64 Sztabki Złota, 16 Diamentów i 64 Bochenki Chleba &8(&e/panstwo haracz&8)!\n");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.6f);
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 2.0f, 0.6f);
        }
    }

    private TerritoryStructure findVulnerableSector(Nation nation) {
        for (TerritoryStructure s : territoryManager.getNationStructures(nation.getId())) {
            if (s.getType() == StructureType.STATE_FARM || s.getType() == StructureType.SAWMILL_TOTEM || s.getType() == StructureType.MINING_SHAFT) {
                return s;
            }
        }
        return null;
    }

    /**
     * Wpłata Haraczu przez gracza nacji
     */
    public boolean payTribute(Player player, PirateRaid raid) {
        Nation nation = nationManager.getNation(raid.getTargetNationId());
        if (nation == null) return false;

        if (raid.isMegaOp()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), 64)
                    || !player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), 16)
                    || !player.getInventory().containsAtLeast(new ItemStack(Material.BREAD), 64)) {
                player.sendMessage(ChatColor.RED + "Haracz za odparcie Floty Admirała (Mega OP Raid) wymaga:\n"
                        + "64 Sztabki Złota, 16 Diamentów i 64 Bochenków Chleba!");
                return false;
            }

            player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, 64));
            player.getInventory().removeItem(new ItemStack(Material.DIAMOND, 16));
            player.getInventory().removeItem(new ItemStack(Material.BREAD, 64));

            raid.setTributePaid(true);
            raid.setCompleted(true);

            nation.incrementTributesPaid();
            nation.setGdp(Math.max(0, nation.getGdp() - 400.0));
            broadcastToNation(nation, "&e[HARACZ OPŁACONY] Admirał Karmazynowej Floty przyjął okup w złocie i diamentach. Flotylla odpływa, lecz państwo straciło 400 pkt PKB!");
        } else {
            if (!player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), 16)
                    || !player.getInventory().containsAtLeast(new ItemStack(Material.BREAD), 32)) {
                player.sendMessage(ChatColor.RED + "Haracz wymaga: 16 Sztabek Złota i 32 Bochenków Chleba!");
                return false;
            }

            player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, 16));
            player.getInventory().removeItem(new ItemStack(Material.BREAD, 32));

            raid.setTributePaid(true);
            raid.setCompleted(true);

            nation.incrementTributesPaid();
            nation.setGdp(Math.max(0, nation.getGdp() - 150.0)); // Utrata PKB i prestiżu
            broadcastToNation(nation, "&e[HARACZ OPŁACONY] Piraci zebrali złoto i żywność z plaży. Desant odpływa, lecz naród utracił prestiż gospodarczy (-150 PKB)!");
        }

        // Usunięcie piratów (odpływają w morze)
        for (UUID pUuid : raid.getPirateUuids()) {
            Entity e = Bukkit.getEntity(pUuid);
            if (e != null) {
                e.getWorld().spawnParticle(Particle.SPLASH, e.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                e.remove();
            }
        }

        activeRaids.remove(raid.getRaidId());
        return true;
    }

    /**
     * Zwycięstwo zbrojne nad piratami
     */
    public void checkRaidVictory(PirateRaid raid) {
        boolean allDefeated = true;
        for (UUID pUuid : raid.getPirateUuids()) {
            Entity e = Bukkit.getEntity(pUuid);
            if (e != null && !e.isDead() && e.isValid()) {
                allDefeated = false;
                break;
            }
        }

        if (allDefeated) {
            raid.setCompleted(true);
            activeRaids.remove(raid.getRaidId());

            Nation nation = nationManager.getNation(raid.getTargetNationId());
            Location loc = raid.getLandingLocation();

            if (raid.isMegaOp()) {
                if (nation != null) {
                    nation.incrementPiratesDefeated();
                    nation.addMilitaryScore(150);
                    nation.setGdp(nation.getGdp() + 1000.0);

                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "\n&6&l☠☠☠ [LEGENDARNE ZWYCIĘSTWO: ROZBICIE KARMAZYNOWEJ FLOTY!] ☠☠☠\n"
                            + "&ePaństwo &f" + nation.getName() + " &8[" + nation.getTag() + "] &ezgładziło Admirała Krakenlorda i odparło &4&lMEGA OP RAID&e!\n"
                            + "&aMieszkańcy zdobywają chwałę na całej wyspie, 3 Skrzynie Zagranicznego Łupu, +150 Punktów Wojska i +1000 PKB!\n"
                            + "&6&l☠☠☠==================================================☠☠☠\n"));
                }

                if (loc != null && loc.getWorld() != null) {
                    loc.getWorld().dropItemNaturally(loc, items.getForeignLootChest());
                    loc.getWorld().dropItemNaturally(loc, items.getForeignLootChest());
                    loc.getWorld().dropItemNaturally(loc, items.getForeignLootChest());
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 16));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, 32));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, 1));
                    loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.2f, 0.8f);
                }
            } else {
                if (nation != null) {
                    nation.incrementPiratesDefeated();
                    nation.addMilitaryScore(25);
                    nation.setGdp(nation.getGdp() + 200.0);

                    broadcastToNation(nation, "\n&6&l[ZWYCIĘSTWO NAD PIRATAMI!]\n"
                            + "&aDesant morski został doszczętnie rozbity przez obrońców wyspy!\n"
                            + "&eZdobyto Skrzynię Zagranicznego Łupu i podbito pozycję w rankingu Pogromców Piratów (+25 Wojska, +200 PKB)!\n");
                }

                if (loc != null && loc.getWorld() != null) {
                    loc.getWorld().dropItemNaturally(loc, items.getForeignLootChest());
                    loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
                }
            }
        }
    }

    /**
     * Odnajduje naturalne miejsce lądowania rajdu u wybrzeży państwa
     */
    public Location findRaidLandingLocation(Nation nation) {
        TerritoryStructure bastion = territoryManager.getNationBastion(nation.getId());
        Location centerLoc = null;
        if (bastion != null) {
            centerLoc = bastion.getLocation();
        }

        if (centerLoc == null) {
            for (UUID mUuid : nation.getMembers().keySet()) {
                Player p = Bukkit.getPlayer(mUuid);
                if (p != null && p.isOnline()) {
                    centerLoc = p.getLocation();
                    break;
                }
            }
        }

        if (centerLoc == null && !nation.getClaimedChunks().isEmpty()) {
            NationClaimChunk chunk = nation.getClaimedChunks().iterator().next();
            World w = Bukkit.getWorld(chunk.getWorld());
            if (w != null) {
                int cx = (chunk.getX() << 4) + 8;
                int cz = (chunk.getZ() << 4) + 8;
                centerLoc = new Location(w, cx, w.getHighestBlockYAt(cx, cz), cz);
            }
        }

        if (centerLoc == null || centerLoc.getWorld() == null) return null;

        World world = centerLoc.getWorld();
        Location shoreLoc = null;

        // Szukamy wody/plaży w promieniu 25-65 kratek od centrum pod gołym niebem
        for (int r = 25; r <= 65; r += 6) {
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 8) {
                int x = centerLoc.getBlockX() + (int) (Math.cos(angle) * r);
                int z = centerLoc.getBlockZ() + (int) (Math.sin(angle) * r);
                
                // Wymóg wolnego bloku nieba - getHighestBlockAt / getHighestBlockYAt
                Block highest = world.getHighestBlockAt(x, z);
                int y = highest.getY();
                
                // Ochrona spawnu: odrzucamy jaskinie i zadaszone farmy mobów (wymóg światła słonecznego >= 10)
                if (highest.getLightFromSky() < 10) {
                    continue;
                }

                Block block = world.getBlockAt(x, y, z);
                Block below = world.getBlockAt(x, Math.max(1, y - 1), z);
                Block above1 = world.getBlockAt(x, y + 1, z);
                Block above2 = world.getBlockAt(x, y + 2, z);

                // Sprawdź czy nad miejscem lądowania jest wolna przestrzeń
                if (!above1.isEmpty() && above1.getType() != Material.WATER) continue;
                if (!above2.isEmpty() && above2.getType() != Material.WATER) continue;

                if (block.getType() == Material.WATER || below.getType() == Material.WATER || below.getType() == Material.SAND) {
                    shoreLoc = new Location(world, x + 0.5, Math.max(y, below.getY() + 1), z + 0.5);
                    break;
                }
            }
            if (shoreLoc != null) break;
        }

        if (shoreLoc == null) {
            int x = centerLoc.getBlockX() + 35;
            int z = centerLoc.getBlockZ() + 35;
            Block highest = world.getHighestBlockAt(x, z);
            shoreLoc = new Location(world, x + 0.5, highest.getY() + 1, z + 0.5);
        }

        return shoreLoc;
    }

    public boolean hasActiveRaid(UUID nationId) {
        for (PirateRaid r : activeRaids.values()) {
            if (r.getTargetNationId().equals(nationId) && !r.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cykliczne sprawdzanie zagrożenia rajdem piratów:
     * - Jeśli państwo ma 10+ chunków: szansa na rajd rośnie o 10% co 10 minut.
     * - Jeśli państwo ma 20+ chunków: szansa na rajd rośnie o 10% co 5 minut.
     * - 2% szansy na Mega OP Trudny Raid.
     */
    public void tickDynamicRaidThreat() {
        long now = System.currentTimeMillis();

        // 1. Sprawdzanie i czyszczenie przedawnionych/martwych rajdów
        List<UUID> toClean = new ArrayList<>();
        for (PirateRaid r : activeRaids.values()) {
            if (r.isCompleted()) {
                toClean.add(r.getRaidId());
                continue;
            }
            boolean anyAlive = false;
            for (UUID pUuid : r.getPirateUuids()) {
                Entity e = Bukkit.getEntity(pUuid);
                if (e != null && !e.isDead() && e.isValid()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                checkRaidVictory(r);
                toClean.add(r.getRaidId());
            } else if (now - r.getStartTime() > 20 * 60 * 1000L) {
                // Po 20 minutach rajd się przedawnia (piraci odpływają)
                r.setCompleted(true);
                toClean.add(r.getRaidId());
            }
        }
        for (UUID id : toClean) {
            activeRaids.remove(id);
        }

        // 2. Dynamiczne naliczanie szansy na raid dla państw
        for (Nation nation : nationManager.getAllNations()) {
            int chunkCount = nation.getClaimedChunks().size();

            // Warunek: państwo musi mieć co najmniej 10 chunków
            if (chunkCount < 10) {
                if (nation.getRaidThreatChance() > 0) {
                    nation.setRaidThreatChance(0.0);
                }
                nation.setLastRaidThreatIncrementTime(0L);
                continue;
            }

            // Nie wywołuj kolejnego rajdu, gdy poprzedni jeszcze trwa
            if (hasActiveRaid(nation.getId())) {
                continue;
            }

            // Weryfikacja obecności przynajmniej jednego gracza państwa online
            boolean hasOnlineMember = false;
            for (UUID mUuid : nation.getMembers().keySet()) {
                Player p = Bukkit.getPlayer(mUuid);
                if (p != null && p.isOnline()) {
                    hasOnlineMember = true;
                    break;
                }
            }
            if (!hasOnlineMember) {
                continue;
            }

            // Określenie interwału: dla 20+ chunków co 5 minut (300 000 ms), dla 10-19 chunków co 10 minut (600 000 ms)
            long intervalMs = (chunkCount >= 20) ? (5L * 60L * 1000L) : (10L * 60L * 1000L);

            if (nation.getLastRaidThreatIncrementTime() <= 0L) {
                nation.setLastRaidThreatIncrementTime(now);
                continue;
            }

            if (now - nation.getLastRaidThreatIncrementTime() >= intervalMs) {
                nation.setLastRaidThreatIncrementTime(now);

                // Szansa na raid powiększa się o 10%
                double currentChance = Math.min(100.0, nation.getRaidThreatChance() + 10.0);
                nation.setRaidThreatChance(currentChance);

                // Losowanie czy raid się rozpoczyna
                double roll = random.nextDouble() * 100.0;
                if (roll < currentChance) {
                    // RAID ROZPOCZĘTY! Reset szansy
                    nation.setRaidThreatChance(0.0);

                    // 2% szansy na mega op trudny raid
                    boolean isMegaOp = (random.nextDouble() < 0.02);

                    Location landingLoc = findRaidLandingLocation(nation);
                    if (landingLoc != null) {
                        triggerPirateRaid(nation, landingLoc, isMegaOp);
                    }
                } else {
                    // Szansa wzrosła, powiadom graczy państwa o rosnącym zagrożeniu
                    broadcastToNation(nation, "&8[&6WYWIAD MORSKI&8] &eWzrosła aktywność pirackich statków u wybrzeży! Aktualna szansa na najazd: &c"
                            + (int) currentChance + "% &8(kolejne sprawdzenie za " + (chunkCount >= 20 ? "5" : "10") + " min).");
                }
            }
        }
    }

    public PirateRaid getRaidByPirate(UUID entityUuid) {
        for (PirateRaid r : activeRaids.values()) {
            if (r.getPirateUuids().contains(entityUuid)) return r;
        }
        return null;
    }

    public Collection<PirateRaid> getActiveRaids() {
        return activeRaids.values();
    }

    private void broadcastToNation(Nation nation, String msg) {
        for (UUID member : nation.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
    }
}
