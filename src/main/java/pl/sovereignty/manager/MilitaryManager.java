package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.sovereignty.model.*;
import pl.sovereignty.util.ParticleUtil;

import java.util.*;

public class MilitaryManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final Map<UUID, MilitaryUnit> unitsByEntityUuid = new HashMap<>();
    private final Map<UUID, List<MilitaryUnit>> nationUnits = new HashMap<>();

    public final NamespacedKey KEY_SOLDIER_NATION;
    public final NamespacedKey KEY_SOLDIER_TYPE;

    public MilitaryManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.KEY_SOLDIER_NATION = new NamespacedKey(plugin, "soldier_nation");
        this.KEY_SOLDIER_TYPE = new NamespacedKey(plugin, "soldier_type");
    }

    public int getMaxArmyLimit(UUID nationId) {
        int limit = 0;
        for (TerritoryStructure s : territoryManager.getNationStructures(nationId)) {
            if (s.getType() == StructureType.BARRACKS) {
                limit += 6; // Każdy barak zwiększa limit o 6 NPC
            }
        }
        return limit;
    }

    public void cleanDeadUnits(UUID nationId) {
        List<MilitaryUnit> list = nationUnits.get(nationId);
        if (list == null) return;
        Iterator<MilitaryUnit> it = list.iterator();
        while (it.hasNext()) {
            MilitaryUnit u = it.next();
            Entity e = Bukkit.getEntity(u.getEntityUuid());
            if (e == null || e.isDead() || !e.isValid()) {
                it.remove();
                unitsByEntityUuid.remove(u.getEntityUuid());
            }
        }
    }

    public int getCurrentArmyCount(UUID nationId) {
        cleanDeadUnits(nationId);
        List<MilitaryUnit> list = nationUnits.get(nationId);
        return list != null ? list.size() : 0;
    }

    private void addUnitToNation(UUID nationId, MilitaryUnit unit) {
        unitsByEntityUuid.put(unit.getEntityUuid(), unit);
        List<MilitaryUnit> list = nationUnits.computeIfAbsent(nationId, k -> new ArrayList<>());
        if (list.stream().noneMatch(u -> u.getEntityUuid().equals(unit.getEntityUuid()))) {
            list.add(unit);
        }
    }

    public MilitaryUnit recruitUnit(Player recruiter, Nation nation, MilitaryUnitType type, Location spawnLoc) {
        if (nation.isStarving()) {
            recruiter.sendMessage(ChatColor.RED + "[GŁÓD] Brak żywności w państwie! Rekrutacja wstrzymana!");
            return null;
        }

        int maxLimit = getMaxArmyLimit(nation.getId());
        int current = getCurrentArmyCount(nation.getId());
        if (current >= maxLimit) {
            recruiter.sendMessage(ChatColor.RED + "Osiągnięto limit armii (" + current + "/" + maxLimit + ")! Zbuduj więcej Baraków.");
            return null;
        }

        double inflation = nation.getInflationFactor();
        int baseFoodCost = switch (type) {
            case SHIELDBEARER -> 5;
            case CROSSBOWMAN -> 6;
            case SAPPER -> 4;
        };
        int foodCost = (int) Math.ceil(baseFoodCost * inflation);

        if (nation.getFoodStockpile() < foodCost) {
            recruiter.sendMessage(ChatColor.RED + "Państwo potrzebuje " + foodCost + " chleba/żywności na wyżywienie rekruta (Wpływ inflacji: " + (int)(inflation*100) + "%)!");
            return null;
        }

        nation.addFood(-foodCost);

        // Tworzenie bytu NPC wojskowego
        World world = spawnLoc.getWorld();
        Skeleton soldier = (Skeleton) world.spawnEntity(spawnLoc, EntityType.SKELETON);
        soldier.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&c[" + nation.getTag() + "] &f" + type.getDisplayName()));
        soldier.setCustomNameVisible(true);
        soldier.setRemoveWhenFarAway(false);
        soldier.setTarget(null);

        // Usunięcie celów agresji (NearestAttackableTarget itp.), aby żołnierz nigdy samoczynnie nie atakował graczy
        try {
            Bukkit.getMobGoals().removeAllGoals(soldier, GoalType.TARGET);
        } catch (Throwable ignored) {}

        soldier.getPersistentDataContainer().set(KEY_SOLDIER_NATION, PersistentDataType.STRING, nation.getId().toString());
        soldier.getPersistentDataContainer().set(KEY_SOLDIER_TYPE, PersistentDataType.STRING, type.name());

        equipSoldier(soldier, type, nation);

        MilitaryUnit unit = new MilitaryUnit(soldier.getUniqueId(), nation.getId(), type);
        addUnitToNation(nation.getId(), unit);
        nation.addMilitaryScore(5);

        world.playSound(spawnLoc, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
        world.spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
        recruiter.sendMessage(ChatColor.GREEN + "Zrekrutowano jednostkę: " + ChatColor.GOLD + type.getDisplayName() + ChatColor.GREEN + "!");

        return unit;
    }

    public void discoverNationSoldiers(UUID nationId) {
        String nIdStr = nationId.toString();
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.isDead()) continue;
                String tag = living.getPersistentDataContainer().get(KEY_SOLDIER_NATION, PersistentDataType.STRING);
                if (nIdStr.equals(tag)) {
                    getUnit(living.getUniqueId());
                }
            }
        }
    }

    public List<MilitaryUnit> getNationUnits(UUID nationId) {
        cleanDeadUnits(nationId);
        List<MilitaryUnit> list = nationUnits.get(nationId);
        if (list == null || list.isEmpty()) {
            discoverNationSoldiers(nationId);
            cleanDeadUnits(nationId);
            list = nationUnits.get(nationId);
        }
        return list != null ? list : Collections.emptyList();
    }

    public MilitaryUnit getUnit(UUID entityUuid) {
        MilitaryUnit unit = unitsByEntityUuid.get(entityUuid);
        if (unit != null) return unit;

        // Próba odzyskania jednostki po restarcie z PersistentDataContainer lub CustomName
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof LivingEntity living) {
            String nationIdStr = living.getPersistentDataContainer().get(KEY_SOLDIER_NATION, PersistentDataType.STRING);
            if (nationIdStr != null) {
                try {
                    UUID nationId = UUID.fromString(nationIdStr);
                    String typeStr = living.getPersistentDataContainer().get(KEY_SOLDIER_TYPE, PersistentDataType.STRING);
                    MilitaryUnitType type = typeStr != null ? MilitaryUnitType.valueOf(typeStr) : MilitaryUnitType.SHIELDBEARER;
                    unit = new MilitaryUnit(entityUuid, nationId, type);
                    addUnitToNation(nationId, unit);
                    return unit;
                } catch (Exception ignored) {}
            }

            // Rezerwowe sprawdzenie po CustomName dla jednostek zespawnowanych przed tą aktualizacją
            String customName = living.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                for (Nation n : nationManager.getAllNations()) {
                    if (customName.contains("[" + n.getTag() + "]")) {
                        MilitaryUnitType type = MilitaryUnitType.SHIELDBEARER;
                        for (MilitaryUnitType t : MilitaryUnitType.values()) {
                            if (customName.contains(t.getDisplayName())) {
                                type = t;
                                break;
                            }
                        }
                        unit = new MilitaryUnit(entityUuid, n.getId(), type);
                        addUnitToNation(n.getId(), unit);
                        living.getPersistentDataContainer().set(KEY_SOLDIER_NATION, PersistentDataType.STRING, n.getId().toString());
                        living.getPersistentDataContainer().set(KEY_SOLDIER_TYPE, PersistentDataType.STRING, type.name());
                        if (living instanceof Mob mob) {
                            mob.setTarget(null);
                            try {
                                Bukkit.getMobGoals().removeAllGoals(mob, GoalType.TARGET);
                            } catch (Throwable ignored) {}
                        }
                        return unit;
                    }
                }
            }
        }
        return null;
    }

    public void removeUnit(UUID entityUuid) {
        unitsByEntityUuid.remove(entityUuid);
        for (List<MilitaryUnit> list : nationUnits.values()) {
            list.removeIf(u -> u.getEntityUuid().equals(entityUuid));
        }
    }

    /**
     * Kryzys wojskowy w Dniu 2 zadłużenia państwa: dezercja części wojska i 50% osłabienie.
     */
    public void applyDebtMilitaryCrisis(UUID nationId) {
        List<MilitaryUnit> units = new ArrayList<>(getNationUnits(nationId));
        if (units.isEmpty()) return;

        // Dezercja jednej jednostki
        MilitaryUnit deserted = units.remove(0);
        Entity dEnt = Bukkit.getEntity(deserted.getEntityUuid());
        if (dEnt != null) {
            dEnt.getWorld().spawnParticle(Particle.SMOKE, dEnt.getLocation().add(0, 1, 0), 15, 0.2, 0.5, 0.2, 0.05);
            dEnt.remove();
        }
        removeUnit(deserted.getEntityUuid());

        // Pozostałe jednostki otrzymują 50% redukcji zdrowia i osłabienie
        for (MilitaryUnit u : units) {
            Entity ent = Bukkit.getEntity(u.getEntityUuid());
            if (ent instanceof LivingEntity living && !living.isDead()) {
                living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 24000, 1));
                living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 24000, 0));
                double currentHealth = living.getHealth();
                living.setHealth(Math.max(1.0, currentHealth * 0.5));
            }
        }
    }

    /**
     * Róg Bojowy: Zmiana szyku armii (Mur Tarcz, Szarża, Odwrót)
     */
    public void cycleFormation(Player player, Nation nation) {
        List<MilitaryUnit> units = nationUnits.get(nation.getId());
        if (units == null || units.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Twoje państwo nie posiada obecnie żadnych aktywnych jednostek wojskowych.");
            return;
        }

        MilitaryFormation nextFormation = units.get(0).getFormation().next();
        for (MilitaryUnit unit : units) {
            unit.setFormation(nextFormation);
            Entity entity = Bukkit.getEntity(unit.getEntityUuid());
            if (entity instanceof LivingEntity living) {
                applyFormationEffects(living, nextFormation);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.5f, 1.0f);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c[RÓG BOJOWY] &6Zmieniono formację wojsk na: &e" + nextFormation.getDisplayName() + "&6!"));
        player.sendMessage(ChatColor.GRAY + nextFormation.getDescription());
    }

    private void applyFormationEffects(LivingEntity entity, MilitaryFormation formation) {
        entity.removePotionEffect(PotionEffectType.SPEED);
        entity.removePotionEffect(PotionEffectType.RESISTANCE);
        entity.removePotionEffect(PotionEffectType.SLOWNESS);

        switch (formation) {
            case SHIELD_WALL -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 1));
            }
            case CHARGE -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1)); // +20% prędkości
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 0));
            }
            case RETREAT -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2));
            }
        }
    }

    /**
     * Buława Marszałkowska: Rozkaz marszu na pozycję
     */
    public void orderMarch(Player commander, Nation nation, Location targetLoc) {
        List<MilitaryUnit> units = nationUnits.get(nation.getId());
        if (units == null || units.isEmpty()) {
            discoverNationSoldiers(nation.getId());
            units = nationUnits.get(nation.getId());
        }
        if (units == null || units.isEmpty()) {
            commander.sendMessage(ChatColor.YELLOW + "Twoje państwo nie posiada obecnie żadnych aktywnych jednostek wojskowych.");
            commander.sendMessage(ChatColor.GRAY + "Wskazówka: Zrekrutuj żołnierzy w strukturze Baraki.");
            return;
        }

        ParticleUtil.spawnCommandBeam(commander.getLocation(), targetLoc);
        targetLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 25, 0.4, 0.8, 0.4, 0.1);
        targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc, 15, 0.3, 0.5, 0.3, 0.05);
        commander.getWorld().playSound(commander.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.2f, 1.4f);

        int count = 0;
        for (MilitaryUnit unit : units) {
            if (unit.isPanicking()) continue;
            unit.setCommandTargetLocation(targetLoc);
            unit.setCommandTargetEntity(null);

            Entity entity = Bukkit.getEntity(unit.getEntityUuid());
            if (entity instanceof Mob mob && mob.isValid()) {
                boostFollowRange(mob);
                mob.setTarget(null);
                mob.getPathfinder().moveTo(targetLoc, 1.35);
                count++;
            }
        }

        commander.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6[BUŁAWA MARSZAŁKOWSKA] &eRozkaz marszu na pozycję (" + targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ() + ") wydany do &f" + count + " &ejednostek!"));
    }

    /**
     * Buława Marszałkowska: Natychmiastowa zbiórka wojsk przy dowódcy
     */
    public void orderRally(Player commander, Nation nation) {
        orderMarch(commander, nation, commander.getLocation());
        commander.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e[BUŁAWA MARSZAŁKOWSKA] &6Wezwano wszystkie oddziały na natychmiastową zbiórkę przy Tobie!"));
    }

    /**
     * Buława Marszałkowska: Rozkaz likwidacji celu
     */
    public void orderAttackTarget(Player commander, Nation nation, LivingEntity target) {
        List<MilitaryUnit> units = nationUnits.get(nation.getId());
        if (units == null || units.isEmpty()) {
            discoverNationSoldiers(nation.getId());
            units = nationUnits.get(nation.getId());
        }
        if (units == null || units.isEmpty()) {
            commander.sendMessage(ChatColor.YELLOW + "Brak oddziałów do ataku.");
            return;
        }

        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.15);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
        commander.getWorld().playSound(commander.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.7f);

        int count = 0;
        for (MilitaryUnit unit : units) {
            if (unit.isPanicking()) continue;
            unit.setCommandTargetEntity(target.getUniqueId());
            unit.setCommandTargetLocation(null);

            Entity entity = Bukkit.getEntity(unit.getEntityUuid());
            if (entity instanceof Mob mob && mob.isValid()) {
                boostFollowRange(mob);
                mob.setTarget(target);
                mob.getPathfinder().moveTo(target.getLocation(), 1.4);
                count++;
            }
        }

        commander.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&4[BUŁAWA MARSZAŁKOWSKA] &cRozkaz skupienia ognia na celu: &f" + target.getName() + " &c(&f" + count + " &cjednostek)!"));
    }

    private void boostFollowRange(Mob mob) {
        try {
            org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute.valueOf("FOLLOW_RANGE");
            if (mob.getAttribute(attr) != null) mob.getAttribute(attr).setBaseValue(128.0);
        } catch (Throwable t) {
            try {
                org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute.valueOf("GENERIC_FOLLOW_RANGE");
                if (mob.getAttribute(attr) != null) mob.getAttribute(attr).setBaseValue(128.0);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Panika armii po śmierci Generała Paolo Or'e
     */
    public void triggerArmyPanic(UUID nationId) {
        List<MilitaryUnit> units = nationUnits.get(nationId);
        if (units == null) return;

        for (MilitaryUnit unit : units) {
            unit.setPanicDurationMs(60_000L); // 60 sekund paniki
            Entity entity = Bukkit.getEntity(unit.getEntityUuid());
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2));
            }
        }
    }

    public static ItemStack makeUnbreakable(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void equipSoldier(LivingEntity soldier, MilitaryUnitType type, Nation nation) {
        EntityEquipment equip = soldier.getEquipment();
        if (equip == null) return;

        equip.clear();
        equip.setHelmetDropChance(0.0f);
        equip.setChestplateDropChance(0.0f);
        equip.setLeggingsDropChance(0.0f);
        equip.setBootsDropChance(0.0f);
        equip.setItemInMainHandDropChance(0.0f);
        equip.setItemInOffHandDropChance(0.0f);

        String tag = nation != null ? "[" + nation.getTag() + "] " : "";

        switch (type) {
            case SHIELDBEARER -> {
                equip.setHelmet(makeUnbreakable(Material.IRON_HELMET, "&fHełm Tarczownika " + tag));
                equip.setChestplate(makeUnbreakable(Material.IRON_CHESTPLATE, "&fZbroja Tarczownika " + tag));
                equip.setLeggings(makeUnbreakable(Material.CHAINMAIL_LEGGINGS, "&fSpodnie Tarczownika " + tag));
                equip.setBoots(makeUnbreakable(Material.IRON_BOOTS, "&fButy Tarczownika " + tag));
                equip.setItemInMainHand(makeUnbreakable(Material.IRON_SWORD, "&fMiecz Bojowy " + tag));
                equip.setItemInOffHand(makeUnbreakable(Material.SHIELD, "&fTarcza Wojskowa " + tag));
            }
            case CROSSBOWMAN -> {
                equip.setHelmet(makeUnbreakable(Material.CHAINMAIL_HELMET, "&fCzako Kusznika " + tag));
                equip.setChestplate(makeUnbreakable(Material.CHAINMAIL_CHESTPLATE, "&fKolczuga Kusznika " + tag));
                equip.setLeggings(makeUnbreakable(Material.LEATHER_LEGGINGS, "&fSpodnie Kusznika " + tag));
                equip.setBoots(makeUnbreakable(Material.CHAINMAIL_BOOTS, "&fButy Kusznika " + tag));
                equip.setItemInMainHand(makeUnbreakable(Material.CROSSBOW, "&fKusza Precyzyjna " + tag));
            }
            case SAPPER -> {
                equip.setHelmet(makeUnbreakable(Material.LEATHER_HELMET, "&6Kask Inżynieryjny " + tag));
                equip.setChestplate(makeUnbreakable(Material.LEATHER_CHESTPLATE, "&6Kubrak Sapera " + tag));
                equip.setLeggings(makeUnbreakable(Material.LEATHER_LEGGINGS, "&6Spodnie Sapera " + tag));
                equip.setBoots(makeUnbreakable(Material.LEATHER_BOOTS, "&6Trzewiki Sapera " + tag));
                equip.setItemInMainHand(makeUnbreakable(Material.IRON_PICKAXE, "&fKilof Burzący " + tag));
            }
        }
    }

    /**
     * Cykliczne utrzymanie stanu armii:
     * 1. Gasi podpalenie słoneczne szkieletów.
     * 2. Błyskawicznie przywraca niezniszczalny hełm i rynsztunek jednostkom, jeśli uległ on zniszczeniu lub zdjęciu.
     */
    public void tickArmyMaintenance() {
        for (UUID entityUuid : new ArrayList<>(unitsByEntityUuid.keySet())) {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                removeUnit(entityUuid);
                continue;
            }

            MilitaryUnit unit = unitsByEntityUuid.get(entityUuid);
            if (unit == null) continue;

            if (living.getFireTicks() > 0) {
                living.setFireTicks(0);
            }

            EntityEquipment equip = living.getEquipment();
            if (equip != null) {
                equip.setHelmetDropChance(0.0f);
                equip.setChestplateDropChance(0.0f);
                equip.setLeggingsDropChance(0.0f);
                equip.setBootsDropChance(0.0f);
                equip.setItemInMainHandDropChance(0.0f);
                equip.setItemInOffHandDropChance(0.0f);

                ItemStack helmet = equip.getHelmet();
                if (helmet == null || helmet.getType().isAir() || helmet.getItemMeta() == null || !helmet.getItemMeta().isUnbreakable()) {
                    Nation nation = nationManager.getNation(unit.getNationId());
                    equipSoldier(living, unit.getType(), nation);
                }
            }

            if (living instanceof Mob mob && mob.isValid()) {
                // 1. Obsługa rozkazu marszu na pozycję
                Location targetLoc = unit.getCommandTargetLocation();
                if (targetLoc != null) {
                    if (targetLoc.getWorld() != null && targetLoc.getWorld().equals(mob.getWorld())) {
                        double distSq = mob.getLocation().distanceSquared(targetLoc);
                        if (distSq <= 6.25) { // dotarł w obręb celu (< 2.5 bloku)
                            unit.setCommandTargetLocation(null);
                            mob.getPathfinder().stopPathfinding();
                            mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, 1.8, 0), 4, 0.2, 0.2, 0.2, 0);
                        } else {
                            mob.setTarget(null);
                            mob.getPathfinder().moveTo(targetLoc, 1.35);
                        }
                    } else {
                        unit.setCommandTargetLocation(null);
                    }
                }

                // 2. Obsługa rozkazu ataku na cel
                UUID targetEntityId = unit.getCommandTargetEntity();
                if (targetEntityId != null) {
                    Entity tEntity = Bukkit.getEntity(targetEntityId);
                    if (tEntity instanceof LivingEntity livingTarget && !livingTarget.isDead() && livingTarget.isValid()) {
                        if (mob.getTarget() == null || !mob.getTarget().equals(livingTarget)) {
                            mob.setTarget(livingTarget);
                        }
                        mob.getPathfinder().moveTo(livingTarget.getLocation(), 1.4);
                    } else {
                        unit.setCommandTargetEntity(null);
                    }
                }
            }
        }
    }
}
