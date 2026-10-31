package pl.sovereignty.manager;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.sovereignty.model.GeneralType;
import pl.sovereignty.model.MilitaryUnit;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PaoloGeneral;
import pl.sovereignty.util.CustomItemRegistry;
import pl.sovereignty.util.ParticleUtil;

import java.util.*;

public class PaoloManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final MilitaryManager militaryManager;
    private final CustomItemRegistry items;
    private final Map<UUID, PaoloGeneral> generalsByNation = new HashMap<>();
    private final Map<UUID, BossBar> bossBarsByNation = new HashMap<>();

    public final NamespacedKey KEY_PAOLO_NATION;
    public final NamespacedKey KEY_GENERAL_TYPE;

    public PaoloManager(Plugin plugin, NationManager nationManager, MilitaryManager militaryManager, CustomItemRegistry items) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.militaryManager = militaryManager;
        this.items = items;
        this.KEY_PAOLO_NATION = new NamespacedKey(plugin, "paolo_nation");
        this.KEY_GENERAL_TYPE = new NamespacedKey(plugin, "general_type");
    }

    public boolean canSignContract(Player player) {
        return canSignPaoloContract(player);
    }

    public boolean canSignPaoloContract(Player player) {
        return player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 16)
                && player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), 8)
                && player.getInventory().containsAtLeast(new ItemStack(Material.BELL), 1)
                && player.getInventory().containsAtLeast(new ItemStack(Material.BREAD), 32);
    }

    public boolean canSignNapoleonContract(Player player) {
        return player.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), 24)
                && player.getInventory().containsAtLeast(new ItemStack(Material.GUNPOWDER), 16)
                && player.getInventory().containsAtLeast(new ItemStack(Material.SPYGLASS), 1)
                && player.getInventory().containsAtLeast(new ItemStack(Material.BREAD), 32);
    }

    public PaoloGeneral summonGeneral(Player player, Nation nation, Location loc) {
        return summonPaolo(player, nation, loc);
    }

    public PaoloGeneral summonPaolo(Player player, Nation nation, Location loc) {
        if (generalsByNation.containsKey(nation.getId())) {
            player.sendMessage(ChatColor.RED + "Twoje państwo posiada już aktywnego wodza polowego!");
            return null;
        }

        if (!canSignPaoloContract(player)) {
            player.sendMessage(ChatColor.RED + "Wymogi kontraktu Paolo: 16 Żelaza, 8 Złota, 1 Dzwon i 32 Chleby!");
            return null;
        }

        player.getInventory().removeItem(new ItemStack(Material.IRON_INGOT, 16));
        player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, 8));
        player.getInventory().removeItem(new ItemStack(Material.BELL, 1));
        player.getInventory().removeItem(new ItemStack(Material.BREAD, 32));

        World world = loc.getWorld();
        WitherSkeleton generalEntity = (WitherSkeleton) world.spawnEntity(loc, EntityType.WITHER_SKELETON);
        generalEntity.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&6&lGenerał Paolo Or'e &8[&f" + nation.getTag() + "&8]"));
        generalEntity.setCustomNameVisible(true);
        generalEntity.setRemoveWhenFarAway(false);
        generalEntity.setTarget(null);

        try {
            Bukkit.getMobGoals().removeAllGoals(generalEntity, GoalType.TARGET);
        } catch (Throwable ignored) {}

        generalEntity.getPersistentDataContainer().set(KEY_PAOLO_NATION, PersistentDataType.STRING, nation.getId().toString());
        generalEntity.getPersistentDataContainer().set(KEY_GENERAL_TYPE, PersistentDataType.STRING, GeneralType.PAOLO_ORE.name());

        // Ustawienie 150 HP
        if (generalEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            generalEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(150.0);
        }
        generalEntity.setHealth(150.0);

        EntityEquipment equip = generalEntity.getEquipment();
        if (equip != null) {
            equip.setHelmetDropChance(0.0f);
            equip.setChestplateDropChance(0.0f);
            equip.setLeggingsDropChance(0.0f);
            equip.setBootsDropChance(0.0f);
            equip.setItemInMainHandDropChance(0.0f);
            equip.setItemInOffHandDropChance(0.0f);

            equip.setHelmet(MilitaryManager.makeUnbreakable(Material.GOLDEN_HELMET, "&6Złoty Hełm Generała Paolo"));
            equip.setChestplate(MilitaryManager.makeUnbreakable(Material.DIAMOND_CHESTPLATE, "&bNapierśnik Wodza Paolo"));
            equip.setLeggings(MilitaryManager.makeUnbreakable(Material.IRON_LEGGINGS, "&7Nagolenice Wojenne"));
            equip.setBoots(MilitaryManager.makeUnbreakable(Material.DIAMOND_BOOTS, "&bTrzewiki Zwycięstwa"));
            equip.setItemInMainHand(MilitaryManager.makeUnbreakable(Material.MACE, "&6Buława Rozbijająca"));
            equip.setItemInOffHand(MilitaryManager.makeUnbreakable(Material.SHIELD, "&6Pawęż Paolo Or'e"));
        }

        PaoloGeneral general = new PaoloGeneral(nation.getId(), generalEntity.getUniqueId(), GeneralType.PAOLO_ORE);
        generalsByNation.put(nation.getId(), general);

        BossBar bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&',
                "&6Generał Paolo Or'e &7(" + nation.getName() + ")"), BarColor.YELLOW, BarStyle.SOLID);
        bar.setProgress(1.0);
        for (UUID memberUuid : nation.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null) bar.addPlayer(m);
        }
        bossBarsByNation.put(nation.getId(), bar);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l[POWOŁANIE WODZA] &ePaństwo &f" + nation.getName()
                + " &epowołało legendarnego wodza: &6Generała Paolo Or'e&e!\n"
                + "&cObowiązuje bezwzględna zasada Permadeath — śmierć generała jest ostateczna!\n"));

        world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.8f);
        return general;
    }

    public PaoloGeneral summonNapoleon(Player player, Nation nation, Location loc) {
        if (generalsByNation.containsKey(nation.getId())) {
            player.sendMessage(ChatColor.RED + "Twoje państwo posiada już aktywnego wodza polowego!");
            return null;
        }

        if (!canSignNapoleonContract(player)) {
            player.sendMessage(ChatColor.RED + "Wymogi kontraktu Napoleona: 24 Złota, 16 Prochu, 1 Luneta (Spyglass) i 32 Chleby!");
            return null;
        }

        player.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, 24));
        player.getInventory().removeItem(new ItemStack(Material.GUNPOWDER, 16));
        player.getInventory().removeItem(new ItemStack(Material.SPYGLASS, 1));
        player.getInventory().removeItem(new ItemStack(Material.BREAD, 32));

        World world = loc.getWorld();
        Stray generalEntity = (Stray) world.spawnEntity(loc, EntityType.STRAY);
        generalEntity.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&e&lGenerał Kłot Napoleon Ciepły &8[&f" + nation.getTag() + "&8]"));
        generalEntity.setCustomNameVisible(true);
        generalEntity.setRemoveWhenFarAway(false);
        generalEntity.setTarget(null);

        try {
            Bukkit.getMobGoals().removeAllGoals(generalEntity, GoalType.TARGET);
        } catch (Throwable ignored) {}

        generalEntity.getPersistentDataContainer().set(KEY_PAOLO_NATION, PersistentDataType.STRING, nation.getId().toString());
        generalEntity.getPersistentDataContainer().set(KEY_GENERAL_TYPE, PersistentDataType.STRING, GeneralType.KLOT_NAPOLEON.name());

        // Ustawienie 120 HP
        if (generalEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            generalEntity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(120.0);
        }
        generalEntity.setHealth(120.0);

        EntityEquipment equip = generalEntity.getEquipment();
        if (equip != null) {
            equip.setHelmetDropChance(0.0f);
            equip.setChestplateDropChance(0.0f);
            equip.setLeggingsDropChance(0.0f);
            equip.setBootsDropChance(0.0f);
            equip.setItemInMainHandDropChance(0.0f);
            equip.setItemInOffHandDropChance(0.0f);

            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            if (helmet.getItemMeta() instanceof LeatherArmorMeta meta) {
                meta.setUnbreakable(true);
                meta.setColor(Color.fromRGB(20, 25, 60)); // Granatowo-czarny Bicorne Napoleona
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9&lBicorne Cesarza Napoleona"));
                helmet.setItemMeta(meta);
            }
            equip.setHelmet(helmet);

            ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
            ItemMeta cMeta = chest.getItemMeta();
            if (cMeta != null) {
                cMeta.setUnbreakable(true);
                cMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lMundur Wielkiej Armii Ciepłej"));
                chest.setItemMeta(cMeta);
            }
            equip.setChestplate(chest);

            ItemStack legs = new ItemStack(Material.GOLDEN_LEGGINGS);
            ItemMeta lMeta = legs.getItemMeta();
            if (lMeta != null) {
                lMeta.setUnbreakable(true);
                lMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lSpodnie Sztabowe z Austerlitz"));
                legs.setItemMeta(lMeta);
            }
            equip.setLeggings(legs);

            ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
            ItemMeta bMeta = boots.getItemMeta();
            if (bMeta != null) {
                bMeta.setUnbreakable(true);
                bMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8&lOficerki Marszowe Cesarza"));
                boots.setItemMeta(bMeta);
            }
            equip.setBoots(boots);

            ItemStack saber = new ItemStack(Material.GOLDEN_SWORD);
            saber.addEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
            saber.addEnchantment(org.bukkit.enchantments.Enchantment.KNOCKBACK, 1);
            saber.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 4);
            ItemMeta sMeta = saber.getItemMeta();
            if (sMeta != null) {
                sMeta.setUnbreakable(true);
                sMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lSzabla Austerlitz Napoleona"));
                saber.setItemMeta(sMeta);
            }
            equip.setItemInMainHand(saber);
            ItemStack spyglass = new ItemStack(Material.SPYGLASS);
            ItemMeta spMeta = spyglass.getItemMeta();
            if (spMeta != null) {
                spMeta.setUnbreakable(true);
                spMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lLuneta Taktyczna Cesarza"));
                spyglass.setItemMeta(spMeta);
            }
            equip.setItemInOffHand(spyglass);
        }

        // Stałe perki Kłota Napoleona: Szybki (Speed II), Siła I (Strength I), Odporność na ogień (Ciepły)
        generalEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        generalEntity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
        generalEntity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        PaoloGeneral general = new PaoloGeneral(nation.getId(), generalEntity.getUniqueId(), GeneralType.KLOT_NAPOLEON);
        generalsByNation.put(nation.getId(), general);

        BossBar bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&',
                "&e&lGenerał Kłot Napoleon Ciepły &7(" + nation.getName() + ")"), BarColor.YELLOW, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        for (UUID memberUuid : nation.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(memberUuid);
            if (m != null) bar.addPlayer(m);
        }
        bossBarsByNation.put(nation.getId(), bar);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l[POWOŁANIE CESARZA TAKTYKI] &ePaństwo &f" + nation.getName()
                + " &epowołało legendarnego wodza: &e&lGenerała Kłota Napoleona Ciepłego&e!\n"
                + "&7Taktyczny geniusz, Siła I, błyskawiczny manewr i ogień artylerii Wielkiej Armii!\n"
                + "&cObowiązuje bezwzględna zasada Permadeath — śmierć generała jest ostateczna!\n"));

        world.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_1, 1.5f, 1.0f);
        return general;
    }

    public PaoloGeneral getGeneral(UUID nationId) {
        return generalsByNation.get(nationId);
    }

    public PaoloGeneral getGeneralByEntity(UUID entityUuid) {
        for (PaoloGeneral g : generalsByNation.values()) {
            if (g.getEntityUuid().equals(entityUuid)) return g;
        }

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof LivingEntity living) {
            String nationIdStr = living.getPersistentDataContainer().get(KEY_PAOLO_NATION, PersistentDataType.STRING);
            if (nationIdStr != null) {
                try {
                    UUID nationId = UUID.fromString(nationIdStr);
                    String typeStr = living.getPersistentDataContainer().get(KEY_GENERAL_TYPE, PersistentDataType.STRING);
                    GeneralType type = typeStr != null ? GeneralType.valueOf(typeStr) : GeneralType.PAOLO_ORE;
                    PaoloGeneral general = new PaoloGeneral(nationId, entityUuid, type);
                    generalsByNation.put(nationId, general);
                    return general;
                } catch (Exception ignored) {}
            }

            String customName = living.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                for (Nation n : nationManager.getAllNations()) {
                    if (customName.contains("[" + n.getTag() + "]")) {
                        GeneralType type = customName.contains("Napoleon") ? GeneralType.KLOT_NAPOLEON : GeneralType.PAOLO_ORE;
                        PaoloGeneral general = new PaoloGeneral(n.getId(), entityUuid, type);
                        generalsByNation.put(n.getId(), general);
                        living.getPersistentDataContainer().set(KEY_PAOLO_NATION, PersistentDataType.STRING, n.getId().toString());
                        living.getPersistentDataContainer().set(KEY_GENERAL_TYPE, PersistentDataType.STRING, type.name());
                        if (living instanceof Mob mob) {
                            mob.setTarget(null);
                            try {
                                Bukkit.getMobGoals().removeAllGoals(mob, GoalType.TARGET);
                            } catch (Throwable ignored) {}
                        }
                        return general;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Aura wodza (wywoływane co 2 sekundy):
     * Paolo: bęben i Ostatni Szaniec
     * Napoleon: ciepły ogień, Speed I dla armii, ochrona przed mrozem i manewr Austerlitz
     */
    public void tickDrumbeatAura() {
        for (PaoloGeneral general : generalsByNation.values()) {
            Entity entity = Bukkit.getEntity(general.getEntityUuid());
            if (!(entity instanceof LivingEntity living) || living.isDead()) continue;

            Location loc = living.getLocation();

            // Aktualizacja paska BossBar
            BossBar bar = bossBarsByNation.get(general.getNationId());
            if (bar != null) {
                double health = living.getHealth();
                double maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                bar.setProgress(Math.max(0.0, Math.min(1.0, health / maxHealth)));
            }

            if (general.getType() == GeneralType.KLOT_NAPOLEON) {
                // Efekty Kłota Napoleona Ciepłego
                loc.getWorld().playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.8f, 1.2f);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.02);
                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 1.5, 0), 2, 0.2, 0.3, 0.2, 0.01);

                // Aura Ciepłego Manewru: Sojusznicy w promieniu 16 kratek
                for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 16, 8, 16)) {
                    if (nearby instanceof LivingEntity allied) {
                        boolean isAllied = false;
                        if (allied instanceof Player p) {
                            Nation pn = nationManager.getNationByPlayer(p.getUniqueId());
                            if (pn != null && pn.getId().equals(general.getNationId())) isAllied = true;
                        } else if (militaryManager.getUnit(allied.getUniqueId()) != null) {
                            MilitaryUnit u = militaryManager.getUnit(allied.getUniqueId());
                            if (u.getNationId().equals(general.getNationId())) isAllied = true;
                        }
                        if (isAllied) {
                            allied.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0)); // Speed I
                            allied.setFreezeTicks(0);
                        }
                    }
                }

                // Manewr Austerlitz: gdy HP < 35% (< 42 HP)
                if (living.getHealth() < 42.0 && !general.isLastStandTriggered()) {
                    general.setLastStandTriggered(true);
                    loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.8f);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc.clone().add(0, 1, 0), 30, 0.8, 1.0, 0.8, 0.1);

                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&e&l[MANEWR POD AUSTERLITZ] &6Generał Kłot Napoleon Ciepły: "
                            + "&c„Żołnierze! Błyskawiczny manewr oskrzydlający! Zaskoczyć wroga!” "
                            + "&eWszystkie sojusznicze jednostki zyskują Prędkość III i Regenerację!"));

                    for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 20, 10, 20)) {
                        if (nearby instanceof LivingEntity allied) {
                            boolean isAllied = false;
                            if (allied instanceof Player p) {
                                Nation pn = nationManager.getNationByPlayer(p.getUniqueId());
                                if (pn != null && pn.getId().equals(general.getNationId())) isAllied = true;
                            } else if (militaryManager.getUnit(allied.getUniqueId()) != null) {
                                MilitaryUnit u = militaryManager.getUnit(allied.getUniqueId());
                                if (u.getNationId().equals(general.getNationId())) isAllied = true;
                            }
                            if (isAllied) {
                                allied.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2)); // Speed III na 10s
                                allied.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                            }
                        }
                    }
                }
            } else {
                // Paolo: Bęben wojenny i Ostatni Szaniec
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.6f);
                ParticleUtil.spawnDrumRing(loc, 4.0);

                if (living.getHealth() < 45.0 && !general.isLastStandTriggered()) {
                    triggerLastStand(general, living);
                }

                if (general.getLastStandStandardLocation() != null) {
                    Location bannerLoc = general.getLastStandStandardLocation();
                    ParticleUtil.spawnLastStandZone(bannerLoc, 12.0);

                    for (Entity nearby : bannerLoc.getWorld().getNearbyEntities(bannerLoc, 12, 6, 12)) {
                        if (nearby instanceof LivingEntity allied) {
                            boolean isAllied = false;
                            if (allied instanceof Player p) {
                                Nation pn = nationManager.getNationByPlayer(p.getUniqueId());
                                if (pn != null && pn.getId().equals(general.getNationId())) isAllied = true;
                            } else if (militaryManager.getUnit(allied.getUniqueId()) != null) {
                                MilitaryUnit u = militaryManager.getUnit(allied.getUniqueId());
                                if (u.getNationId().equals(general.getNationId())) isAllied = true;
                            }
                            if (isAllied) {
                                allied.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2)); // 60% redukcji
                            }
                        }
                    }
                }
            }
        }
    }

    private void triggerLastStand(PaoloGeneral general, LivingEntity living) {
        general.setLastStandTriggered(true);
        Location loc = living.getLocation();
        general.setLastStandStandardLocation(loc);

        loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.5f, 0.7f);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "&4&l[OSTATNI SZANIEC] &cGenerał Paolo Or'e wbija sztandar w grunt! "
                + "Powstała strefa redukująca obrażenia sojuszniczych wojsk o 60%!"));
    }

    /**
     * Zdolność specjalna wodza:
     * Paolo: Werbel Wojny (podwojenie obrażeń machin, blokada regeneracji wrogów)
     * Napoleon: Szarża Gwardii Cesarskiej (Speed II + Siła I dla całej armii)
     */
    public void activateDrumrollOfWar(Player commander, PaoloGeneral general) {
        general.activateDrumroll();
        Entity entity = Bukkit.getEntity(general.getEntityUuid());
        if (entity instanceof LivingEntity living) {
            Location loc = living.getLocation();

            if (general.getType() == GeneralType.KLOT_NAPOLEON) {
                loc.getWorld().playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_1, 2.0f, 1.0f);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 2, 0), 40, 1.0, 1.0, 1.0, 0.15);

                for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 30, 12, 30)) {
                    if (nearby instanceof LivingEntity allied) {
                        boolean isAllied = false;
                        if (allied instanceof Player p) {
                            Nation pn = nationManager.getNationByPlayer(p.getUniqueId());
                            if (pn != null && pn.getId().equals(general.getNationId())) isAllied = true;
                        } else if (militaryManager.getUnit(allied.getUniqueId()) != null) {
                            MilitaryUnit u = militaryManager.getUnit(allied.getUniqueId());
                            if (u.getNationId().equals(general.getNationId())) isAllied = true;
                        }
                        if (isAllied) {
                            allied.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1)); // Speed II na 30s
                            allied.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0)); // Strength I na 30s
                        }
                    }
                }

                commander.sendMessage(ChatColor.GOLD + "[SZARŻA GWARDII CESARSKIEJ] Generał Kłot Napoleon Ciepły nadał wojskom Prędkość II i Siłę I na 30 sekund!");
            } else {
                loc.getWorld().playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_2, 2.0f, 0.9f);
                loc.getWorld().spawnParticle(Particle.FLASH, loc.add(0, 2, 0), 3);

                for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 25, 10, 25)) {
                    if (nearby instanceof Player enemy && !enemy.getUniqueId().equals(commander.getUniqueId())) {
                        Nation enemyNation = nationManager.getNationByPlayer(enemy.getUniqueId());
                        if (enemyNation == null || !enemyNation.getId().equals(general.getNationId())) {
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 0));
                            enemy.sendMessage(ChatColor.RED + "[WERBEL WOJNY] Generał Paolo Or'e wyłączył Twoją regenerację zdrowia!");
                        }
                    }
                }

                commander.sendMessage(ChatColor.GOLD + "[WERBEL WOJNY] Podwojono obrażenia machin i osłabiono obrońców na 30 sekund!");
            }
        }
    }

    /**
     * Śmierć Wodza Polowego — Zasada Permadeath
     */
    public void handlePaoloDeath(PaoloGeneral general, LivingEntity entity) {
        Nation nation = nationManager.getNation(general.getNationId());
        String nationName = nation != null ? nation.getName() : "Nieznane Państwo";

        BossBar bar = bossBarsByNation.remove(general.getNationId());
        if (bar != null) bar.removeAll();

        generalsByNation.remove(general.getNationId());

        Location dropLoc = entity.getLocation();
        dropLoc.getWorld().dropItemNaturally(dropLoc, items.getBrokenRhythmStandard());

        militaryManager.triggerArmyPanic(general.getNationId());

        if (general.getType() == GeneralType.KLOT_NAPOLEON) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&4&l========================================\n"
                    + "&4&l[PERMADEATH: POLEGŁ CESARZ TAKTYKI]\n"
                    + "&cGenerał Kłot Napoleon Ciepły z państwa &f" + nationName + " &czginął na polu walki pod Waterloo!\n"
                    + "&7Jego płonąca szabla i taktyczny geniusz gasną na zawsze.\n"
                    + "&eWojska NPC wpadły w panikę na 60 sekund!\n"
                    + "&4&l========================================\n"));
            dropLoc.getWorld().playSound(dropLoc, Sound.ENTITY_WITHER_DEATH, 1.2f, 0.7f);
        } else {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&4&l========================================\n"
                    + "&4&l[PERMADEATH: POLEGŁY WÓDZ]\n"
                    + "&cGenerał Paolo Or'e z państwa &f" + nationName + " &czginął na polu walki!\n"
                    + "&7Na ziemię upadł &4Złamany Sztandar Rytmu&7 jako trofeum dla wrogów.\n"
                    + "&eWojska NPC wpadły w panikę na 60 sekund!\n"
                    + "&4&l========================================\n"));
            dropLoc.getWorld().playSound(dropLoc, Sound.ENTITY_WITHER_DEATH, 1.2f, 0.7f);
        }
    }
}
