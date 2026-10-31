package pl.sovereignty.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import pl.sovereignty.manager.MilitaryManager;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.manager.PaoloManager;
import pl.sovereignty.manager.PirateRaidManager;
import pl.sovereignty.manager.WarManager;
import pl.sovereignty.model.GeneralType;
import pl.sovereignty.model.MilitaryUnit;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PaoloGeneral;
import pl.sovereignty.model.PirateRaid;

public class PaoloCombatListener implements Listener {
    private final PaoloManager paoloManager;
    private final MilitaryManager militaryManager;
    private final PirateRaidManager pirateRaidManager;
    private final NationManager nationManager;
    private final WarManager warManager;

    public PaoloCombatListener(PaoloManager paoloManager, MilitaryManager militaryManager,
                               PirateRaidManager pirateRaidManager, NationManager nationManager,
                               WarManager warManager) {
        this.paoloManager = paoloManager;
        this.militaryManager = militaryManager;
        this.pirateRaidManager = pirateRaidManager;
        this.nationManager = nationManager;
        this.warManager = warManager;
    }

    /**
     * Zapobieganie agresji żołnierzy i Generała Paolo wobec sojuszników,
     * rekrutujących graczy i osób neutralnych.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        LivingEntity target = event.getTarget();
        if (target == null) return;

        // 1. Sprawdź czy to żołnierz państwowy
        MilitaryUnit unit = militaryManager.getUnit(entity.getUniqueId());
        if (unit != null) {
            // Nigdy nie atakuj gracza z tego samego państwa!
            if (target instanceof Player player) {
                Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
                if (playerNation != null && playerNation.getId().equals(unit.getNationId())) {
                    event.setCancelled(true);
                    event.setTarget(null);
                    return;
                }
            }

            // Nigdy nie atakuj sojuszniczego żołnierza
            MilitaryUnit targetUnit = militaryManager.getUnit(target.getUniqueId());
            if (targetUnit != null && targetUnit.getNationId().equals(unit.getNationId())) {
                event.setCancelled(true);
                event.setTarget(null);
                return;
            }

            // Nigdy nie atakuj sojuszniczego Generała Paolo
            PaoloGeneral targetGeneral = paoloManager.getGeneralByEntity(target.getUniqueId());
            if (targetGeneral != null && targetGeneral.getNationId().equals(unit.getNationId())) {
                event.setCancelled(true);
                event.setTarget(null);
                return;
            }

            // Jeśli jednostka ma wyznaczony cel przez Buławę Marszałkowską:
            if (unit.getCommandTargetEntity() != null) {
                if (!unit.getCommandTargetEntity().equals(target.getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                // Brak bezpośredniego rozkazu: nie atakuj gracza, jeśli nie trwa z nim oficjalna wojna
                if (target instanceof Player player) {
                    Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
                    boolean atWar = (playerNation != null && warManager.isAtWar(unit.getNationId(), playerNation.getId()));
                    if (!atWar) {
                        event.setCancelled(true);
                        event.setTarget(null);
                        return;
                    }
                }
            }
        }

        // 2. Sprawdź czy to Generał Paolo Or'e
        PaoloGeneral general = paoloManager.getGeneralByEntity(entity.getUniqueId());
        if (general != null) {
            // Nigdy nie atakuj członka swojego państwa
            if (target instanceof Player player) {
                Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
                if (playerNation != null && playerNation.getId().equals(general.getNationId())) {
                    event.setCancelled(true);
                    event.setTarget(null);
                    return;
                }
                // Nie atakuj graczy neutralnych bez wojny
                boolean atWar = (playerNation != null && warManager.isAtWar(general.getNationId(), playerNation.getId()));
                if (!atWar) {
                    event.setCancelled(true);
                    event.setTarget(null);
                    return;
                }
            }

            // Nigdy nie atakuj żołnierzy swojego państwa
            MilitaryUnit targetUnit = militaryManager.getUnit(target.getUniqueId());
            if (targetUnit != null && targetUnit.getNationId().equals(general.getNationId())) {
                event.setCancelled(true);
                event.setTarget(null);
                return;
            }
        }
    }

    /**
     * Blokada Friendly-Fire, ochrona przyboczna (Bodyguard) i ciosy krytyczne Paolo
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity realDamager = event.getDamager();
        if (realDamager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            realDamager = shooter;
        }

        Entity victim = event.getEntity();

        // 1. Atakujący to żołnierz państwowy
        MilitaryUnit damagerUnit = militaryManager.getUnit(realDamager.getUniqueId());
        if (damagerUnit != null) {
            // Ofiara to gracz z tego samego państwa
            if (victim instanceof Player victimPlayer) {
                Nation playerNation = nationManager.getNationByPlayer(victimPlayer.getUniqueId());
                if (playerNation != null && playerNation.getId().equals(damagerUnit.getNationId())) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Ofiara to żołnierz z tego samego państwa
            MilitaryUnit victimUnit = militaryManager.getUnit(victim.getUniqueId());
            if (victimUnit != null && victimUnit.getNationId().equals(damagerUnit.getNationId())) {
                event.setCancelled(true);
                return;
            }
            // Ofiara to Generał Paolo z tego samego państwa
            PaoloGeneral victimGeneral = paoloManager.getGeneralByEntity(victim.getUniqueId());
            if (victimGeneral != null && victimGeneral.getNationId().equals(damagerUnit.getNationId())) {
                event.setCancelled(true);
                return;
            }

            // Zdolności specjalne generałów podczas ataku wojsk
            PaoloGeneral general = paoloManager.getGeneral(damagerUnit.getNationId());
            if (general != null) {
                LivingEntity generalEntity = (LivingEntity) Bukkit.getEntity(general.getEntityUuid());
                if (generalEntity != null && generalEntity.getLocation().distance(realDamager.getLocation()) <= 18.0) {
                    int hitCount = general.incrementHitCounter();

                    if (general.getType() == GeneralType.KLOT_NAPOLEON) {
                        // Wielka Bateria Napoleońska: salwa artyleryjska co 5. cios
                        if (hitCount % 5 == 0) {
                            Location targetLoc = victim.getLocation();
                            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
                            targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc.clone().add(0, 1, 0), 2);
                            targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.clone().add(0, 1, 0), 20, 0.6, 0.5, 0.6, 0.1);

                            for (Entity nearby : targetLoc.getWorld().getNearbyEntities(targetLoc, 4.0, 3.0, 4.0)) {
                                if (nearby instanceof LivingEntity livingEnemy && !nearby.getUniqueId().equals(realDamager.getUniqueId())) {
                                    boolean isFriendly = false;
                                    if (livingEnemy instanceof Player p) {
                                        Nation pn = nationManager.getNationByPlayer(p.getUniqueId());
                                        if (pn != null && pn.getId().equals(general.getNationId())) isFriendly = true;
                                    } else if (militaryManager.getUnit(livingEnemy.getUniqueId()) != null) {
                                        MilitaryUnit u = militaryManager.getUnit(livingEnemy.getUniqueId());
                                        if (u.getNationId().equals(general.getNationId())) isFriendly = true;
                                    }
                                    if (!isFriendly) {
                                        livingEnemy.damage(6.0, realDamager);
                                        livingEnemy.setFireTicks(80);
                                    }
                                }
                            }

                            if (victim instanceof Player targetPlayer) {
                                targetPlayer.sendMessage(ChatColor.GOLD + "[WIELKA BATERIA] " + ChatColor.RED + "Salwa artylerii Generała Kłota Napoleona rozbiła Twoją pozycję!");
                            }
                        }
                    } else {
                        // Paolo: Krok Bojowy (co 4. uderzenie)
                        if (hitCount % 4 == 0) {
                            event.setDamage(event.getDamage() * 1.6);
                            Location targetLoc = victim.getLocation();
                            targetLoc.getWorld().playSound(targetLoc, Sound.ITEM_SHIELD_BREAK, 1.2f, 0.9f);
                            targetLoc.getWorld().spawnParticle(Particle.CRIT, targetLoc.clone().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.2);

                            if (victim instanceof Player targetPlayer) {
                                targetPlayer.setCooldown(Material.SHIELD, 100);
                                targetPlayer.sendMessage(ChatColor.RED + "[KROK BOJOWY] Zsynchronizowane uderzenie wojsk Generała Paolo Or'e rozbiło Twoją tarczę!");
                            }
                        }
                    }
                }
            }
        }

        // 2. Atakujący to Generał
        PaoloGeneral damagerGeneral = paoloManager.getGeneralByEntity(realDamager.getUniqueId());
        if (damagerGeneral != null) {
            if (victim instanceof Player victimPlayer) {
                Nation playerNation = nationManager.getNationByPlayer(victimPlayer.getUniqueId());
                if (playerNation != null && playerNation.getId().equals(damagerGeneral.getNationId())) {
                    event.setCancelled(true);
                    return;
                }
            }
            MilitaryUnit victimUnit = militaryManager.getUnit(victim.getUniqueId());
            if (victimUnit != null && victimUnit.getNationId().equals(damagerGeneral.getNationId())) {
                event.setCancelled(true);
                return;
            }

            // Dodatkowy efekt ognia i salwy, gdy sam Napoleon wyprowadza atak
            if (damagerGeneral.getType() == GeneralType.KLOT_NAPOLEON) {
                victim.setFireTicks(60); // 3 sekundy ognia
                int hitCount = damagerGeneral.incrementHitCounter();
                if (hitCount % 4 == 0) {
                    Location targetLoc = victim.getLocation();
                    targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 1.3f);
                    targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc.clone().add(0, 1, 0), 2);
                    targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.clone().add(0, 1, 0), 20, 0.6, 0.5, 0.6, 0.1);
                    event.setDamage(event.getDamage() + 4.0);
                }
            }
        }

        // 3. Gracz atakuje własne wojska (przypadkowy cios)
        if (realDamager instanceof Player player) {
            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            if (playerNation != null) {
                MilitaryUnit victimUnit = militaryManager.getUnit(victim.getUniqueId());
                if (victimUnit != null && victimUnit.getNationId().equals(playerNation.getId())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "To żołnierz Twojego państwa! Nie atakuj własnych wojsk.");
                    return;
                }
                PaoloGeneral victimGeneral = paoloManager.getGeneralByEntity(victim.getUniqueId());
                if (victimGeneral != null && victimGeneral.getNationId().equals(playerNation.getId())) {
                    event.setCancelled(true);
                    String genName = victimGeneral.getType().getSimpleName();
                    player.sendMessage(ChatColor.RED + "To " + genName + " Twojego państwa! Nie atakuj swojego dowódcy.");
                    return;
                }
            }
        }

        // 4. Parowanie Krótką Tarczą Bojową (CustomModelData: 1001)
        if (victim instanceof Player defender && defender.isBlocking()) {
            ItemStack offHand = defender.getInventory().getItemInOffHand();
            ItemStack mainHand = defender.getInventory().getItemInMainHand();
            boolean hasShortShield = (offHand.getType() == Material.SHIELD && offHand.hasItemMeta() && offHand.getItemMeta().hasCustomModelData() && offHand.getItemMeta().getCustomModelData() == 1001)
                    || (mainHand.getType() == Material.SHIELD && mainHand.hasItemMeta() && mainHand.getItemMeta().hasCustomModelData() && mainHand.getItemMeta().getCustomModelData() == 1001);

            if (hasShortShield) {
                Location loc = defender.getLocation().add(0, 1.2, 0);
                defender.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.3, 0.3, 0.3, 0.1);
                defender.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.6f);

                if (realDamager instanceof LivingEntity damager) {
                    ItemStack weapon = damager.getEquipment() != null ? damager.getEquipment().getItemInMainHand() : null;
                    if (weapon != null && weapon.getType().name().endsWith("_AXE")) {
                        Bukkit.getScheduler().runTaskLater(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                            defender.setCooldown(Material.SHIELD, 50);
                        }, 1L);
                    }
                }
            }
        }

        // 4. Ochrona przyboczna (Bodyguard): Jeśli wróg uderzy obywatela/władcę, pobliscy żołnierze ruszają do walki
        if (victim instanceof Player victimPlayer) {
            Nation victimNation = nationManager.getNationByPlayer(victimPlayer.getUniqueId());
            if (victimNation != null && realDamager instanceof LivingEntity livingEnemy) {
                boolean isFriendly = false;
                if (livingEnemy instanceof Player pEnemy) {
                    Nation pNation = nationManager.getNationByPlayer(pEnemy.getUniqueId());
                    if (pNation != null && pNation.getId().equals(victimNation.getId())) {
                        isFriendly = true;
                    }
                }
                if (!isFriendly) {
                    for (MilitaryUnit unit : militaryManager.getNationUnits(victimNation.getId())) {
                        Entity uEntity = Bukkit.getEntity(unit.getEntityUuid());
                        if (uEntity instanceof Mob mob && mob.isValid()) {
                            if (mob.getLocation().distance(victimPlayer.getLocation()) <= 20.0) {
                                if (mob.getTarget() == null || !mob.getTarget().isValid()) {
                                    mob.setTarget(livingEnemy);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Samoobrona żołnierza: Jeśli żołnierz zostanie zaatakowany przez wroga
        MilitaryUnit victimUnit = militaryManager.getUnit(victim.getUniqueId());
        if (victimUnit != null && realDamager instanceof LivingEntity livingEnemy) {
            if (victim instanceof Mob mob && (mob.getTarget() == null || !mob.getTarget().isValid())) {
                mob.setTarget(livingEnemy);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        boolean isSoldier = entity.getPersistentDataContainer().has(militaryManager.KEY_SOLDIER_NATION, org.bukkit.persistence.PersistentDataType.STRING)
                || militaryManager.getUnit(entity.getUniqueId()) != null;
        boolean isGeneral = entity.getPersistentDataContainer().has(paoloManager.KEY_PAOLO_NATION, org.bukkit.persistence.PersistentDataType.STRING)
                || paoloManager.getGeneralByEntity(entity.getUniqueId()) != null;

        // Blokada dropu ekwipunku, hełmów, broni i expa z wojsk państwowych i wodzów
        if (isSoldier || isGeneral) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // 1. Śmierć Generała Paolo Or'e (Permadeath)
        PaoloGeneral general = paoloManager.getGeneralByEntity(entity.getUniqueId());
        if (general != null) {
            paoloManager.handlePaoloDeath(general, entity);
            return;
        }

        // 2. Śmierć pirata z rajdu
        PirateRaid raid = pirateRaidManager.getRaidByPirate(entity.getUniqueId());
        if (raid != null) {
            pirateRaidManager.checkRaidVictory(raid);
            return;
        }

        // 3. Śmierć jednostki wojskowej NPC
        if (isSoldier) {
            militaryManager.removeUnit(entity.getUniqueId());
        }
    }
}
