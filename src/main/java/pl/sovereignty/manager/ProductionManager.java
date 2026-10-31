package pl.sovereignty.manager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PoliticalRegime;
import pl.sovereignty.model.TechnologyTier;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.util.RecipeRegistry;

import java.util.*;

public class ProductionManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final VaultManager vaultManager;
    private final PlayerWoodTracker playerWoodTracker;
    private final Random random = new Random();
    private final Map<UUID, Long> manualMineCooldowns = new HashMap<>();
    private final Map<UUID, Long> manualFarmCooldowns = new HashMap<>();

    public ProductionManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager, VaultManager vaultManager, PlayerWoodTracker playerWoodTracker) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.vaultManager = vaultManager;
        this.playerWoodTracker = playerWoodTracker;
    }

    public ProductionManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager, VaultManager vaultManager) {
        this(plugin, nationManager, territoryManager, vaultManager, null);
    }

    /**
     * Główny automatyczny cykl produkcyjny wyspy (co 10 sekund).
     */
    public void runProductionCycle() {
        for (TerritoryStructure structure : territoryManager.getAllStructures()) {
            if (!structure.isSupplyConnected()) {
                continue;
            }

            Nation nation = nationManager.getNation(structure.getNationId());
            if (nation == null) continue;

            // Wstrzymanie pasywnej produkcji przy zadłużeniu (Dzień 1+ kryzysu)
            if (nation.isIndebted() && nation.getDebtDays() >= 1) {
                continue;
            }

            // W ustroju Komuny Rolniczo-Robotniczej (o ile brak długu) obywatele nigdy nie cierpią głodu
            boolean immuneToStarvation = (nation.getRegime() == PoliticalRegime.AGRARIAN_COMMUNE && !nation.isIndebted());
            if (nation.isStarving() && !immuneToStarvation && random.nextDouble() < 0.40) {
                continue;
            }

            switch (structure.getType()) {
                case STATE_FARM -> processStateFarm(structure, nation);
                case SAWMILL_TOTEM -> processSawmillTotem(structure, nation);
                case MINING_SHAFT -> processMiningShaft(structure, nation);
                default -> {}
            }
        }
    }

    /**
     * Przekazuje wyprodukowane przedmioty:
     * 1. Jeśli istnieje Skarbiec Krajowy (National Vault) - przedmioty wędrują prosto do skarbca.
     * 2. Jeśli nie ma skarbca, ale obok struktury stoi skrzynia - lądują w skrzyni.
     * 3. W ostateczności przedmioty upuszczane są bezpiecznie na ziemię przy strukturze.
     */
    public void routeProductionYield(Nation nation, TerritoryStructure structure, ItemStack item) {
        if (item == null || item.getAmount() <= 0) return;

        // 1. Skarbiec Krajowy
        if (vaultManager.hasNationalVault(nation)) {
            vaultManager.addItemToVault(nation, item);
            return;
        }

        // 2. Skrzynia / Beczka / Lej / Shulker przylegający lub pod strukturą
        Location sLoc = structure.getLocation();
        BlockFace[] faces = new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP};
        for (BlockFace face : faces) {
            Block rel = sLoc.getBlock().getRelative(face);
            if (rel.getState() instanceof Container container) {
                HashMap<Integer, ItemStack> leftover = container.getInventory().addItem(item);
                if (leftover.isEmpty()) return;
                item = leftover.values().iterator().next();
            }
        }

        // 3. Upuszczenie na ziemię
        if (sLoc.getWorld() != null) {
            sLoc.getWorld().dropItemNaturally(sLoc.clone().add(0.5, 1.0, 0.5), item);
        }
    }

    /**
     * Ręczny zbiór plonów z Centrum Rolniczego (Farmy Państwowej) pod prawym przyciskiem myszy (PPM).
     * Zbiera dojrzałe uprawy i zasila nasionami oraz żywnością państwo i gracza.
     */
    public void handleManualFarm(Player player, TerritoryStructure structure, Nation nation) {
        if (nation.isIndebted() && nation.getDebtDays() >= 1) {
            player.sendMessage(ChatColor.RED + "[CENTRUM ROLNICZE] Z powodu zadłużenia państwa produkcja rolnicza została wstrzymana!");
            return;
        }

        long now = System.currentTimeMillis();
        TechnologyTier tech = nation.getTechTier();

        long cooldownMs = switch (tech) {
            case TIER_1_PRIMITIVE -> 120_000L;
            case TIER_2_STEEL_TOOLS -> 90_000L;
            case TIER_3_EARLY_MECHANIZATION -> 60_000L;
            case TIER_4_INDUSTRIAL_REVOLUTION -> 45_000L;
            case TIER_5_MASTER_CRAFTSMANSHIP -> 30_000L;
        };

        Long lastTime = manualFarmCooldowns.get(structure.getId());
        if (lastTime != null && (now - lastTime) < cooldownMs) {
            long secondsLeft = (cooldownMs - (now - lastTime)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "[CENTRUM ROLNICZE] Chłopi doglądają upraw! Następny zbiór za: "
                    + ChatColor.YELLOW + secondsLeft + "s.");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
            return;
        }

        manualFarmCooldowns.put(structure.getId(), now);

        Location loc = structure.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.ITEM_HOE_TILL, 0.9f, 1.0f);
            loc.getWorld().playSound(loc, Sound.BLOCK_CROP_BREAK, 1.0f, 0.9f);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.3, 0.3, 0);
        }

        List<ItemStack> drops = new ArrayList<>();
        // Znerfiony koszyk warzyw - 1 sztuka
        Material baseCrop = switch (random.nextInt(3)) {
            case 0 -> Material.WHEAT;
            case 1 -> Material.CARROT;
            default -> Material.POTATO;
        };
        drops.add(new ItemStack(baseCrop, 1));

        if (tech.getLevel() >= 3 && random.nextInt(100) < 30) {
            drops.add(new ItemStack(Material.BEETROOT, 1));
        }
        if (tech.getLevel() >= 4 && random.nextInt(100) < 15) {
            drops.add(new ItemStack(Material.BREAD, 1));
        }


        boolean sendToVault = player.isSneaking();
        StringBuilder summary = new StringBuilder();

        for (ItemStack drop : drops) {
            if (sendToVault) {
                routeProductionYield(nation, structure, drop);
            } else {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                for (ItemStack rem : leftover.values()) {
                    if (loc.getWorld() != null) {
                        loc.getWorld().dropItemNaturally(loc.clone().add(0, 1, 0), rem);
                    }
                }
            }
            if (summary.length() > 0) summary.append(", ");
            summary.append(drop.getAmount()).append("x ").append(getFriendlyCropName(drop.getType()));
        }

        double regimeBonus = (!nation.isIndebted() && nation.getRegime() == PoliticalRegime.AGRARIAN_COMMUNE)
                ? 1.20
                : (nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier());
        nation.addFood(1.5 * tech.getYieldMultiplier() * regimeBonus);

        if (sendToVault) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l[CENTRUM ROLNICZE] &fZ pól zebrano i przekazano do &6Skarbca Krajowego&f: &e" + summary.toString()
                            + "&a! &7(Zapasy żywności państwa wzrosły)"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l[CENTRUM ROLNICZE] &fZ pól zebrano plony: &e" + summary.toString()
                            + "&a! &8[Kucnij Shift+PPM, aby wysłać do Skarbca]"));
        }
    }



    /**
     * Ręczne wydobycie urobku z Głowicy Szybu Górniczego pod prawym przyciskiem myszy (PPM).
     * Skalowane przez poziom technologii nacji (Tier I: 60s cooldown, bruk/węgiel -> Tier V: 10s, szmaragdy).
     */
    public void handleManualMining(Player player, TerritoryStructure structure, Nation nation) {
        if (nation.isIndebted() && nation.getDebtDays() >= 1) {
            player.sendMessage(ChatColor.RED + "[GŁOWICA KOPALNI] Z powodu zadłużenia państwa wydobycie w kopalniach zostało wstrzymane!");
            return;
        }

        long now = System.currentTimeMillis();
        TechnologyTier tech = nation.getTechTier();

        long cooldownMs = switch (tech) {
            case TIER_1_PRIMITIVE -> 120_000L; // 2 minuty
            case TIER_2_STEEL_TOOLS -> 90_000L;
            case TIER_3_EARLY_MECHANIZATION -> 60_000L;
            case TIER_4_INDUSTRIAL_REVOLUTION -> 45_000L;
            case TIER_5_MASTER_CRAFTSMANSHIP -> 30_000L;
        };

        Long lastTime = manualMineCooldowns.get(structure.getId());
        if (lastTime != null && (now - lastTime) < cooldownMs) {
            long secondsLeft = (cooldownMs - (now - lastTime)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "[GŁOWICA KOPALNI] Górnicy głowicy urabiają skałę! Następny urobek za: "
                    + ChatColor.YELLOW + secondsLeft + "s &8(Poz. tech: " + tech.getDisplayName() + "&8)");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
            return;
        }

        manualMineCooldowns.put(structure.getId(), now);

        Location loc = structure.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 0.9f);
            loc.getWorld().playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
            loc.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.3, 0.3, Material.DEEPSLATE.createBlockData());
            loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 1.2, 0.5), 8, 0.2, 0.2, 0.2, 0.02);
        }

        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(random.nextBoolean() ? Material.COBBLESTONE : Material.COBBLED_DEEPSLATE, 1));

        int roll = random.nextInt(100);
        switch (tech) {
            case TIER_1_PRIMITIVE -> {
                // Wyłącznie bruk i węgiel
                if (roll < 40) {
                    drops.add(new ItemStack(Material.COAL, 1));
                }
            }
            case TIER_2_STEEL_TOOLS -> {
                // Bruk, węgiel, miedź
                if (roll < 50) {
                    drops.add(new ItemStack(Material.COAL, 1 + random.nextInt(3)));
                } else {
                    drops.add(new ItemStack(Material.RAW_COPPER, 1 + random.nextInt(2)));
                }
            }
            case TIER_3_EARLY_MECHANIZATION -> {
                // Bruk, węgiel, miedź, żelazo, lapis, redstone + 3% szansy na 1 Diament!
                if (roll < 38) {
                    drops.add(new ItemStack(Material.COAL, 1 + random.nextInt(3)));
                } else if (roll < 62) {
                    drops.add(new ItemStack(Material.RAW_COPPER, 1 + random.nextInt(3)));
                } else if (roll < 82) {
                    drops.add(new ItemStack(Material.RAW_IRON, 1 + random.nextInt(2)));
                } else if (roll < 97) {
                    drops.add(new ItemStack(random.nextBoolean() ? Material.REDSTONE : Material.LAPIS_LAZULI, 2 + random.nextInt(2)));
                } else {
                    drops.add(new ItemStack(Material.DIAMOND, 1));
                    if (loc.getWorld() != null) {
                        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.0, 0.5), 25, 0.3, 0.5, 0.3, 0.1);
                    }
                }
            }
            case TIER_4_INDUSTRIAL_REVOLUTION -> {
                // Bruk, węgiel, miedź, żelazo, lapis, redstone, złoto, szmaragdy i diamenty
                if (roll < 30) {
                    drops.add(new ItemStack(Material.COAL, 1 + random.nextInt(3)));
                } else if (roll < 52) {
                    drops.add(new ItemStack(Material.RAW_COPPER, 1 + random.nextInt(3)));
                } else if (roll < 72) {
                    drops.add(new ItemStack(Material.RAW_IRON, 1 + random.nextInt(3)));
                } else if (roll < 85) {
                    drops.add(new ItemStack(random.nextBoolean() ? Material.REDSTONE : Material.LAPIS_LAZULI, 2 + random.nextInt(3)));
                } else if (roll < 95) {
                    drops.add(new ItemStack(Material.RAW_GOLD, 1 + random.nextInt(2)));
                } else if (roll < 98) {
                    drops.add(new ItemStack(Material.EMERALD, 1));
                    if (loc.getWorld() != null) {
                        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                } else {
                    drops.add(new ItemStack(Material.DIAMOND, 1));
                    if (loc.getWorld() != null) {
                        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.0, 0.5), 35, 0.3, 0.5, 0.3, 0.1);
                    }
                }
            }
            case TIER_5_MASTER_CRAFTSMANSHIP -> {
                // Elitarny zbiór (złoto, szmaragdy, diamenty)
                if (roll < 22) {
                    drops.add(new ItemStack(Material.COAL, 2 + random.nextInt(3)));
                } else if (roll < 45) {
                    drops.add(new ItemStack(Material.RAW_COPPER, 2 + random.nextInt(3)));
                } else if (roll < 65) {
                    drops.add(new ItemStack(Material.RAW_IRON, 2 + random.nextInt(3)));
                } else if (roll < 78) {
                    drops.add(new ItemStack(random.nextBoolean() ? Material.REDSTONE : Material.LAPIS_LAZULI, 3 + random.nextInt(3)));
                } else if (roll < 92) {
                    drops.add(new ItemStack(Material.RAW_GOLD, 2));
                } else if (roll < 96) {
                    drops.add(new ItemStack(Material.EMERALD, 1));
                    if (loc.getWorld() != null) {
                        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                } else {
                    drops.add(new ItemStack(Material.DIAMOND, 1));
                    if (loc.getWorld() != null) {
                        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.0, 0.5), 45, 0.4, 0.6, 0.4, 0.1);
                    }
                }
            }
        }

        boolean sendToVault = player.isSneaking();
        StringBuilder summary = new StringBuilder();

        for (ItemStack drop : drops) {
            if (sendToVault) {
                routeProductionYield(nation, structure, drop);
            } else {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                for (ItemStack rem : leftover.values()) {
                    if (loc.getWorld() != null) {
                        loc.getWorld().dropItemNaturally(loc.clone().add(0, 1, 0), rem);
                    }
                }
            }
            if (summary.length() > 0) summary.append(", ");
            summary.append(drop.getAmount()).append("x ").append(getFriendlyMiningName(drop.getType()));
        }

        double regimeMultiplier = nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier();
        nation.addRawMaterials(1.0 * tech.getYieldMultiplier() * regimeMultiplier);

        double manualBankBonus = switch (tech) {
            case TIER_1_PRIMITIVE -> 1.5;
            case TIER_2_STEEL_TOOLS -> 3.5;
            case TIER_3_EARLY_MECHANIZATION -> 8.0;
            case TIER_4_INDUSTRIAL_REVOLUTION -> 16.0;
            case TIER_5_MASTER_CRAFTSMANSHIP -> 32.0;
        } * regimeMultiplier;
        nation.depositBank(manualBankBonus);

        if (sendToVault) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l[GŁOWICA KOPALNI] &fZ głębin szybu wydobyto i przekazano do &6Skarbca Krajowego&f: &e" + summary.toString()
                            + "&a! &7(+&6" + String.format(Locale.US, "%.2f", manualBankBonus) + "$&7 do Skarbca)"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l[GŁOWICA KOPALNI] &fZ głębin szybu wydobyto: &e" + summary.toString()
                            + "&a! &7(+&6" + String.format(Locale.US, "%.2f", manualBankBonus) + "$&7 do Skarbca) &8[Kucnij Shift+PPM, aby wysłać do Skarbca]"));
        }
    }

    private String getFriendlyWoodName(Material mat) {
        return switch (mat) {
            case OAK_LOG -> "Dębowe Drewno";
            case BIRCH_LOG -> "Brzozowe Drewno";
            case SPRUCE_LOG -> "Świerkowe Drewno";
            case JUNGLE_LOG -> "Drewno Dżunglowe";
            case ACACIA_LOG -> "Drewno Akacjowe";
            case DARK_OAK_LOG -> "Ciemny Dąb";
            case MANGROVE_LOG -> "Drewno Namorzynowe";
            case CHERRY_LOG -> "Drewno Wiśniowe";
            case BAMBOO_BLOCK -> "Blok Bambusa";
            case BAMBOO -> "Bambus";
            case OAK_SAPLING -> "Sadzonka Dębu";
            case BIRCH_SAPLING -> "Sadzonka Brzozy";
            case SPRUCE_SAPLING -> "Sadzonka Świerku";
            case APPLE -> "Jabłko";
            case STICK -> "Patyki";
            default -> mat.name();
        };
    }

    private String getFriendlyMiningName(Material mat) {
        return switch (mat) {
            case COBBLESTONE -> "Bruk";
            case COBBLED_DEEPSLATE -> "Łupkowy Bruk";
            case COAL -> "Węgiel";
            case RAW_COPPER -> "Surowa Miedź";
            case RAW_IRON -> "Surowe Żelazo";
            case REDSTONE -> "Czerwony Proszek";
            case LAPIS_LAZULI -> "Lazuryt (Lapis)";
            case RAW_GOLD -> "Surowe Złoto";
            case EMERALD -> "§a§lSzmaragd (Emerald)§e";
            case DIAMOND -> "§b§lDiament (Diamond)§e";
            default -> mat.name();
        };
    }

    private String getFriendlyCropName(Material mat) {
        return switch (mat) {
            case WHEAT -> "Pszenica";
            case CARROT -> "Marchew";
            case POTATO -> "Ziemniak";
            case BEETROOT -> "Burak";
            case SWEET_BERRIES -> "Słodkie Jagody";
            case SUGAR_CANE -> "Trzcina Cukrowa";
            case PUMPKIN -> "Dynia";
            case MELON_SLICE -> "Kawałek Arbuza";
            case MELON -> "Arbuz";
            case COCOA_BEANS -> "Ziarna Kakao";
            case NETHER_WART -> "Brodawka Netherowa";
            case BREAD -> "Bochenek Chleba";
            case PUMPKIN_PIE -> "Placek Dyniowy";
            case GOLDEN_CARROT -> "§6§lZłota Marchew§e";
            case BAKED_POTATO -> "Pieczony Ziemniak";
            default -> mat.name();
        };
    }

    /**
     * Automatyczna praca Farmy Państwowej: zbiera dojrzałe uprawy i obsiewa pole,
     * przesyłając plony do Skarbca Krajowego.
     * Wspiera: pszenicę, marchew, ziemniaki, buraki, trzcinę cukrową, słodkie jagody,
     * dynie, arbuzy, kakao, brodawkę netherową oraz bonusy żywnościowe.
     */
    private void processStateFarm(TerritoryStructure structure, Nation nation) {
        Location center = structure.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        int radius = 20;
        TechnologyTier tier = nation.getTechTier();

        int maxHarvests = switch (tier) {
            case TIER_1_PRIMITIVE, TIER_2_STEEL_TOOLS -> 1;
            case TIER_3_EARLY_MECHANIZATION -> 1;
            case TIER_4_INDUSTRIAL_REVOLUTION, TIER_5_MASTER_CRAFTSMANSHIP -> 2;
        };

        // 1. Autonomiczna produkcja żywności z samej budowli farmy (zbalansowana)
        double regimeBonus = 1.0;
        if (!nation.isIndebted()) {
            if (nation.getRegime() == PoliticalRegime.AGRARIAN_COMMUNE) {
                regimeBonus = 1.20; // +20% plonów w Komunie
            } else if (nation.getRegime() == PoliticalRegime.MILITARISM) {
                regimeBonus = 0.95; // -5% w Militaryzmie
            } else {
                regimeBonus = nation.getRegime().getProductionMultiplier();
            }
        }

        // Pasywny przyrost żywności dla nacji zasilający dobrobyt (mocno znerfiony do 0.04)
        double passiveFood = 0.04 * tier.getYieldMultiplier() * regimeBonus;
        nation.addFood(passiveFood);

        // Autonomiczne przekazywanie płodów rolnych do Skarbca Krajowego (nerf: zredukowane z 35% do 8%)
        if (random.nextInt(100) < 8) {
            Material passiveCrop = switch (random.nextInt(4)) {
                case 0 -> Material.CARROT;
                case 1 -> Material.POTATO;
                case 2 -> Material.BEETROOT;
                default -> Material.WHEAT;
            };
            routeProductionYield(nation, structure, new ItemStack(passiveCrop, 1));
        }

        world.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0.5, 1.2, 0.5), 4, 0.3, 0.3, 0.3, 0);

        int harvestedCount = 0;
        double totalFoodGained = 0.0;

        // 2. Dodatkowe spiralne poszukiwanie plonów w promieniu farmy (jeśli posadzono grządki)
        outer:
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -2; dy <= 3; dy++) {
                        Block block = center.clone().add(dx, dy, dz).getBlock();
                        Material mat = block.getType();

                        // 1. Obsługa Trzciny Cukrowej (Tier 2+)
                        if (mat == Material.SUGAR_CANE && tier.getLevel() >= 2) {
                            if (block.getRelative(BlockFace.UP).getType() == Material.SUGAR_CANE
                                    || block.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
                                Block base = block;
                                while (base.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
                                    base = base.getRelative(BlockFace.DOWN);
                                }
                                Block curr = base.getRelative(BlockFace.UP);
                                int harvestedCane = 0;
                                while (curr.getType() == Material.SUGAR_CANE && harvestedCane < (tier.getLevel() >= 4 ? 4 : 2)) {
                                    curr.setType(Material.AIR);
                                    harvestedCane++;
                                    curr = curr.getRelative(BlockFace.UP);
                                }
                                if (harvestedCane > 0) {
                                    routeProductionYield(nation, structure, new ItemStack(Material.SUGAR_CANE, harvestedCane));
                                    totalFoodGained += 0.5 * harvestedCane;
                                    world.spawnParticle(Particle.HAPPY_VILLAGER, base.getLocation().add(0.5, 1.0, 0.5), 5, 0.2, 0.3, 0.2, 0);
                                    world.playSound(base.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.7f, 1.2f);
                                    harvestedCount++;
                                    if (harvestedCount >= maxHarvests) break outer;
                                }
                            }
                            continue;
                        }

                        // 2. Obsługa Dyń i Arbuzów (Tier 3+)
                        if ((mat == Material.PUMPKIN || mat == Material.MELON) && tier.getLevel() >= 3) {
                            block.setType(Material.AIR);
                            ItemStack yield = (mat == Material.PUMPKIN)
                                    ? new ItemStack(Material.PUMPKIN, 1)
                                    : new ItemStack(Material.MELON_SLICE, 3 + random.nextInt(4));
                            routeProductionYield(nation, structure, yield);
                            totalFoodGained += (mat == Material.PUMPKIN ? 2.0 : 1.5);
                            world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, mat.createBlockData());
                            world.playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.8f, 1.2f);
                            harvestedCount++;
                            if (harvestedCount >= maxHarvests) break outer;
                            continue;
                        }

                        // 3. Rośliny ze statusem wieku (Ageable)
                        BlockData data = block.getBlockData();
                        if (data instanceof Ageable ageable) {
                            // Słodkie jagody (Tier 2+): zbierane przy wieku >= 2, resetowane do wieku 1
                            if (mat == Material.SWEET_BERRY_BUSH) {
                                if (tier.getLevel() >= 2 && ageable.getAge() >= 2) {
                                    int berryCount = ageable.getAge() == 3 ? (2 + random.nextInt(2)) : 1;
                                    ageable.setAge(1);
                                    block.setBlockData(ageable);
                                    routeProductionYield(nation, structure, new ItemStack(Material.SWEET_BERRIES, berryCount));
                                    totalFoodGained += 1.0 * berryCount;
                                    world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 4, 0.2, 0.2, 0.2, 0);
                                    world.playSound(block.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.7f, 1.2f);
                                    harvestedCount++;
                                    if (harvestedCount >= maxHarvests) break outer;
                                }
                                continue;
                            }

                            // Pozostałe uprawy - sprawdzamy pełną dojrzałość
                            if (ageable.getAge() >= ageable.getMaximumAge()) {
                                // Sprawdzenie dopuszczalności poziomu technologicznego dla roślin egzotycznych
                                if (mat == Material.COCOA || mat == Material.NETHER_WART) {
                                    if (tier.getLevel() < 3) continue;
                                }

                                // Reset do stanu posadzonego nasiona
                                ageable.setAge(0);
                                block.setBlockData(ageable);

                                double yieldBase = 0.5;

                                ItemStack cropYield = switch (mat) {
                                    case CARROTS -> new ItemStack(Material.CARROT, 1);
                                    case POTATOES -> new ItemStack(Material.POTATO, 1);
                                    case BEETROOTS -> new ItemStack(Material.BEETROOT, 1);
                                    case COCOA -> new ItemStack(Material.COCOA_BEANS, 1);
                                    case NETHER_WART -> new ItemStack(Material.NETHER_WART, 1);
                                    default -> new ItemStack(Material.WHEAT, 1);
                                };

                                routeProductionYield(nation, structure, cropYield);

                                // Nasiona dla pszenicy i buraków (20% szansy)
                                if (mat == Material.WHEAT && random.nextInt(100) < 20) {
                                    routeProductionYield(nation, structure, new ItemStack(Material.WHEAT_SEEDS, 1));
                                } else if (mat == Material.BEETROOTS && random.nextInt(100) < 20) {
                                    routeProductionYield(nation, structure, new ItemStack(Material.BEETROOT_SEEDS, 1));
                                }

                                // Drobny bonus w Tierze 5 (10% na 1 chleb)
                                if (tier.getLevel() >= 5 && random.nextInt(100) < 10) {
                                    routeProductionYield(nation, structure, new ItemStack(Material.BREAD, 1));
                                }

                                totalFoodGained += yieldBase;
                                world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 4, 0.2, 0.2, 0.2, 0);
                                world.playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 0.5f, 1.0f);
                                harvestedCount++;
                                if (harvestedCount >= maxHarvests) break outer;
                            }
                        }
                    }
                }
            }
        }

        if (totalFoodGained > 0) {
            double finalFood = totalFoodGained * tier.getYieldMultiplier() * regimeBonus;
            nation.addFood(Math.max(1.0, finalFood));
        }
    }

    /**
     * Zwraca zasięg działania tartaku w zależności od poziomu rozwoju technologicznego nacji.
     * Tier 1: 15 bloków -> Tier 5: 35 bloków.
     */
    public int getSawmillRadius(Nation nation) {
        if (nation == null) return 15;
        return switch (nation.getTechTier()) {
            case TIER_1_PRIMITIVE -> 15;
            case TIER_2_STEEL_TOOLS -> 20;
            case TIER_3_EARLY_MECHANIZATION -> 25;
            case TIER_4_INDUSTRIAL_REVOLUTION -> 30;
            case TIER_5_MASTER_CRAFTSMANSHIP -> 35;
        };
    }

    /**
     * Automatyczna praca Totemu Tartaku:
     * 1. Ścięcie 1-3 bloków pnia drzewa w zasięgu rosnącym wraz z ulepszeniami (15 -> 35 bloków).
     * 2. W Tier 2+ automatyczne posadzenie sadzonki w podłożu (odnawialny las). W Tier 1 brak autozalesiania.
     * 3. W Tier 4+ nawożenie mączką pobliskich sadzonek w razie braku kłód.
     * 4. Przesłanie drewna i sadzonek bezpośrednio do Skarbca Krajowego.
     */
    private void processSawmillTotem(TerritoryStructure structure, Nation nation) {
        Location center = structure.getLocation();
        int radius = getSawmillRadius(nation);
        World world = center.getWorld();
        if (world == null) return;
        TechnologyTier tech = nation.getTechTier();

        // Ciężki nerf tartaku - drwale pracują bardzo powoli (zaledwie 20% szansy na ścięcie w takcie)
        if (random.nextInt(100) < 80) return;

        // 7% szansy na wypadek przy pracy drwali (zatarcie pił / zapłon trocin) -> spalony węgiel drzewny do Skarbca Krajowego
        if (random.nextInt(100) < 7) {
            ItemStack burntCoal = new ItemStack(Material.CHARCOAL, 1 + random.nextInt(2));
            org.bukkit.inventory.meta.ItemMeta meta = burntCoal.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§8§lSpalony Węgiel §7(Wypadek w Tartaku)");
                meta.setLore(List.of(
                        "§7Powstał w wyniku zatarcia pił i wypadku przy obróbce drewna.",
                        "§8Przekazano bezpośrednio do Skarbca Krajowego."
                ));
                burntCoal.setItemMeta(meta);
            }
            routeProductionYield(nation, structure, burntCoal);

            Location accidentLoc = center.clone().add(0.5, 1.0, 0.5);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, accidentLoc, 15, 0.3, 0.5, 0.3, 0.05);
            world.spawnParticle(Particle.FLAME, accidentLoc, 5, 0.2, 0.3, 0.2, 0.02);
            world.playSound(accidentLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.9f);
        }

        // 1. Poszukiwanie pnia drzewa lub pędów bambusa od środka na zewnątrz
        Block targetWood = null;
        for (int r = 1; r <= radius && targetWood == null; r++) {
            for (int dx = -r; dx <= r && targetWood == null; dx++) {
                for (int dz = -r; dz <= r && targetWood == null; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    for (int dy = -4; dy <= 14; dy++) {
                        Block block = center.clone().add(dx, dy, dz).getBlock();
                        Material mat = block.getType();
                        if ((mat.name().endsWith("_LOG") || mat.name().endsWith("_WOOD") || Tag.LOGS.isTagged(mat))
                                && !mat.name().contains("STRIPPED_") && mat != Material.BAMBOO_BLOCK) {
                            if (playerWoodTracker == null || playerWoodTracker.isNaturalTree(block)) {
                                targetWood = block;
                                break;
                            }
                        } else if (mat == Material.BAMBOO) {
                            if (playerWoodTracker != null && playerWoodTracker.isPlayerPlaced(block)) {
                                continue;
                            }
                            // Bambus kwalifikuje się do wycinki, gdy ma min. 2 bloki wysokości (aby zachować dolny odrastający pęd)
                            if (block.getRelative(BlockFace.UP).getType() == Material.BAMBOO
                                    || block.getRelative(BlockFace.DOWN).getType() == Material.BAMBOO) {
                                targetWood = block;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (targetWood != null) {
            if (targetWood.getType() == Material.BAMBOO) {
                // Znajdź najniższy blok łodygi bambusa (bazę na ziemi)
                Block base = targetWood;
                while (base.getRelative(BlockFace.DOWN).getType() == Material.BAMBOO) {
                    base = base.getRelative(BlockFace.DOWN);
                }

                // Ścinamy łodygi powyżej bazy (pozostawiając 1 blok przy gruncie, dzięki czemu bambus samoczynnie odrasta!)
                Block curr = base.getRelative(BlockFace.UP);
                int maxHarvestBamboo = 1;

                int harvestedCount = 0;
                while (curr.getType() == Material.BAMBOO && harvestedCount < maxHarvestBamboo) {
                    curr.setType(Material.AIR);
                    harvestedCount++;
                    curr = curr.getRelative(BlockFace.UP);
                }

                if (harvestedCount > 0) {
                    ItemStack bambooItem = new ItemStack(Material.BAMBOO, harvestedCount);
                    routeProductionYield(nation, structure, bambooItem);

                    double sawmillRegimeBonus = (!nation.isIndebted() && nation.getRegime() == PoliticalRegime.AGRARIAN_COMMUNE)
                            ? 1.10
                            : (nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier());
                    nation.addWood(0.04 * harvestedCount * sawmillRegimeBonus * tech.getYieldMultiplier());

                    Location pLoc = base.getLocation().add(0.5, 1.0, 0.5);
                    world.spawnParticle(Particle.BLOCK, pLoc, 15, 0.3, 0.5, 0.3, Material.BAMBOO.createBlockData());
                    world.playSound(pLoc, Sound.BLOCK_BAMBOO_BREAK, 0.9f, 1.0f);
                    return;
                }
            }

            Material originalLog = targetWood.getType();
            Material sapling = getMatchingSapling(originalLog);

            // Zbalansowana wycinka drewna (1 blok na niższych tierach, rzadko 2 na wyższych)
            Block curr = targetWood;
            int maxHarvest = switch (tech) {
                case TIER_1_PRIMITIVE, TIER_2_STEEL_TOOLS -> 1;
                case TIER_3_EARLY_MECHANIZATION, TIER_4_INDUSTRIAL_REVOLUTION, TIER_5_MASTER_CRAFTSMANSHIP -> (random.nextInt(100) < 30 ? 2 : 1);
            };
            int harvestedCount = 0;
            Block bottomDirt = null;

            while (curr != null && (curr.getType().name().endsWith("_LOG") || curr.getType().name().endsWith("_WOOD") || Tag.LOGS.isTagged(curr.getType())) && harvestedCount < maxHarvest) {
                if (playerWoodTracker != null && playerWoodTracker.isPlayerPlaced(curr)) {
                    break;
                }
                Block below = curr.getRelative(BlockFace.DOWN);
                if (isPlantableSoil(below.getType())) {
                    bottomDirt = below;
                }
                curr.setType(Material.AIR);
                harvestedCount++;
                curr = curr.getRelative(BlockFace.UP);
            }

            // Automatyczne sadzenie sadzonki odblokowane od Tier II (Narzędzia Stalowe)
            if (bottomDirt != null && tech.getLevel() >= 2) {
                bottomDirt.getRelative(BlockFace.UP).setType(sapling);
            }

            int logYield = Math.max(1, harvestedCount);
            ItemStack logItem = new ItemStack(originalLog, logYield);
            routeProductionYield(nation, structure, logItem);

            if (random.nextInt(100) < 10) {
                routeProductionYield(nation, structure, new ItemStack(sapling, 1));
            }
            if (tech.getLevel() >= 3 && random.nextInt(100) < 3) {
                routeProductionYield(nation, structure, new ItemStack(Material.APPLE, 1));
            }

            double sawmillRegimeBonus = (!nation.isIndebted() && nation.getRegime() == PoliticalRegime.AGRARIAN_COMMUNE)
                    ? 1.10
                    : (nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier());
            nation.addWood(0.20 * logYield * sawmillRegimeBonus * tech.getYieldMultiplier());

            Location pLoc = targetWood.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.BLOCK, pLoc, 12, 0.3, 0.3, 0.3, originalLog.createBlockData());
            world.playSound(pLoc, Sound.BLOCK_WOOD_BREAK, 0.8f, 0.9f);
            return;
        }

        // 2. Jeśli brak dojrzałych pni lub bambusa, nawożenie i przyspieszanie sadzonek i pędów w promieniu totemu (Tier 4+, 40% szansy)
        if (tech.getLevel() >= 4 && random.nextInt(100) < 40) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    for (int dy = -3; dy <= 5; dy++) {
                        Block block = center.clone().add(dx, dy, dz).getBlock();
                        if (RecipeRegistry.ALL_SAPLINGS.contains(block.getType())
                                || block.getType() == Material.BAMBOO
                                || block.getType() == Material.BAMBOO_SAPLING) {
                            block.applyBoneMeal(BlockFace.UP);
                            world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0);
                            world.playSound(block.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.6f, 1.2f);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isPlantableSoil(Material mat) {
        return mat == Material.DIRT || mat == Material.GRASS_BLOCK || mat == Material.COARSE_DIRT
                || mat == Material.PODZOL || mat == Material.ROOTED_DIRT || mat == Material.MUD
                || mat == Material.SAND || mat == Material.RED_SAND || mat == Material.GRAVEL
                || mat == Material.MOSS_BLOCK || mat == Material.MYCELIUM;
    }

    /**
     * Automatyczna praca Głowicy Szybu Górniczego: generuje urobek zależny od poziomu technologii
     * i przesyła go do Skarbca Krajowego.
     */
    private void processMiningShaft(TerritoryStructure structure, Nation nation) {
        // Ciężki nerf kopalni: 75% szansy na brak urobku w cyklu (wydobycie drastycznie zredukowane)
        if (random.nextInt(100) < 75) return;

        Location loc = structure.getLocation();

        ItemStack minedYield = generateOreYield(nation);
        routeProductionYield(nation, structure, minedYield);

        double regimeMultiplier = nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier();
        nation.addRawMaterials(0.05 * nation.getTechTier().getYieldMultiplier() * regimeMultiplier);

        // Znerfiony dochód finansowy z urobku kopalni zasilający Skarbiec Narodowy (Vault Bank)
        double bankIncome = switch (nation.getTechTier()) {
            case TIER_1_PRIMITIVE -> 0.1;
            case TIER_2_STEEL_TOOLS -> 0.25;
            case TIER_3_EARLY_MECHANIZATION -> 0.5;
            case TIER_4_INDUSTRIAL_REVOLUTION -> 1.0;
            case TIER_5_MASTER_CRAFTSMANSHIP -> 2.0;
        } * regimeMultiplier;
        nation.depositBank(bankIncome);

        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0.5, 1.0, 0.5), 3, 0.2, 0.2, 0.2, 0);
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_STEP, 0.6f, 0.8f);
        }
    }

    /**
     * Automatyczny urobek kopalni dopasowany do poziomu rozwoju technologicznego.
     * Dodano szansę na wydobycie Diamentów (Tier 3-5)!
     */
    private ItemStack generateOreYield(Nation nation) {
        TechnologyTier tier = nation.getTechTier();
        int roll = random.nextInt(100);

        switch (tier) {
            case TIER_1_PRIMITIVE -> {
                // Wyłącznie bruk i węgiel
                if (roll < 60) {
                    return new ItemStack(Material.COBBLESTONE, 1 + random.nextInt(2));
                } else {
                    return new ItemStack(Material.COAL, 1);
                }
            }
            case TIER_2_STEEL_TOOLS -> {
                // Bruk, węgiel, miedź
                if (roll < 45) {
                    return new ItemStack(Material.COBBLESTONE, 2 + random.nextInt(2));
                } else if (roll < 75) {
                    return new ItemStack(Material.COAL, 1 + random.nextInt(2));
                } else {
                    return new ItemStack(Material.RAW_COPPER, 1 + random.nextInt(2));
                }
            }
            case TIER_3_EARLY_MECHANIZATION -> {
                // Bruk, węgiel, miedź, żelazo, lapis, redstone
                if (roll < 35) {
                    return new ItemStack(Material.COBBLESTONE, 2 + random.nextInt(2));
                } else if (roll < 58) {
                    return new ItemStack(Material.COAL, 1 + random.nextInt(3));
                } else if (roll < 74) {
                    return new ItemStack(Material.RAW_COPPER, 1 + random.nextInt(2));
                } else if (roll < 88) {
                    return new ItemStack(Material.RAW_IRON, 1 + random.nextInt(2));
                } else {
                    return new ItemStack(random.nextBoolean() ? Material.REDSTONE : Material.LAPIS_LAZULI, 2 + random.nextInt(2));
                }
            }
            case TIER_4_INDUSTRIAL_REVOLUTION -> {
                // Bruk, węgiel, miedź, żelazo, redstone, złoto, szmaragdy (1%) i diamenty (2%)
                if (roll < 30) {
                    return new ItemStack(Material.COBBLESTONE, 2 + random.nextInt(3));
                } else if (roll < 52) {
                    return new ItemStack(Material.COAL, 2 + random.nextInt(2));
                } else if (roll < 67) {
                    return new ItemStack(Material.RAW_COPPER, 2 + random.nextInt(2));
                } else if (roll < 80) {
                    return new ItemStack(Material.RAW_IRON, 2 + random.nextInt(2));
                } else if (roll < 90) {
                    return new ItemStack(Material.REDSTONE, 2 + random.nextInt(3));
                } else if (roll < 97) {
                    return new ItemStack(Material.RAW_GOLD, 1);
                } else if (roll < 98) {
                    return new ItemStack(Material.EMERALD, 1);
                } else {
                    return new ItemStack(Material.DIAMOND, 1);
                }
            }
            case TIER_5_MASTER_CRAFTSMANSHIP -> {
                // Elitarne wydobycie: złoto, szmaragdy (3%) i diamenty (3%)
                if (roll < 22) {
                    return new ItemStack(Material.COBBLED_DEEPSLATE, 3 + random.nextInt(3));
                } else if (roll < 42) {
                    return new ItemStack(Material.COAL, 3 + random.nextInt(3));
                } else if (roll < 57) {
                    return new ItemStack(Material.RAW_COPPER, 3 + random.nextInt(2));
                } else if (roll < 74) {
                    return new ItemStack(Material.RAW_IRON, 3 + random.nextInt(2));
                } else if (roll < 85) {
                    return new ItemStack(Material.LAPIS_LAZULI, 3 + random.nextInt(3));
                } else if (roll < 94) {
                    return new ItemStack(Material.RAW_GOLD, 1 + random.nextInt(2));
                } else if (roll < 97) {
                    return new ItemStack(Material.EMERALD, 1);
                } else {
                    return new ItemStack(Material.DIAMOND, 1);
                }
            }
        }
        return new ItemStack(Material.COBBLESTONE, 1);
    }

    private Material getMatchingSapling(Material log) {
        if (log.name().contains("BIRCH")) return Material.BIRCH_SAPLING;
        if (log.name().contains("SPRUCE")) return Material.SPRUCE_SAPLING;
        if (log.name().contains("JUNGLE")) return Material.JUNGLE_SAPLING;
        if (log.name().contains("ACACIA")) return Material.ACACIA_SAPLING;
        if (log.name().contains("DARK_OAK")) return Material.DARK_OAK_SAPLING;
        if (log.name().contains("CHERRY")) return Material.CHERRY_SAPLING;
        if (log.name().contains("MANGROVE")) return Material.MANGROVE_PROPAGULE;
        if (log.name().contains("BAMBOO")) return Material.BAMBOO;
        return Material.OAK_SAPLING;
    }
}
