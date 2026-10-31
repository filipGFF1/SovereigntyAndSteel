package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import pl.sovereignty.gui.BarracksGui;
import pl.sovereignty.gui.WarCouncilGui;
import pl.sovereignty.manager.*;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.SiegeMachineType;
import pl.sovereignty.model.StructureType;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.Random;

public class TerritoryProtectionListener implements Listener {
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final SiegeMachineManager machineManager;
    private final MilitaryManager militaryManager;
    private final PaoloManager paoloManager;
    private final WarManager warManager;
    private final WarDamageManager warDamage;
    private final ProductionManager productionManager;
    private final VaultManager vaultManager;
    private final VaultEconomyManager vaultEconomyManager;
    private final WallManager wallManager;
    private final CustomItemRegistry items;
    private final Random random = new Random();
    private MapManager mapManager;
    private DiplomacyManager diplomacyManager;

    public TerritoryProtectionListener(NationManager nationManager, TerritoryManager territoryManager,
                                       SiegeMachineManager machineManager, MilitaryManager militaryManager,
                                       PaoloManager paoloManager, WarManager warManager,
                                       WarDamageManager warDamage, ProductionManager productionManager,
                                       VaultManager vaultManager, VaultEconomyManager vaultEconomyManager,
                                       WallManager wallManager, CustomItemRegistry items) {
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.machineManager = machineManager;
        this.militaryManager = militaryManager;
        this.paoloManager = paoloManager;
        this.warManager = warManager;
        this.warDamage = warDamage;
        this.productionManager = productionManager;
        this.vaultManager = vaultManager;
        this.vaultEconomyManager = vaultEconomyManager;
        this.wallManager = wallManager;
        this.items = items;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void setDiplomacyManager(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = event.getItemInHand();
        Location loc = event.getBlockPlaced().getLocation();

        // Sprawdź czy to blok struktury customowej
        if (hand.hasItemMeta()) {
            String structTypeStr = hand.getItemMeta().getPersistentDataContainer().get(items.KEY_STRUCTURE_TYPE, PersistentDataType.STRING);
            if (structTypeStr != null) {
                StructureType type = StructureType.valueOf(structTypeStr);
                Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());

                if (playerNation == null) {
                    player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby stawiać struktury nacji!");
                    event.setCancelled(true);
                    return;
                }

                if (type == StructureType.BASTION_CORE) {
                    TerritoryStructure existing = territoryManager.getNationBastion(playerNation.getId());
                    if (existing != null) {
                        player.sendMessage(ChatColor.RED + "Twoje państwo posiada już postawiony Rdzeń Bastionu!");
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    // Pozostałe struktury muszą być stawiane na własnym terytorium
                    Nation territoryOwner = territoryManager.getNationAt(loc);
                    if (territoryOwner == null || !territoryOwner.getId().equals(playerNation.getId())) {
                        player.sendMessage(ChatColor.RED + "Tę strukturę możesz postawić wyłącznie w strefie wpływów swojego państwa!");
                        event.setCancelled(true);
                        return;
                    }

                    // Limit do 10 sztuk dla tartaków, kopalni i farm
                    if (type == StructureType.SAWMILL_TOTEM || type == StructureType.MINING_SHAFT || type == StructureType.STATE_FARM) {
                        int currentCount = territoryManager.getStructureCount(playerNation.getId(), type);
                        if (currentCount >= TerritoryManager.MAX_PRODUCTION_STRUCTURES) {
                            player.sendMessage(ChatColor.RED + "Twoje państwo osiągnęło limit "
                                    + TerritoryManager.MAX_PRODUCTION_STRUCTURES + " sztuk budowli typu: " + type.getDisplayName() + "!");
                            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 0.8f);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }

                // Rejestracja struktury
                territoryManager.registerStructure(playerNation, type, loc);
                int countAfter = territoryManager.getStructureCount(playerNation.getId(), type);
                String countInfo = (type == StructureType.SAWMILL_TOTEM || type == StructureType.MINING_SHAFT || type == StructureType.STATE_FARM)
                        ? " " + ChatColor.GRAY + "(" + countAfter + "/" + TerritoryManager.MAX_PRODUCTION_STRUCTURES + ")"
                        : "";
                player.sendMessage(ChatColor.GREEN + "Pomyślnie wzniesiono strukturę: " + ChatColor.GOLD + type.getDisplayName() + ChatColor.GREEN + "!" + countInfo);
                return;
            }
        }

        // Zwykłe bloki - ochrona terenu
        Nation territoryOwner = territoryManager.getNationAt(loc);
        if (territoryOwner != null) {
            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            if (playerNation == null || !playerNation.getId().equals(territoryOwner.getId())) {
                boolean atWar = playerNation != null && warManager.isAtWar(playerNation.getId(), territoryOwner.getId());
                if (atWar) {
                    // Podczas wojny na terenie wroga można stawiać JEDYNIE TNT!
                    if (event.getBlockPlaced().getType() == Material.TNT) {
                        return; // stawianie TNT dozwolone
                    }
                    player.sendMessage(ChatColor.RED + "Podczas wojny na terenie wroga możesz stawiać jedynie TNT!");
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(ChatColor.RED + "Ten obszar wyspy należy do państwa " + territoryOwner.getName() + " [" + territoryOwner.getTag() + "]!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        TerritoryStructure structure = territoryManager.getStructureAt(loc);
        if (structure == null) {
            Nation owner = territoryManager.getNationAt(loc);
            if (owner != null) {
                TerritoryStructure bastion = territoryManager.getNationBastion(owner.getId());
                if (bastion != null && bastion.getLocation().equals(loc.getBlock().getLocation())) {
                    structure = bastion;
                }
            }
        }

        if (structure != null) {
            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            if (playerNation != null && playerNation.getId().equals(structure.getNationId())) {
                territoryManager.removeStructure(structure);
                event.setDropItems(false);
                ItemStack customItem = getStructureCustomItem(structure.getType());
                if (customItem != null && loc.getWorld() != null) {
                    loc.getWorld().dropItemNaturally(loc, customItem);
                }
                player.sendMessage(ChatColor.YELLOW + "Zdemontowano strukturę państwową: " + structure.getType().getDisplayName());
                return;
            } else if (structure.getType() == StructureType.BASTION_CORE) {
                // To jest serce obcego państwa!
                Nation victimNation = nationManager.getNation(structure.getNationId());
                if (victimNation != null) {
                    boolean atWar = playerNation != null && warManager.isAtWar(playerNation.getId(), victimNation.getId());
                    if (!atWar) {
                        player.sendMessage(ChatColor.RED + "Możesz zniszczyć Serce Państwa " + victimNation.getName() + " tylko podczas wojny!");
                        event.setCancelled(true);
                        return;
                    }

                    // Okna Wojenne (Siege Window) - ochrona przed niszczeniem rdzenia o 4:00 rano
                    if (!victimNation.isWithinSiegeWindow()) {
                        player.sendMessage(ChatColor.RED + "[OKNO WOJENNE] Serce Państwa jest nietykalne! Oblężenie rdzenia jest aktywne tylko w godzinach "
                                + victimNation.getSiegeWindowStartHour() + ":00 - " + victimNation.getSiegeWindowEndHour() + ":00.");
                        event.setCancelled(true);
                        return;
                    }

                    // Gracz niszczy serce państwa podczas wojny w oknie wojennym!
                    territoryManager.removeStructure(structure);
                    event.setDropItems(false);

                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, loc.clone().add(0.5, 0.5, 0.5), 3);
                        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.8f);
                        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
                    }

                    String victimName = victimNation.getName();
                    String conquerorName = playerNation.getName();

                    org.bukkit.Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "\n&4&l==============================================\n"
                            + "&4&l[UPADEK PAŃSTWA] &cSerce państwa &f" + victimName + " &czostało zniszczone przez &f" + player.getName() + " &c(&f" + conquerorName + "&c)!\n"
                            + "&cPaństwo &f" + victimName + " &cupadło i zostało całkowicie zniszczone!\n"
                            + "&4&l==============================================\n"));

                    for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.6f);
                    }

                    // Zakończ wojnę - wygrywa państwo najeźdźcy
                    pl.sovereignty.model.WarState war = warManager.getActiveWar(playerNation.getId(), victimNation.getId());
                    if (war != null) {
                        warManager.endWar(war, playerNation.getId());
                    }
                    warManager.concludeWarsForNation(victimNation.getId());

                    // Usuń struktury i rozwiąż niszczone państwo
                    territoryManager.removeAllStructuresForNation(victimNation.getId());
                    if (mapManager != null) {
                        mapManager.removeNation(victimNation);
                    }
                    nationManager.disbandNation(victimNation);
                    return;
                }
            } else {
                Nation owner = nationManager.getNation(structure.getNationId());
                boolean atWar = playerNation != null && owner != null && warManager.isAtWar(playerNation.getId(), owner.getId());
                if (atWar) {
                    player.sendMessage(ChatColor.RED + "Na terenie wroga możesz niszczyć jedynie mury obronne oraz Serce Państwa (Rdzeń Bastionu)!");
                } else {
                    player.sendMessage(ChatColor.RED + "Ta struktura należy do innego państwa!");
                }
                event.setCancelled(true);
                return;
            }
        }

        Nation territoryOwner = territoryManager.getNationAt(loc);
        if (territoryOwner != null) {
            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            if (playerNation == null || !playerNation.getId().equals(territoryOwner.getId())) {
                boolean atWar = playerNation != null && warManager.isAtWar(playerNation.getId(), territoryOwner.getId());
                if (atWar) {
                    // Podczas wojny na terenie wroga można niszczyć JEDYNIE mury obronne!
                    if (wallManager.isWallBlock(loc) || wallManager.isWallBlockMaterial(block.getType())) {
                        warDamage.damageBlockInWar(block);
                        wallManager.handleWallBlockDestroyed(loc);
                        return;
                    }
                    player.sendMessage(ChatColor.RED + "Na terenie wroga możesz niszczyć jedynie mury obronne oraz Serce Państwa!");
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(ChatColor.RED + "Terytorium chronione przez państwo " + territoryOwner.getName() + "!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 1. Skrzynie Montażowe Machin (stawianie jak łódka)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item.hasItemMeta()) {
            String machineTypeStr = item.getItemMeta().getPersistentDataContainer().get(items.KEY_MACHINE_TYPE, PersistentDataType.STRING);
            if (machineTypeStr != null) {
                event.setCancelled(true);
                Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
                if (nation == null) {
                    player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby rozstawiać machiny wojenne!");
                    return;
                }

                SiegeMachineType type = SiegeMachineType.valueOf(machineTypeStr);
                Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
                machineManager.deployMachine(player, nation, type, spawnLoc);

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                return;
            }
        }

        // 2. Kliknięcie w struktury (Koszary / Stół Narad / Kopalnia / Tartak / Skarbiec)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            TerritoryStructure structure = territoryManager.getStructureAt(event.getClickedBlock().getLocation());
            if (structure != null) {
                Nation nation = nationManager.getNationByPlayer(player.getUniqueId());

                if (structure.getType() == StructureType.NATIONAL_VAULT) {
                    event.setCancelled(true);
                    if (nation != null && nation.getId().equals(structure.getNationId())) {
                        if (player.isSneaking()) {
                            pl.sovereignty.gui.VaultUpgradeGui.open(player, nation, vaultEconomyManager);
                        } else {
                            if (nation.getVaultTier() == pl.sovereignty.model.VaultTier.TIER_1_WOODEN) {
                                vaultManager.openVault(player, nation, 0);
                            } else {
                                pl.sovereignty.gui.VaultSelectorGui.open(player, nation, vaultManager);
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Nie masz dostępu do Skarbca Krajowego innego państwa!");
                    }
                    return;
                }

                if (nation != null && nation.getId().equals(structure.getNationId())) {
                    if (structure.getType() == StructureType.BARRACKS) {
                        event.setCancelled(true);
                        BarracksGui.open(player, nation, militaryManager);
                        return;
                    }
                    if (structure.getType() == StructureType.WAR_COUNCIL) {
                        event.setCancelled(true);
                        WarCouncilGui.open(player, nation, paoloManager);
                        return;
                    }
                    if (structure.getType() == StructureType.MINING_SHAFT) {
                        event.setCancelled(true);
                        productionManager.handleManualMining(player, structure, nation);
                        return;
                    }
                    if (structure.getType() == StructureType.SAWMILL_TOTEM) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&6&l[TOTEM TARTAKU] &7Tartak pracuje w pełni autonomicznie! Ścina pnie oraz bambus i przesyła urobek prosto do Skarbca Krajowego."));
                        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 0.8f, 1.2f);
                        return;
                    }
                    if (structure.getType() == StructureType.STATE_FARM) {
                        event.setCancelled(true);
                        productionManager.handleManualFarm(player, structure, nation);
                        return;
                    }
                }
            }
        }

        // 3. Otwieranie Skrzyni Zagranicznego Łupu z rajdu pirackiego
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && item.hasItemMeta()) {
            String itemId = items.getCustomItemId(item);
            if ("foreign_loot_chest".equals(itemId)) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                Location loc = player.getLocation();
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 2 + random.nextInt(4)));
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.EMERALD, 5 + random.nextInt(8)));
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, 8 + random.nextInt(12)));
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RAW_COPPER, 16));
                player.sendMessage(ChatColor.GOLD + "[ŁUP PIRACKI] Otworzono Skrzynię Zagranicznego Łupu!");
                return;
            }
        }

        // 4. Ochrona pojemników (Skrzynie, Beczki, Shulkery, Leje itp.) i mechanizmów przed intruzami
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block clickedBlock = event.getClickedBlock();
            Nation territoryOwner = territoryManager.getNationAt(clickedBlock.getLocation());
            if (territoryOwner != null) {
                Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
                boolean isMember = (playerNation != null && playerNation.getId().equals(territoryOwner.getId()));

                if (!isMember) {
                    boolean isContainer = (clickedBlock.getState() instanceof Container)
                            || isProtectedContainerType(clickedBlock.getType());

                    if (isContainer) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Ta skrzynia/pojemnik jest zabezpieczony przez państwo "
                                + territoryOwner.getName() + " [" + territoryOwner.getTag() + "]!");
                        return;
                    }

                    boolean isWarActive = playerNation != null && warManager.isSiegeActive(playerNation.getId(), territoryOwner.getId());
                    if (!isWarActive && isInteractiveMechanism(clickedBlock.getType())) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Teren chroniony przez państwo " + territoryOwner.getName() + "! Nie możesz używać tego mechanizmu.");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Nation territoryOwner = territoryManager.getNationAt(entity.getLocation());
        if (territoryOwner != null) {
            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            boolean isMember = (playerNation != null && playerNation.getId().equals(territoryOwner.getId()));
            if (!isMember) {
                if (entity instanceof ArmorStand
                        || entity instanceof ItemFrame
                        || entity instanceof GlowItemFrame
                        || entity instanceof Villager
                        || entity instanceof Animals) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Nie możesz wchodzić w interakcję z obiektami na terenie państwa " + territoryOwner.getName() + "!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            Nation territoryOwner = territoryManager.getNationAt(event.getEntity().getLocation());
            if (territoryOwner != null) {
                Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
                boolean isMember = (playerNation != null && playerNation.getId().equals(territoryOwner.getId()));
                if (!isMember) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Teren chroniony przez państwo " + territoryOwner.getName() + "!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNonMonsterDamage(EntityDamageByEntityEvent event) {
        Entity realDamager = event.getDamager();
        if (realDamager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            realDamager = shooter;
        }

        if (realDamager instanceof Player player) {
            Entity target = event.getEntity();

            // 0. Globalny brak friendly-fire w obrębie tego samego państwa oraz między sojusznikami (ALLY)
            if (target instanceof Player victim) {
                Nation damagerNation = nationManager.getNationByPlayer(player.getUniqueId());
                Nation victimNation = nationManager.getNationByPlayer(victim.getUniqueId());
                if (damagerNation != null && victimNation != null) {
                    if (damagerNation.getId().equals(victimNation.getId())) {
                        event.setCancelled(true);
                        return;
                    }
                    if (diplomacyManager != null && diplomacyManager.areAllies(damagerNation.getId(), victimNation.getId())) {
                        player.sendMessage(ChatColor.YELLOW + "Nie możesz atakować swoich sojuszników z państwa " + victimNation.getName() + "!");
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            Nation territoryOwner = territoryManager.getNationAt(target.getLocation());
            if (territoryOwner == null) return;

            Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
            boolean isMember = (playerNation != null && playerNation.getId().equals(territoryOwner.getId()));
            if (isMember) return;

            boolean atWarWithTerritory = (playerNation != null && warManager.isAtWar(playerNation.getId(), territoryOwner.getId()));

            // 1. Walka z graczami (PvP)
            if (target instanceof Player victim) {
                Nation victimNation = nationManager.getNationByPlayer(victim.getUniqueId());
                boolean atWarWithVictim = (playerNation != null && victimNation != null && warManager.isAtWar(playerNation.getId(), victimNation.getId()));

                if (atWarWithTerritory || atWarWithVictim) {
                    // Podczas wojny walka na terenie wroga jest w pełni dozwolona!
                    return;
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Nie możesz atakować graczy na terenie państwa " + territoryOwner.getName() + " bez wypowiedzenia wojny!");
                    return;
                }
            }

            // 2. Wojsko / Generał wroga podczas wojny
            if (atWarWithTerritory) {
                if (target instanceof Monster || target instanceof LivingEntity) {
                    return;
                }
            }

            // 3. Poza wojną lub dla zwierząt / obiektów
            if (!(target instanceof Monster)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Nie możesz niszczyć obiektów ani zwierząt na terenie państwa " + territoryOwner.getName() + "!");
            }
        }
    }

    private boolean isProtectedContainerType(Material mat) {
        String name = mat.name();
        return name.contains("CHEST")
                || name.contains("BARREL")
                || name.contains("SHULKER_BOX")
                || name.contains("FURNACE")
                || name.contains("SMOKER")
                || name.contains("DISPENSER")
                || name.contains("DROPPER")
                || name.contains("HOPPER")
                || name.contains("BREWING_STAND")
                || name.contains("ANVIL")
                || name.contains("BEACON")
                || name.contains("CRAFTER")
                || name.contains("POT")
                || name.contains("LECTERN");
    }

    private boolean isInteractiveMechanism(Material mat) {
        String name = mat.name();
        return name.contains("DOOR")
                || name.contains("TRAPDOOR")
                || name.contains("GATE")
                || name.contains("BUTTON")
                || name.contains("LEVER")
                || name.contains("PRESSURE_PLATE")
                || name.contains("REPEATER")
                || name.contains("COMPARATOR")
                || name.contains("DAYLIGHT_DETECTOR")
                || name.contains("TRIPWIRE_HOOK");
    }

    /**
     * Ochrona przed wybuchami: TNT i Creepery nie niszczą bloków na terenie państwa poza stanem wojny.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Nation owner = territoryManager.getNationAt(block.getLocation());
            if (owner == null) return false;
            return !warManager.isAtWarWithAny(owner.getId());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Nation owner = territoryManager.getNationAt(block.getLocation());
            if (owner == null) return false;
            return !warManager.isAtWarWithAny(owner.getId());
        });
    }

    /**
     * Strażnice Nadbrzeżne: Kontrola plaż i pobieranie cła z obcych łodzi na wodach państwa
     */
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (boat.getPassengers().isEmpty() || !(boat.getPassengers().get(0) instanceof Player player)) return;

        Location to = event.getTo();
        Nation owner = territoryManager.getNationAt(to);
        if (owner == null) return;

        Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());
        if (playerNation != null && playerNation.getId().equals(owner.getId())) return;

        // Jeśli to woda i obcy gracz przepływa w pobliżu
        if (to.getBlock().getType() == Material.WATER) {
            // Pobierz cło 1 raz na 2 minuty (np. 1-2 monety do skarbca)
            owner.addTreasury(0.5);
        }
    }

    private ItemStack getStructureCustomItem(StructureType type) {
        return switch (type) {
            case BASTION_CORE -> items.getBastionCore();
            case WATCHTOWER -> items.getWatchtower();
            case BARRACKS -> items.getBarracksCore();
            case SAWMILL_TOTEM -> items.getSawmillTotem();
            case MINING_SHAFT -> items.getMiningHead();
            case STATE_FARM -> items.getStateFarm();
            case WAR_COUNCIL -> items.getWarCouncilTable();
            case NATIONAL_VAULT -> items.getNationalVault();
        };
    }
}
