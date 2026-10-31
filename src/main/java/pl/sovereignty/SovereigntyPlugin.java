package pl.sovereignty;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.sovereignty.command.*;
import pl.sovereignty.listener.*;
import pl.sovereignty.manager.*;
import pl.sovereignty.model.SiegeMachine;
import pl.sovereignty.util.CustomItemRegistry;
import pl.sovereignty.util.RecipeRegistry;

import java.util.ArrayList;
import java.util.List;

public class SovereigntyPlugin extends JavaPlugin {
    private CustomItemRegistry items;
    private RecipeRegistry recipes;

    private NationManager nationManager;
    private TerritoryManager territoryManager;
    private TerritoryDisplayManager territoryDisplayManager;
    private EconomyManager economyManager;
    private ProductionManager productionManager;
    private VaultManager vaultManager;
    private SiegeMachineManager siegeMachineManager;
    private MilitaryManager militaryManager;
    private PaoloManager paoloManager;
    private PirateRaidManager pirateRaidManager;
    private RestorationManager restorationManager;
    private WarDamageManager warDamageManager;
    private WarManager warManager;
    private LeaderboardManager leaderboardManager;

    // Nowe systemy: Vault, Mury, Chunki, Upkeep, Mapy (Squaremap, Dynmap, BlueMap, Pl3xMap)
    private VaultEconomyManager vaultEconomyManager;
    private WallManager wallManager;
    private ChunkClaimManager chunkClaimManager;
    private UpkeepManager upkeepManager;
    private MapManager mapManager;
    private RegimeBuffManager regimeBuffManager;
    private AnimalSpawningManager animalSpawningManager;
    private SeatManager seatManager;
    private PlayerWoodTracker playerWoodTracker;

    // Dyplomacja, Baza Danych SQL, Walka, Fale Odrodzeń i Super Piec
    private DatabaseManager databaseManager;
    private DiplomacyManager diplomacyManager;
    private RespawnWaveManager respawnWaveManager;
    private SuperFurnaceManager superFurnaceManager;
    private CombatTagManager combatTagManager;
    private LanguageManager languageManager;

    private final List<BukkitTask> tasks = new ArrayList<>();

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            this.languageManager = new LanguageManager(this);
            this.languageManager.init();
        } catch (Exception e) {
            getLogger().warning("Nie udało się zapisać domyślnego config.yml: " + e.getMessage());
        }

        try {
            // 1. Rejestry
            this.items = new CustomItemRegistry(this);
            this.recipes = new RecipeRegistry(this, this.items);
            this.recipes.registerAll();
        } catch (Throwable e) {
            getLogger().severe("Błąd inicjalizacji receptur/przedmiotów: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // 2. Menedżery
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.init();

            this.combatTagManager = new CombatTagManager();

            this.nationManager = new NationManager(this);
            this.nationManager.loadNations();

            this.diplomacyManager = new DiplomacyManager(this, this.nationManager);
            this.diplomacyManager.setDatabaseManager(this.databaseManager);

            this.territoryManager = new TerritoryManager(this, this.nationManager);
            this.territoryManager.loadStructures();

            this.vaultManager = new VaultManager(this, this.territoryManager);
            this.vaultManager.loadVaults();

            this.territoryDisplayManager = new TerritoryDisplayManager(this, this.nationManager, this.territoryManager);

            this.economyManager = new EconomyManager(this, this.nationManager, this.items);
            this.playerWoodTracker = new PlayerWoodTracker(this);
            this.productionManager = new ProductionManager(this, this.nationManager, this.territoryManager, this.vaultManager, this.playerWoodTracker);
            this.siegeMachineManager = new SiegeMachineManager(this, this.nationManager, this.items);
            this.militaryManager = new MilitaryManager(this, this.nationManager, this.territoryManager);
            this.paoloManager = new PaoloManager(this, this.nationManager, this.militaryManager, this.items);
            this.pirateRaidManager = new PirateRaidManager(this, this.nationManager, this.territoryManager, this.items);
            this.restorationManager = new RestorationManager(this);
            this.warDamageManager = new WarDamageManager(this, this.restorationManager);
            this.warManager = new WarManager(this, this.nationManager, this.restorationManager);
            this.warManager.setDiplomacyManager(this.diplomacyManager);
            this.leaderboardManager = new LeaderboardManager(this, this.nationManager);

            // Inicjalizacja nowych systemów Vault, Murów, Chunków i Map (Squaremap, Dynmap, BlueMap, Pl3xMap)
            this.vaultEconomyManager = new VaultEconomyManager(this);
            this.wallManager = new WallManager(this, this.nationManager, this.vaultEconomyManager);
            this.wallManager.setDatabaseManager(this.databaseManager);

            this.chunkClaimManager = new ChunkClaimManager(this, this.nationManager, this.vaultEconomyManager);
            this.warManager.setChunkClaimManager(this.chunkClaimManager);

            this.upkeepManager = new UpkeepManager(this, this.nationManager, this.vaultEconomyManager, this.chunkClaimManager);
            this.upkeepManager.setTerritoryManager(this.territoryManager);
            this.upkeepManager.setMilitaryManager(this.militaryManager);

            this.mapManager = new MapManager(this, this.nationManager, this.territoryManager, this.chunkClaimManager, this.vaultEconomyManager);
            this.mapManager.init();

            this.upkeepManager.setMapManager(this.mapManager);
            this.warManager.setMapManager(this.mapManager);

            this.regimeBuffManager = new RegimeBuffManager(this.nationManager, this.territoryManager);
            this.animalSpawningManager = new AnimalSpawningManager(this);
            this.seatManager = new SeatManager(this);

            this.respawnWaveManager = new RespawnWaveManager(this, this.nationManager, this.territoryManager, this.warManager);
            this.superFurnaceManager = new SuperFurnaceManager(this, this.items);

            this.chunkClaimManager.setWallManager(this.wallManager);
            this.chunkClaimManager.setMapManager(this.mapManager);
            this.wallManager.setMapManager(this.mapManager);
        } catch (Throwable e) {
            getLogger().severe("Błąd inicjalizacji menedżerów nacji: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // 3. Listenery
            TerritoryProtectionListener territoryProtectionListener = new TerritoryProtectionListener(
                    nationManager, territoryManager, siegeMachineManager, militaryManager, paoloManager, warManager, warDamageManager, productionManager, vaultManager, this.vaultEconomyManager, this.wallManager, items);
            territoryProtectionListener.setMapManager(this.mapManager);
            territoryProtectionListener.setDiplomacyManager(this.diplomacyManager);
            getServer().getPluginManager().registerEvents(territoryProtectionListener, this);
            getServer().getPluginManager().registerEvents(new TerritoryDisplayListener(
                    territoryDisplayManager), this);
            getServer().getPluginManager().registerEvents(new MachineInteractionListener(
                    siegeMachineManager, nationManager, warDamageManager, items), this);
            getServer().getPluginManager().registerEvents(new MilitaryCommandListener(
                    militaryManager, paoloManager, nationManager, items), this);
            getServer().getPluginManager().registerEvents(new PaoloCombatListener(
                    paoloManager, militaryManager, pirateRaidManager, nationManager, warManager), this);
            getServer().getPluginManager().registerEvents(new GuiListener(
                    nationManager, militaryManager, paoloManager, items, wallManager, vaultEconomyManager, mapManager, this.vaultManager), this);
            getServer().getPluginManager().registerEvents(new CraftingListener(
                    this, this.items), this);
            getServer().getPluginManager().registerEvents(new SpawnerSilkTouchListener(
                    this), this);
            getServer().getPluginManager().registerEvents(new RegimeEffectsListener(
                    this.nationManager, this.territoryManager), this);
            getServer().getPluginManager().registerEvents(new HungerBalanceListener(
                    this), this);
            getServer().getPluginManager().registerEvents(new SitListener(
                    this.seatManager), this);
            getServer().getPluginManager().registerEvents(new MobDropBalanceListener(), this);
            getServer().getPluginManager().registerEvents(new NetheriteArmorBlockListener(), this);
            getServer().getPluginManager().registerEvents(this.playerWoodTracker, this);
            getServer().getPluginManager().registerEvents(this.combatTagManager, this);
            getServer().getPluginManager().registerEvents(this.respawnWaveManager, this);
            getServer().getPluginManager().registerEvents(this.superFurnaceManager, this);
            getServer().getPluginManager().registerEvents(new ChatListener(this.nationManager), this);
            getServer().getPluginManager().registerEvents(new pl.sovereignty.listener.ShopListener(this.vaultEconomyManager, this.economyManager, this.seatManager), this);
        } catch (Throwable e) {
            getLogger().severe("Błąd rejestracji listenerów: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // 4. Komendy
            SovereigntyCommand sovereigntyCmd = new SovereigntyCommand(
                    nationManager, territoryManager, economyManager, leaderboardManager, warManager,
                    paoloManager, pirateRaidManager, items, chunkClaimManager, wallManager,
                    vaultEconomyManager, mapManager, this.vaultManager);

            sovereigntyCmd.setSeatManager(this.seatManager);
            sovereigntyCmd.setDiplomacyManager(this.diplomacyManager);
            sovereigntyCmd.setCombatTagManager(this.combatTagManager);
            sovereigntyCmd.setLanguageManager(this.languageManager);

            PluginCommand countryCmd = getCommand("country");
            if (countryCmd != null) {
                countryCmd.setExecutor(sovereigntyCmd);
                countryCmd.setTabCompleter(sovereigntyCmd);
            }

            PluginCommand panstwoCmd = getCommand("panstwo");
            if (panstwoCmd != null) {
                panstwoCmd.setExecutor(sovereigntyCmd);
                panstwoCmd.setTabCompleter(sovereigntyCmd);
            }

            PluginCommand krajCmd = getCommand("kraj");
            if (krajCmd != null) {
                krajCmd.setExecutor(sovereigntyCmd);
                krajCmd.setTabCompleter(sovereigntyCmd);
            }

            PluginCommand claimCmd = getCommand("claim");
            if (claimCmd != null) {
                claimCmd.setExecutor(new ClaimCommand(sovereigntyCmd));
            }

            PluginCommand unclaimCmd = getCommand("unclaim");
            if (unclaimCmd != null) {
                unclaimCmd.setExecutor(new UnclaimCommand(sovereigntyCmd));
            }

            PluginCommand bankCmd = getCommand("bank");
            if (bankCmd != null) {
                BankCommand bankExecutor = new BankCommand(sovereigntyCmd);
                bankCmd.setExecutor(bankExecutor);
                bankCmd.setTabCompleter(bankExecutor);
            }

            PluginCommand skarbiecCmd = getCommand("skarbiec");
            if (skarbiecCmd != null) {
                pl.sovereignty.command.SkarbiecCommand skarbiecExecutor = new pl.sovereignty.command.SkarbiecCommand(sovereigntyCmd);
                skarbiecCmd.setExecutor(skarbiecExecutor);
                skarbiecCmd.setTabCompleter(skarbiecExecutor);
            }

            PluginCommand terenCmd = getCommand("teren");
            if (terenCmd != null) {
                TerritoryCommand territoryCmd = new TerritoryCommand(sovereigntyCmd);
                terenCmd.setExecutor(territoryCmd);
            }

            AdminSovereigntyCommand adminCmd = new AdminSovereigntyCommand(
                    nationManager, economyManager, siegeMachineManager, pirateRaidManager, restorationManager, items, mapManager);
            PluginCommand adminCmdPlugin = getCommand("sovereigntyadmin");
            if (adminCmdPlugin != null) {
                adminCmdPlugin.setExecutor(adminCmd);
                adminCmdPlugin.setTabCompleter(adminCmd);
            } else {
                getLogger().warning("Nie odnaleziono komendy 'sovereigntyadmin' w plugin.yml!");
            }

            CraftingCommand craftingCmd = new CraftingCommand(items);
            PluginCommand craftingCmdPlugin = getCommand("crafting");
            if (craftingCmdPlugin != null) {
                craftingCmdPlugin.setExecutor(craftingCmd);
                craftingCmdPlugin.setTabCompleter(craftingCmd);
            }

            PluginCommand tcCmd = getCommand("tc");
            if (tcCmd != null) {
                tcCmd.setExecutor(new TownChatCommand(this.nationManager));
            }
            PluginCommand acCmd = getCommand("ac");
            if (acCmd != null) {
                acCmd.setExecutor(new AllyChatCommand(this.nationManager, this.diplomacyManager));
            }

            pl.sovereignty.command.ShopCommand shopCmd = new pl.sovereignty.command.ShopCommand(this.vaultEconomyManager);
            PluginCommand sklepCmd = getCommand("sklep");
            if (sklepCmd != null) {
                sklepCmd.setExecutor(shopCmd);
                sklepCmd.setTabCompleter(shopCmd);
            }

            // Bezpieczna rejestracja komend (działa również po /reload, gdy commandMap nie został odświeżony przez Bukkit)
            registerCommandSafely("country", "Manage your island nation realm", List.of("panstwo", "nation", "kingdom", "kraj", "sovereignty", "reino", "pais"), sovereigntyCmd, sovereigntyCmd);
            registerCommandSafely("panstwo", "Zarządzanie państwem wyspiarskim", List.of("country", "kraj", "sovereignty", "nation"), sovereigntyCmd, sovereigntyCmd);
            registerCommandSafely("kraj", "Zarządzanie państwem wyspiarskim", List.of("panstwo", "country", "sovereignty", "nation"), sovereigntyCmd, sovereigntyCmd);
            registerCommandSafely("sklep", "Serwerowy rynek handlowy i sklep", List.of("shop", "bsklep"), shopCmd, shopCmd);
            registerCommandSafely("kosz", "Kosz na niepotrzebne przedmioty i odpady", List.of("trash", "bin", "smietnik", "smieci"), new KoszCommand(), null);
            registerCommandSafely("sit", "Siadanie na ziemi, schodkach i krzesłach", List.of("siadz", "usiadz", "siadaj"), new SitCommand(this.seatManager), null);
            registerCommandSafely("tc", "Czat nacji / państwa", List.of("townchat", "panstwochat", "kchat"), new TownChatCommand(this.nationManager), null);
            registerCommandSafely("ac", "Czat sojuszniczy", List.of("allychat", "sojuszchat"), new AllyChatCommand(this.nationManager, this.diplomacyManager), null);
        } catch (Throwable e) {
            getLogger().severe("Błąd rejestracji komend: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // 5. Zadania cykliczne (Schedulers)
            // a) Cykl produkcyjny (co 10 sekund) & weryfikacja traktów zaopatrzeniowych
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    territoryManager.verifyAllSupplyRoutes();
                    productionManager.runProductionCycle();
                } catch (Exception e) {
                    getLogger().warning("Błąd cyklu produkcyjnego: " + e.getMessage());
                }
            }, 200L, 200L));

            // b) Krok Bojowy Generała Paolo Or'e (co 2 sekundy / 40 ticków)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    paoloManager.tickDrumbeatAura();
                    militaryManager.tickArmyMaintenance();
                } catch (Exception e) {
                    getLogger().warning("Błąd aury Kroku Bojowego / utrzymania armii: " + e.getMessage());
                }
            }, 40L, 40L));

            // c) Obliczanie PKB (GDP) co 24 godziny w grze (24000 ticków)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    economyManager.calculateGlobalGdp(siegeMachineManager.getAllMachines().size());
                } catch (Exception e) {
                    getLogger().warning("Błąd obliczania PKB: " + e.getMessage());
                }
            }, 24000L, 24000L));

            // d) Dzienny pobór kosztów utrzymania (Upkeep - 5 $/chunk) co 24000 ticków
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    upkeepManager.runDailyUpkeepCycle();
                } catch (Exception e) {
                    getLogger().warning("Błąd cyklu poboru podatku gruntowego (Upkeep): " + e.getMessage());
                }
            }, 24000L, 24000L));

            // e) Autozapis danych co 5 minut (6000 ticków)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    nationManager.saveNations();
                    territoryManager.saveStructures();
                    if (vaultManager != null) {
                        vaultManager.saveVaults();
                    }
                    if (wallManager != null) {
                        wallManager.saveWallBlocks();
                    }
                } catch (Exception e) {
                    getLogger().warning("Błąd autozapisu: " + e.getMessage());
                }
            }, 6000L, 6000L));

            // f) Pasek Action Bar z bieżącym terytorium (własne państwo, obce państwo, dzicz) co 2s
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        territoryDisplayManager.updateActionBarForPlayer(p);
                    }
                } catch (Exception e) {
                    getLogger().warning("Błąd paska terytorium Action Bar: " + e.getMessage());
                }
            }, 20L, 40L));

            // g) Pasywne buffy i debuffy ustrojowe co 2 sekundy (40 ticków)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    regimeBuffManager.tickBuffs();
                } catch (Exception e) {
                    getLogger().warning("Błąd odświeżania buffów ustrojowych: " + e.getMessage());
                }
            }, 40L, 40L));

            // h) Odradzanie dzikiej zwierzyny na wyspach (Wildlife Repopulation)
            int animalIntervalTicks = Math.max(200, getConfig().getInt("wildlife.interval_seconds", 60) * 20);
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    animalSpawningManager.runAnimalSpawningCycle();
                } catch (Exception e) {
                    getLogger().warning("Błąd cyklu odradzania zwierzyny: " + e.getMessage());
                }
            }, 600L, animalIntervalTicks));

            // i) Okresowe sprawdzanie i usuwanie zablokowanej zbroi z netherytu (co 5 sekund)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        NetheriteArmorBlockListener.purgePlayerArmor(p);
                    }
                } catch (Exception ignored) {}
            }, 40L, 100L));

            // j) Dynamiczny system najazdów piratów (sprawdzanie co 10 sekund)
            tasks.add(Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    pirateRaidManager.tickDynamicRaidThreat();
                } catch (Exception e) {
                    getLogger().warning("Błąd cyklu zagrożenia najazdami piratów: " + e.getMessage());
                }
            }, 200L, 200L));
        } catch (Exception e) {
            getLogger().severe("Błąd uruchamiania schedulerów: " + e.getMessage());
            e.printStackTrace();
        }

        // Natychmiastowe usunięcie zbroi z netherytu u graczy online
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            NetheriteArmorBlockListener.purgePlayerArmor(p);
        }

        String activeLang = (languageManager != null) ? languageManager.getCurrentLanguage().toUpperCase() : "EN";
        getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "\n&6========================================================\n" +
                " &6&l⚔ SOVEREIGNTY & STEEL: ISLAND REALM (1.21.4) ⚔\n" +
                " &eOfficial Author: &f&lfilipGFF1\n" +
                " &eBuild: &bSOVEREIGNTY-STEEL-v1.0.0-FILIPGFF1-ORIGINAL &8| &eLang: &a" + activeLang + "\n" +
                " &7All Rights Reserved. Anti-Plagiarism Protection Active.\n" +
                "&6========================================================"
        ));
        getLogger().info("Sovereignty & Steel: Island Realm (1.21.4) w pełni załadowany!");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : tasks) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
        tasks.clear();

        try {
            if (nationManager != null) {
                nationManager.saveNations();
            }
            if (territoryManager != null) {
                territoryManager.saveStructures();
            }
            if (vaultManager != null) {
                vaultManager.saveVaults();
            }
            if (wallManager != null) {
                wallManager.saveWallBlocks();
            }
            if (mapManager != null) {
                mapManager.shutdown();
            }

            if (siegeMachineManager != null) {
                for (SiegeMachine machine : new ArrayList<>(siegeMachineManager.getAllMachines())) {
                    siegeMachineManager.dismantleMachine(machine);
                }
            }

            if (seatManager != null) {
                seatManager.cleanupAll();
            }

            if (playerWoodTracker != null) {
                playerWoodTracker.save();
            }

            if (superFurnaceManager != null) {
                superFurnaceManager.saveSuperFurnaces();
            }

            if (respawnWaveManager != null) {
                respawnWaveManager.cleanup();
            }

            if (databaseManager != null) {
                databaseManager.shutdown();
            }
        } catch (Exception e) {
            getLogger().severe("Błąd podczas wyłączania pluginu: " + e.getMessage());
        }

        getLogger().info("Sovereignty & Steel pomyślnie wyłączony i zapisany.");
    }

    public SeatManager getSeatManager() {
        return seatManager;
    }

    public CustomItemRegistry getItems() {
        return items;
    }

    public NationManager getNationManager() {
        return nationManager;
    }

    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public SiegeMachineManager getSiegeMachineManager() {
        return siegeMachineManager;
    }

    public MilitaryManager getMilitaryManager() {
        return militaryManager;
    }

    public PaoloManager getPaoloManager() {
        return paoloManager;
    }

    public PirateRaidManager getPirateRaidManager() {
        return pirateRaidManager;
    }

    public RestorationManager getRestorationManager() {
        return restorationManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public TerritoryDisplayManager getTerritoryDisplayManager() {
        return territoryDisplayManager;
    }

    public VaultEconomyManager getVaultEconomyManager() {
        return vaultEconomyManager;
    }

    public WallManager getWallManager() {
        return wallManager;
    }

    public ChunkClaimManager getChunkClaimManager() {
        return chunkClaimManager;
    }

    public UpkeepManager getUpkeepManager() {
        return upkeepManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public RegimeBuffManager getRegimeBuffManager() {
        return regimeBuffManager;
    }

    public AnimalSpawningManager getAnimalSpawningManager() {
        return animalSpawningManager;
    }

    public SquaremapHook getSquaremapHook() {
        return mapManager != null ? mapManager.getSquaremapHook() : null;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DiplomacyManager getDiplomacyManager() {
        return diplomacyManager;
    }

    public RespawnWaveManager getRespawnWaveManager() {
        return respawnWaveManager;
    }

    public SuperFurnaceManager getSuperFurnaceManager() {
        return superFurnaceManager;
    }

    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    private void registerCommandSafely(String name, String description, List<String> aliases,
                                      org.bukkit.command.CommandExecutor executor,
                                      org.bukkit.command.TabCompleter tabCompleter) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (tabCompleter != null) cmd.setTabCompleter(tabCompleter);
        } else {
            try {
                org.bukkit.command.Command dynCmd = new org.bukkit.command.Command(name, description, "/" + name, aliases) {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                        return executor.onCommand(sender, this, commandLabel, args);
                    }

                    @Override
                    public List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                        if (tabCompleter != null) {
                            List<String> list = tabCompleter.onTabComplete(sender, this, alias, args);
                            if (list != null) return list;
                        }
                        return super.tabComplete(sender, alias, args);
                    }
                };
                Bukkit.getCommandMap().register("sovereignty", dynCmd);
            } catch (Exception e) {
                getLogger().warning("Błąd awaryjnej rejestracji komendy " + name + ": " + e.getMessage());
            }
        }
    }
}
