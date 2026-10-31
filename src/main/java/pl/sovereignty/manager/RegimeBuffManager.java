package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;

/**
 * Pasywny menedżer cyklicznych buffów i debuffów ustrojowych dla obywateli nacji.
 */
public class RegimeBuffManager {
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;

    public RegimeBuffManager(NationManager nationManager, TerritoryManager territoryManager) {
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
    }

    public void tickBuffs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            Nation territoryOwner = territoryManager.getNationAt(player.getLocation());

            // 1. Wykrywanie intruzów przez państwa autorytarne (Inwigilacja graniczna & Detektory)
            if (territoryOwner != null && !territoryOwner.isIndebted()) {
                boolean isCitizen = (nation != null && nation.getId().equals(territoryOwner.getId()));
                if (!isCitizen) {
                    if (territoryOwner.getRegime() == PoliticalRegime.TOTALITARIAN_STATE) {
                        // Inwigilacja graniczna: obcy gracz świeci przez 10 sekund
                        if (!player.hasPotionEffect(PotionEffectType.GLOWING)) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false, true));
                            player.sendMessage(ChatColor.RED + "[INWIGILACJA] Przekroczyłeś granicę państwa totalitarnego "
                                    + territoryOwner.getName() + "! Twoja pozycja została podświetlona!");
                        }
                    } else if (territoryOwner.getRegime() == PoliticalRegime.POLICE_STATE) {
                        // Państwo Policyjne: zrywanie niewidzialności
                        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            player.removePotionEffect(PotionEffectType.INVISIBILITY);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false, true));
                            player.sendMessage(ChatColor.RED + "[PAŃSTWO POLICYJNE] Systemy bezpieczeństwa nacji "
                                    + territoryOwner.getName() + " zneutralizowały Twoją niewidzialność!");
                        }
                    }
                }
            }

            // 2. Buff'y i debuff'y własnego ustroju gracza
            if (nation == null || nation.isIndebted()) {
                continue; // Bonusy wyłączone dla bezpaństwowców lub przy zadłużeniu (Upkeep Debt)
            }

            PoliticalRegime regime = nation.getRegime();
            if (regime == null) continue;

            boolean onOwnTerritory = (territoryOwner != null && territoryOwner.getId().equals(nation.getId()));
            applyRegimeEffects(player, regime, onOwnTerritory);
        }
    }

    private void applyRegimeEffects(Player player, PoliticalRegime regime, boolean onOwnTerritory) {
        final int duration = 80; // 4 sekundy (płynne odświeżanie co 2s)

        switch (regime) {
            case HEREDITARY_MONARCHY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, true, false, true));
                } else if (player.getFoodLevel() <= 14) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, false, true));
                }
            }
            case MERCHANT_REPUBLIC -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false, true));
            }
            case MILITARISM -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, false, true));
                }
                if (player.isSprinting()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0, true, false, true));
                }
            }
            case TECHNOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration + 60, 0, true, false, true));
                }
            }
            case THEOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, true, false, true));
                }
                if (player.getHealth() < (player.getMaxHealth() / 2.0)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, false, true));
                }
            }
            case AUTOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, true, false, true));
                }
                if (player.getLocation().getBlockY() < 40) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 0, true, false, true));
                }
            }
            case AGRARIAN_COMMUNE -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 0, true, false, true));
                }
            }
            case CORSAIR_SYNDICATE -> {
                if (player.isInWater() || player.getLocation().getBlock().getType() == Material.WATER) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, duration, 0, true, false, true));
                }
                if (player.getLocation().getBlockY() < 50) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 0, true, false, true));
                }
            }
            case FEUDAL_REALM -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, false, true));
                }
                if (player.isInsideVehicle()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, true, false, true));
                } else if (hasHeavyArmor(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, false, true));
                }
            }
            case TRIBAL_FEDERATION -> {
                if (hasHeavyArmor(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, false, true));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 0, true, false, true));
                }
            }
            case PLUTOCRACY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, 1, true, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, true, false, true));
            }
            case MYSTIC_MAGOCRACY -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration + 60, 0, true, false, true));
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, true, false, true));
                }
            }
            case ANARCHO_SYNDICALISM -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 1, true, false, true));
                }
            }
            case SPARTAN_STRATOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, false, true));
                }
                if (player.isSprinting()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 0, true, false, true));
                }
            }
            case CONSTITUTIONAL_DEMOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, true, false, true));
                }
            }
            case CONSTITUTIONAL_REPUBLIC -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, false, true));
                }
            }
            case TOTALITARIAN_STATE -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false, true));
                }
            }
            case POLICE_STATE -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, true, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, false, true));
                }
            }
            case EXPANSIONIST_HEGEMONY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, true, false, true));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false, true));
                }
            }
            case MERITOCRACY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 0, true, false, true));
                }
            }
            case CLASSICAL_OLIGARCHY -> {
                if (onOwnTerritory) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 0, true, false, true));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, true, false, true));
                }
            }
        }
    }

    private boolean hasHeavyArmor(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null) return false;
        Material mat = chest.getType();
        return mat == Material.DIAMOND_CHESTPLATE;
    }
}
