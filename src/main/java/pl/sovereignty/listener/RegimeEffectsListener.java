package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.sovereignty.manager.NationManager;
import pl.sovereignty.manager.TerritoryManager;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;

import java.util.Random;

/**
 * Event-based listener dla zaawansowanych buffów i debuffów bojowych/zbierackich ustrojów politycznych.
 */
public class RegimeEffectsListener implements Listener {

    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final Random random = new Random();

    public RegimeEffectsListener(NationManager nationManager, TerritoryManager territoryManager) {
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        // 1. Modyfikatory ataku (Gdy gracz zadaje obrażenia)
        if (event.getDamager() instanceof Player attacker) {
            Nation attackerNation = nationManager.getNationByPlayer(attacker.getUniqueId());
            if (attackerNation != null && !attackerNation.isIndebted()) {
                PoliticalRegime regime = attackerNation.getRegime();

                if (regime == PoliticalRegime.SPARTAN_STRATOCRACY || regime == PoliticalRegime.EXPANSIONIST_HEGEMONY) {
                    // +15% zadawanych obrażeń
                    event.setDamage(event.getDamage() * 1.15);
                } else if (regime == PoliticalRegime.MILITARISM || regime == PoliticalRegime.TOTALITARIAN_STATE) {
                    Nation owner = territoryManager.getNationAt(attacker.getLocation());
                    if (owner != null && owner.getId().equals(attackerNation.getId())) {
                        // +15% obrażeń w obronie własnego terytorium przed intruzami
                        event.setDamage(event.getDamage() * 1.15);
                    }
                } else if (regime == PoliticalRegime.AGRARIAN_COMMUNE) {
                    // Debuff pacyfizmu robotniczego: -15% zadawanych obrażeń w PvP
                    if (event.getEntity() instanceof Player) {
                        event.setDamage(event.getDamage() * 0.85);
                    }
                } else if (regime == PoliticalRegime.MERCHANT_REPUBLIC || regime == PoliticalRegime.PLUTOCRACY) {
                    // Debuff pacyfizmu handlowego i plutokratycznego: -10% obrażeń w PvP
                    if (event.getEntity() instanceof Player) {
                        event.setDamage(event.getDamage() * 0.90);
                    }
                } else if (regime == PoliticalRegime.MYSTIC_MAGOCRACY) {
                    // Szansa 25% na podpalenie celu mistycznym płomieniem
                    if (random.nextDouble() < 0.25 && event.getEntity() instanceof LivingEntity victim) {
                        victim.setFireTicks(80);
                    }
                }
            }
        }

        // 2. Modyfikatory obrony (Gdy gracz otrzymuje obrażenia)
        if (event.getEntity() instanceof Player victim) {
            Nation victimNation = nationManager.getNationByPlayer(victim.getUniqueId());
            if (victimNation != null && !victimNation.isIndebted()) {
                PoliticalRegime regime = victimNation.getRegime();

                if (regime == PoliticalRegime.MYSTIC_MAGOCRACY) {
                    // Debuff szklanej armaty: +20% obrażeń fizycznych od broni
                    event.setDamage(event.getDamage() * 1.20);
                } else if (regime == PoliticalRegime.FEUDAL_REALM && victim.isInsideVehicle()) {
                    // Bonus obronny kawalerii: -20% otrzymywanych obrażeń
                    event.setDamage(event.getDamage() * 0.80);
                } else if (regime == PoliticalRegime.CONSTITUTIONAL_REPUBLIC) {
                    Nation owner = territoryManager.getNationAt(victim.getLocation());
                    if (owner != null && owner.getId().equals(victimNation.getId())) {
                        // -15% otrzymywanych obrażeń na własnym terenie prawa
                        event.setDamage(event.getDamage() * 0.85);
                    }
                } else if (regime == PoliticalRegime.ANARCHO_SYNDICALISM) {
                    // Słabszy pancerz bez dyscypliny wojskowej
                    event.setDamage(event.getDamage() * 1.10);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Nation nation = nationManager.getNationByPlayer(killer.getUniqueId());
        if (nation == null || nation.isIndebted()) return;

        PoliticalRegime regime = nation.getRegime();

        // 1. Spartańska Stratokracja - szał bojowy i leczenie po zabójstwie
        if (regime == PoliticalRegime.SPARTAN_STRATOCRACY) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        }

        // 2. Magokracja - 2x więcej EXP za każdego zabitego potwora
        if (regime == PoliticalRegime.MYSTIC_MAGOCRACY) {
            event.setDroppedExp(event.getDroppedExp() * 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null || nation.isIndebted()) return;

        PoliticalRegime regime = nation.getRegime();

        // Plutokracja - 25% szansy na złote samorodki przy wydobyciu rud
        if (regime == PoliticalRegime.PLUTOCRACY) {
            Material type = event.getBlock().getType();
            if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE ||
                type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE ||
                type == Material.NETHER_GOLD_ORE) {
                if (random.nextDouble() < 0.25 && event.getBlock().getWorld() != null) {
                    int count = 1 + random.nextInt(3);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                            new ItemStack(Material.GOLD_NUGGET, count));
                }
            }
        }
    }
}
