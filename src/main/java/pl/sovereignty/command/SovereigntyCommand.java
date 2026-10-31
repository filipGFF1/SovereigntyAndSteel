package pl.sovereignty.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.sovereignty.gui.PoliticalRegimeGui;
import pl.sovereignty.gui.TechnologyUpgradeGui;
import pl.sovereignty.gui.TopLeaderboardGui;
import pl.sovereignty.gui.WallMenuGui;
import pl.sovereignty.manager.*;
import pl.sovereignty.model.DiplomaticRelationType;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationRole;
import pl.sovereignty.model.PaoloGeneral;
import pl.sovereignty.model.PirateRaid;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SovereigntyCommand implements CommandExecutor, TabCompleter {
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final EconomyManager economyManager;
    private final LeaderboardManager leaderboardManager;
    private final WarManager warManager;
    private final PaoloManager paoloManager;
    private final PirateRaidManager pirateRaidManager;
    private final CustomItemRegistry items;
    private final ChunkClaimManager chunkClaimManager;
    private final WallManager wallManager;
    private final VaultEconomyManager vaultEconomyManager;
    private final MapManager mapManager;
    private final VaultManager vaultManager;
    private SeatManager seatManager;
    private DiplomacyManager diplomacyManager;
    private CombatTagManager combatTagManager;
    private LanguageManager languageManager;

    public static final double CREATE_NATION_COST = 1000.0;

    public SovereigntyCommand(NationManager nationManager, TerritoryManager territoryManager,
                              EconomyManager economyManager, LeaderboardManager leaderboardManager,
                              WarManager warManager, PaoloManager paoloManager,
                              PirateRaidManager pirateRaidManager, CustomItemRegistry items,
                              ChunkClaimManager chunkClaimManager, WallManager wallManager,
                              VaultEconomyManager vaultEconomyManager, MapManager mapManager,
                              VaultManager vaultManager) {
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.economyManager = economyManager;
        this.leaderboardManager = leaderboardManager;
        this.warManager = warManager;
        this.paoloManager = paoloManager;
        this.pirateRaidManager = pirateRaidManager;
        this.items = items;
        this.chunkClaimManager = chunkClaimManager;
        this.wallManager = wallManager;
        this.vaultEconomyManager = vaultEconomyManager;
        this.mapManager = mapManager;
        this.vaultManager = vaultManager;
    }

    public void setSeatManager(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    public void setDiplomacyManager(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;
    }

    public void setCombatTagManager(CombatTagManager combatTagManager) {
        this.combatTagManager = combatTagManager;
    }

    public void setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą zarządzać państwem! / Only players can manage a nation!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc") || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("ayuda")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "stworz", "create", "crear" -> handleCreate(player, args);
            case "zapros", "invite", "invitar" -> handleInvite(player, args);
            case "dolacz", "join", "unirse" -> handleJoin(player, args);
            case "opusc", "leave", "abandonar" -> handleLeave(player);
            case "info" -> {
                if (args.length > 1 && (args[1].equalsIgnoreCase("author") || args[1].equalsIgnoreCase("autor")
                        || args[1].equalsIgnoreCase("tworca") || args[1].equalsIgnoreCase("about") || args[1].equalsIgnoreCase("version"))) {
                    handleAuthor(player);
                } else {
                    handleInfo(player, args);
                }
            }
            case "author", "autor", "tworca", "twórca", "about", "version" -> handleAuthor(player);
            case "top", "ranking", "leaderboard" -> TopLeaderboardGui.open(player, leaderboardManager);
            case "mennica", "mint", "ceca" -> handleMint(player, args);
            case "bank", "treasury", "banco" -> handleBank(player);
            case "skarbiec", "vault", "tesoro" -> handleSkarbiec(player, args);
            case "wplac", "deposit", "depositar" -> handleDeposit(player, args);
            case "wyplac", "withdraw", "retirar" -> handleWithdraw(player, args);
            case "claim", "zajmij", "reclamar" -> handleClaim(player);
            case "unclaim", "odpusc", "renunciar" -> handleUnclaim(player);
            case "mur", "mury", "wall", "walls", "muro", "muros" -> handleWall(player);
            case "ustroj", "ustrój", "regime", "regimen" -> handleRegime(player);
            case "upgrady", "ulepszenia", "technologia", "tech", "technology", "tecnologia" -> handleTech(player);
            case "kolor", "color" -> handleColor(player, args);
            case "wojna", "war", "guerra" -> handleWar(player, args);
            case "relacja", "relacje", "dyplomacja", "relation", "relations", "diplomacy", "relacion" -> handleRelation(player, args);
            case "kapituluj", "kapitulacja", "poddaj", "surrender", "rendirse" -> handleSurrender(player);
            case "oknowojenne", "okno", "siegewindow", "siege", "ventana" -> handleSiegeWindow(player, args);
            case "werbel", "szarza", "szarża", "taktyka", "wodz", "wódz", "napoleon", "general", "drumroll", "charge" -> handleDrumroll(player);
            case "haracz", "tribute", "tributo" -> handleTribute(player);
            case "receptury", "crafting", "craftingi", "recipes", "recipe", "recetas" -> pl.sovereignty.gui.RecipeBookGui.openMainMenu(player, items);
            case "teren", "gdzie", "granica", "granice", "dzicz", "territory", "land", "territorio" -> handleTerritory(player);
            case "rozwiaz", "rozwiąż", "disband", "disolver" -> handleDisband(player, args);
            case "testglod", "glod", "hunger" -> handleTestHunger(player);
            case "kosz", "trash", "bin", "smietnik", "smieci", "garbage", "basura" -> KoszCommand.openTrash(player);
            case "sit", "siadz", "usiadz", "siadaj", "seat", "sentarse" -> {
                if (seatManager != null) {
                    SitCommand.handleSit(player, seatManager);
                } else {
                    player.sendMessage(ChatColor.RED + "Moduł siadania nie jest zainicjalizowany!");
                }
            }
            default -> sendHelp(player);
        }

        return true;
    }

    public void handleAuthor(Player player) {
        if (languageManager != null) {
            languageManager.sendAuthorInfo(player);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6====================================================\n"
                    + " &6&l⚔ SOVEREIGNTY & STEEL: ISLAND REALM ⚔\n"
                    + " &eOfficial Author: &f&lfilipGFF1\n"
                    + " &eBuild Signature: &bSOVEREIGNTY-STEEL-v1.0.0-FILIPGFF1-ORIGINAL\n"
                    + " &7© 2026 filipGFF1. All rights reserved.\n"
                    + "&6====================================================\n"));
        }
    }

    public void handleBank(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby przeglądać Skarbiec Narodowy!");
            return;
        }

        int chunkCount = nation.getClaimedChunks().size();
        double upkeepPerChunk = UpkeepManager.BASE_UPKEEP_PER_CHUNK * nation.getRegime().getUpkeepMultiplier();
        double totalDailyUpkeep = chunkCount * upkeepPerChunk;

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l======== [ SKARBIEC NARODOWY: " + nation.getName().toUpperCase() + " ] ========\n"
                + "&7Stan konta w Vault: &6&l" + vaultEconomyManager.format(nation.getBankBalance()) + "\n"
                + "&7Ustrój państwa: &f" + nation.getRegime().getDisplayName() + "\n"
                + "&7Poziom technologii: &b" + nation.getTechTier().getDisplayName() + "\n"
                + "&7Pojemność Skarbca Krajowego: &e" + nation.getVaultTier().getDisplayName() + " &8(" + (nation.getVaultTier().getPages() * 54) + " slotów)\n"
                + (nation.getVaultTier().hasAutoSell() ? "&6⭐ Moduł Auto-Sell: " + (nation.isVaultAutoSell() ? "&aWŁĄCZONY" : "&cWYŁĄCZONY") + "\n" : "")
                + "&7Fortyfikacje graniczne: &e" + nation.getWallTier().getDisplayName() + "\n"
                + "&7Posiadane chunki: &e" + chunkCount + " chunków\n"
                + "&7Dzienny koszt utrzymania (Upkeep): &c" + vaultEconomyManager.format(totalDailyUpkeep)
                + " &8(" + vaultEconomyManager.format(upkeepPerChunk) + "/chunk)\n"
                + (nation.isIndebted()
                        ? "&c&lSTAN: ZADŁUŻENIE (&4Dzień " + nation.getDebtDays() + "/3&c) &7- Bonusy ustrojowe wyłączone!\n"
                        : "&a&lSTAN: FINANSE STABILNE &7(Wszystkie bonusy aktywne)\n")
                + " \n"
                + "&eWpłata do banku: &f/panstwo wplac <kwota>\n"
                + "&eWypłata z banku: &f/panstwo wyplac <kwota> &7(tylko Władca/Oficer)\n"
                + "&eSkrzynie skarbca: &f/panstwo skarbiec &7[1-5|ulepsz|autosell]\n"
                + "&6&l==================================================\n"));
    }

    public void handleSkarbiec(Player player, String[] args) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby przeglądać Skarbiec!");
            return;
        }

        if (args.length < 2) {
            if (nation.getVaultTier() == pl.sovereignty.model.VaultTier.TIER_1_WOODEN) {
                vaultManager.openVault(player, nation, 0);
            } else {
                pl.sovereignty.gui.VaultSelectorGui.open(player, nation, vaultManager);
            }
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "1", "2", "3", "4", "5" -> {
                int page = Integer.parseInt(sub) - 1;
                int maxPages = nation.getVaultTier().getPages();
                if (page < maxPages) {
                    vaultManager.openVault(player, nation, page);
                } else {
                    player.sendMessage(ChatColor.RED + "Skrzynia #" + (page + 1) + " nie została jeszcze odblokowana!");
                    player.sendMessage(ChatColor.YELLOW + "Użyj /panstwo skarbiec ulepsz, aby powiększyć skarbiec.");
                }
            }
            case "ulepsz", "upgrade", "upgrady", "ulepszenia" -> {
                pl.sovereignty.gui.VaultUpgradeGui.open(player, nation, vaultEconomyManager);
            }
            case "menu", "skrzynie", "lista" -> {
                pl.sovereignty.gui.VaultSelectorGui.open(player, nation, vaultManager);
            }
            case "autosell", "autospredaz" -> {
                if (!nation.getVaultTier().hasAutoSell()) {
                    player.sendMessage(ChatColor.RED + "Moduł Auto-Sell wymaga ulepszenia Skarbca do Poziomu V (Netherytowego)!");
                    return;
                }
                NationRole role = nation.getRole(player.getUniqueId());
                if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                    player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zarządzać modułem Auto-Sell!");
                    return;
                }
                boolean newState = !nation.isVaultAutoSell();
                nation.setVaultAutoSell(newState);
                nationManager.saveNations();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&l[AUTO-SELL] &fZmieniono stan modułu Auto-Sell na: " + (newState ? "&aWŁĄCZONY" : "&cWYŁĄCZONY")));
            }
            case "sprzedaj", "sell" -> {
                if (!nation.getVaultTier().hasAutoSell()) {
                    player.sendMessage(ChatColor.RED + "Moduł sprzedaży wymaga ulepszenia Skarbca do Poziomu V (Netherytowego)!");
                    return;
                }
                NationRole role = nation.getRole(player.getUniqueId());
                if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                    player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może spieniężać surowce państwowe!");
                    return;
                }
                int totalPages = nation.getVaultTier().getPages();
                double earned = vaultManager.sellAllVaultContents(nation);
                if (earned > 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                    nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                            "&6&l[AUTO-SELL] &fSpieniężono surowce ze skrzyń skarbca (#1 - #" + totalPages + ") za łączną kwotę: &a+"
                                    + vaultEconomyManager.format(earned) + " &7(zdeponowano w Skarbcu Narodowym)!"));
                    vaultManager.saveVaults();
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Skrzynie skarbca są puste lub nie zawierają surowców do skupu.");
                }
            }
            case "bank", "finanse", "stan" -> handleBank(player);
            default -> {
                player.sendMessage(ChatColor.YELLOW + "Użycie komendy /panstwo skarbiec:");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo skarbiec" + ChatColor.GRAY + " : otwiera skrzynie skarbca");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo skarbiec <1-5>" + ChatColor.GRAY + " : otwiera wybraną skrzynię");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo skarbiec ulepsz" + ChatColor.GRAY + " : otwiera menu rozbudowy pojemności");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo skarbiec autosell" + ChatColor.GRAY + " : przełącza auto-sell (Tier 5)");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo skarbiec sprzedaj" + ChatColor.GRAY + " : spienięża zawartość wszystkich skrzyń skarbca");
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + "/panstwo bank" + ChatColor.GRAY + " : finanse i budżet państwa");
            }
        }
    }

    public void handleClaim(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby asymilować nowy teren!");
            return;
        }
        chunkClaimManager.claimChunk(player, nation);
    }

    public void handleUnclaim(Player player) {
        if (combatTagManager != null && combatTagManager.isInCombat(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz zrzekać się terytorium podczas walki (Combat-Tag: "
                    + combatTagManager.getRemainingCombatSeconds(player.getUniqueId()) + "s)!");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby zarządzać terytorium!");
            return;
        }
        chunkClaimManager.unclaimChunk(player, nation);
    }

    public void handleWall(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }
        WallMenuGui.open(player, nation, wallManager, vaultEconomyManager);
    }

    public void handleRegime(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }
        PoliticalRegimeGui.open(player, nation, vaultEconomyManager);
    }

    public void handleTech(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }
        TechnologyUpgradeGui.open(player, nation, vaultEconomyManager);
    }

    public void handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo kolor <#HEX> (np. /panstwo kolor #FF5733)");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }
        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER && role != NationRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zmienić barwy państwa!");
            return;
        }

        String hex = args[1];
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        if (!hex.matches("^#([A-Fa-f0-9]{6})$")) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowy kod koloru HEX! Przykład: #FF5733 lub #3388FF");
            return;
        }

        nation.setMapColorHex(hex);
        nationManager.saveNations();
        if (mapManager != null) {
            mapManager.updateNation(nation);
        }
        String mapNames = (mapManager != null && mapManager.hasAnyEnabled()) ? mapManager.getEnabledMapNames() : "Brak aktywnych";
        player.sendMessage(ChatColor.GREEN + "Pomyślnie zmieniono barwy państwa na mapach WWW (" + ChatColor.AQUA + mapNames + ChatColor.GREEN + ") na: "
                + ChatColor.GOLD + hex.toUpperCase());
    }

    public void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo wplac <kwota>");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            vaultEconomyManager.depositToNationBank(nation, player, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Podaj prawidłową liczbę!");
        }
    }

    public void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo wyplac <kwota>");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }

        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER && role != NationRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może wypłacać środki ze Skarbca Narodowego!");
            return;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            vaultEconomyManager.withdrawFromNationBank(nation, player, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Podaj prawidłową liczbę!");
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (combatTagManager != null && combatTagManager.isInCombat(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz założyć państwa podczas walki (Combat-Tag: "
                    + combatTagManager.getRemainingCombatSeconds(player.getUniqueId()) + "s)!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo stworz <nazwa> <tag 2-5 znaków>");
            player.sendMessage(ChatColor.GRAY + "Koszt rejestracji nowego państwa: " + ChatColor.GOLD
                    + vaultEconomyManager.format(CREATE_NATION_COST));
            return;
        }

        if (nationManager.getNationByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Już należysz do państwa! Aby założyć własne, musisz najpierw opuścić obecne (/panstwo opusc).");
            return;
        }

        String name = args[1];
        String tag = args[2];
        if (tag.length() < 2 || tag.length() > 5) {
            player.sendMessage(ChatColor.RED + "Tag musi mieć od 2 do 5 znaków!");
            return;
        }

        if (nationManager.getNationByName(name) != null) {
            player.sendMessage(ChatColor.RED + "Państwo o nazwie '" + name + "' już istnieje! Wybierz inną nazwę.");
            return;
        }

        // Sprawdzenie funduszy gracza
        if (!vaultEconomyManager.hasPlayerBalance(player, CREATE_NATION_COST)) {
            player.sendMessage(ChatColor.RED + "Założenie państwa wymaga opłaty rejestracyjnej w wysokości "
                    + ChatColor.GOLD + vaultEconomyManager.format(CREATE_NATION_COST) + ChatColor.RED + "!");
            player.sendMessage(ChatColor.GRAY + "Twój obecny stan konta: " + ChatColor.YELLOW
                    + vaultEconomyManager.format(vaultEconomyManager.getPlayerBalance(player)));
            return;
        }

        // Pobranie opłaty
        if (!vaultEconomyManager.chargePlayer(player, CREATE_NATION_COST)) {
            player.sendMessage(ChatColor.RED + "Błąd transakcji bankowej podczas pobierania opłaty rejestracyjnej!");
            return;
        }

        Nation nation = nationManager.createNation(name, tag, player);
        if (nation == null) {
            // Zwrot w przypadku niespodziewanego błędu
            vaultEconomyManager.depositPlayer(player, CREATE_NATION_COST);
            player.sendMessage(ChatColor.RED + "Nie udało się założyć państwa. Zwrócono pobrane środki.");
            return;
        }

        // Dajemy graczowi Rdzeń Bastionu
        player.getInventory().addItem(items.getBastionCore());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l================ [ SOVEREIGNTY ] ================\n"
                + "&aPomyślnie założono nowe państwo wyspiarskie: &e" + nation.getName() + " &8[&f" + nation.getTag() + "&8]&a!\n"
                + "&7Pobrano opłatę rejestracyjną: &6" + vaultEconomyManager.format(CREATE_NATION_COST) + "\n"
                + "&eOtrzymałeś &5Rdzeń Bastionu&e. Postaw go w strategicznym punkcie wyspy, aby wyznaczyć stolicę!\n"
                + "&6&l================================================"));

        if (mapManager != null) {
            mapManager.updateNation(nation);
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo zapros <nick_gracza>");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby zapraszać graczy!");
            return;
        }
        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER && role != NationRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca lub Generał/Oficer państwa może wysyłać zaproszenia!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono gracza online o nicku: " + args[1]);
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz zaprosić samego siebie!");
            return;
        }

        Nation targetNation = nationManager.getNationByPlayer(target.getUniqueId());
        if (targetNation != null) {
            if (targetNation.getId().equals(nation.getId())) {
                player.sendMessage(ChatColor.YELLOW + "Gracz " + target.getName() + " jest już obywatelem Twojego państwa!");
            } else {
                player.sendMessage(ChatColor.RED + "Gracz " + target.getName() + " należy już do innego państwa (" + targetNation.getName() + ")!");
            }
            return;
        }

        nationManager.invitePlayer(target.getUniqueId(), nation.getId());
        player.sendMessage(ChatColor.GREEN + "Wysłano zaproszenie do państwa dla gracza " + ChatColor.GOLD + target.getName() + ChatColor.GREEN + "!");

        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l[ZAPROSZENIE DO PAŃSTWA]\n"
                + "&eOtrzymałeś oficjalne zaproszenie do państwa &f" + nation.getName() + " &8[" + nation.getTag() + "] &eod gracza &f" + player.getName() + "&e!\n"
                + "&aWpisz &e/panstwo dolacz " + nation.getName() + " &aaby dołączyć (zaproszenie jest ważne przez 2 minuty).\n"));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    private void handleJoin(Player player, String[] args) {
        if (combatTagManager != null && combatTagManager.isInCombat(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz dołączyć do państwa podczas walki (Combat-Tag: "
                    + combatTagManager.getRemainingCombatSeconds(player.getUniqueId()) + "s)!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo dolacz <nazwa>");
            return;
        }
        Nation nation = nationManager.getNationByName(args[1]);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono państwa o takiej nazwie!");
            return;
        }

        if (nationManager.getNationByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Należysz już do państwa! Najpierw wpisz /panstwo opusc.");
            return;
        }

        // Weryfikacja zaproszenia
        if (!nationManager.hasInvite(player.getUniqueId(), nation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie posiadasz ważnego zaproszenia do państwa " + nation.getName() + "!");
            player.sendMessage(ChatColor.GRAY + "Poproś Władcę lub Generała o zaproszenie komendą: /panstwo zapros " + player.getName());
            return;
        }

        if (nationManager.joinNation(player, nation)) {
            nationManager.removeInvite(player.getUniqueId(), nation.getId());
            player.sendMessage(ChatColor.GREEN + "Dołączyłeś do obywateli państwa " + nation.getName() + "!");
            for (UUID memberUuid : nation.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(memberUuid);
                if (m != null && !m.getUniqueId().equals(player.getUniqueId())) {
                    m.sendMessage(ChatColor.GOLD + "[PAŃSTWO] " + ChatColor.GREEN + "Gracz " + player.getName() + " dołączył do państwa!");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Wystąpił błąd podczas dołączania do państwa.");
        }
    }

    private void handleLeave(Player player) {
        if (nationManager.leaveNation(player)) {
            player.sendMessage(ChatColor.YELLOW + "Opuściłeś swoje dotychczasowe państwo.");
        } else {
            player.sendMessage(ChatColor.RED + "Nie należysz do państwa lub jesteś jego przywódcą!");
        }
    }

    private void handleInfo(Player player, String[] args) {
        Nation nation;
        if (args.length > 1) {
            nation = nationManager.getNationByName(args[1]);
        } else {
            nation = nationManager.getNationByPlayer(player.getUniqueId());
        }

        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono danych państwa.");
            return;
        }

        boolean isSovereign = economyManager.isTradeSovereign(nation.getId());
        double inflation = nation.getInflationFactor();

        // Relacje dyplomatyczne
        List<String> allies = new ArrayList<>();
        List<String> rivals = new ArrayList<>();
        List<String> naps = new ArrayList<>();
        List<String> truces = new ArrayList<>();
        for (Map.Entry<UUID, DiplomaticRelationType> entry : nation.getAllRelations().entrySet()) {
            Nation other = nationManager.getNation(entry.getKey());
            if (other == null) continue;
            switch (entry.getValue()) {
                case ALLY -> allies.add(other.getName());
                case RIVAL -> rivals.add(other.getName());
                case NAP -> {
                    if (nation.hasActiveNAP(entry.getKey())) naps.add(other.getName());
                }
                case TRUCE -> {
                    if (nation.hasActiveTruce(entry.getKey())) truces.add(other.getName());
                }
            }
        }

        // Tarcza pokoju
        String peaceShieldStr = nation.isPeaceShieldActive()
                ? "&aAKTYWNA &8(pozostało " + String.format("%.1f", nation.getPeaceShieldRemainingHours()) + "h)"
                : "&8Nieaktywna";

        // Okno oblężnicze bastionu
        String siegeWindowStr = String.format("%02d:00 - %02d:00", nation.getSiegeWindowStartHour(), nation.getSiegeWindowEndHour())
                + (nation.isWithinSiegeWindow() ? " &c[OKNO OBLĘŻENIA OTWARTE!]" : " &a[BEZPIECZNE]");

        // Wasal / Suzeren
        String vassalStr = "";
        if (nation.getOverlordNationId() != null) {
            Nation overlord = nationManager.getNation(nation.getOverlordNationId());
            String oName = overlord != null ? overlord.getName() : "Nieznane";
            vassalStr = "&c&lWASAL: &fPaństwo podlega &e" + oName + " &7(Trybut: " + nation.getTributeDaysRemaining() + " dni)\n";
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l======== [ PAŃSTWO: " + nation.getName().toUpperCase() + " (" + nation.getTag() + ") ] ========\n"
                + (isSovereign ? "&6★ TYTUŁ: &e&lWŁADCA HANDLU WYSPY (0.5% cła tranzytowego)!\n" : "")
                + vassalStr
                + "&7Ustrój: &e" + nation.getRegime().getDisplayName() + "\n"
                + "&7Poziom Technologii: &b" + nation.getTechTier().getDisplayName() + "\n"
                + "&7Mury: &6" + nation.getWallTier().getDisplayName() + "\n"
                + "&7PKB (GDP): &a" + nation.getGdp() + " pkt\n"
                + "&7Skarbiec Vault: &6" + vaultEconomyManager.format(nation.getBankBalance()) + "\n"
                + "&7Spichlerz: " + (nation.isStarving() ? "&c&lGŁÓD (0)!" : "&a" + (int) nation.getFoodStockpile() + " żywności") + "\n"
                + "&7Magazyn Drewna: &6" + (int) nation.getWoodStockpile() + " kłód &8| &7Urobek: &f" + (int) nation.getRawMaterialsStockpile() + "\n"
                + "&7Chunki: &f" + nation.getClaimedChunks().size() + " &8| &7Struktury: &f" + nation.getStructures().size() + "\n"
                + "&7Tarcza Pokoju (48h): " + peaceShieldStr + "\n"
                + "&7Okno obrony Bastionu: &e" + siegeWindowStr + "\n"
                + "&7Wskaźnik Dobrobytu: &b" + (int) nation.getWelfareScore() + " pkt"
                + (nation.getFarmCount() > 0 ? " &8(Centra rolnicze: &a+" + (nation.getFarmCount() * 25) + "&8)" : "") + "\n"
                + "&7Potęga Militarna: &c" + nation.getMilitaryScore() + " pkt &8| &7Rajdy piratów odparte: &9" + nation.getPiratesDefeated() + "\n"
                + (nation.getClaimedChunks().size() >= 10
                    ? "&7Zagrożenie rajdem: &e" + (int) nation.getRaidThreatChance() + "% &8(+" + (nation.getClaimedChunks().size() >= 20 ? "10% co 5m" : "10% co 10m") + ")\n"
                    : "")
                + "&7Sojusznicy: " + (allies.isEmpty() ? "&8brak" : "&a" + String.join(", ", allies)) + "\n"
                + "&7Pakty o nieagresji (NAP): " + (naps.isEmpty() ? "&8brak" : "&e" + String.join(", ", naps)) + "\n"
                + "&7Wrogowie/Rywale: " + (rivals.isEmpty() ? "&8brak" : "&c" + String.join(", ", rivals)) + "\n"
                + (truces.isEmpty() ? "" : "&7Aktywne rozejmy: &b" + String.join(", ", truces) + "\n")
                + "&6&l========================================\n"));
    }

    private void handleMint(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo mennica <miedz|zloto> <ilosc_sztabek>");
            return;
        }
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby bić walutę w mennicy!");
            return;
        }

        String typeStr = args[1].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Ilość musi być dodatnią liczbą!");
            return;
        }

        Material metal = typeStr.startsWith("m") ? Material.COPPER_INGOT : Material.GOLD_INGOT;
        economyManager.mintCurrency(player, nation, metal, amount);
    }

    private void handleWar(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /panstwo wojna <nazwa_wrogiego_panstwa>");
            return;
        }
        Nation attacker = nationManager.getNationByPlayer(player.getUniqueId());
        if (attacker == null || attacker.getRole(player.getUniqueId()) != NationRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca państwa może oficjalnie wypowiedzieć wojnę!");
            return;
        }

        Nation defender = nationManager.getNationByName(args[1]);
        if (defender == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono takiego państwa!");
            return;
        }

        if (warManager.declareWar(attacker, defender)) {
            player.sendMessage(ChatColor.GREEN + "Wypowiedziano wojnę państwu " + defender.getName() + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Już jesteście w stanie wojny z tym państwem lub to Wasze własne państwo!");
        }
    }

    private void handleRelation(Player player, String[] args) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa, aby zarządzać dyplomacją!");
            return;
        }

        if (diplomacyManager == null) {
            player.sendMessage(ChatColor.RED + "Moduł dyplomacji nie jest aktywny!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l=== Dyplomacja i Relacje Międzynarodowe ===\n"
                    + "&e/panstwo relacja sojusz <państwo> &7- Zawarcie lub akceptacja sojuszu (wspólny czat, brak friendly fire)\n"
                    + "&e/panstwo relacja nap <państwo> &7- Pakt o Nieagresji na 3 dni (blokada wypowiedzenia wojny)\n"
                    + "&e/panstwo relacja neutralny <państwo> &7- Powrót do stanu neutralnego\n"
                    + "&e/panstwo relacja rywal <państwo> &7- Ogłoszenie rywala (bonusy do PKB za walkę)\n"));
            return;
        }

        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER && role != NationRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca lub Generał/Oficer państwa może zarządzać relacjami dyplomatycznymi!");
            return;
        }

        String typeStr = args[1].toLowerCase();
        String targetName = args[2];
        Nation targetNation = nationManager.getNationByName(targetName);

        if (targetNation == null) {
            player.sendMessage(ChatColor.RED + "Nie odnaleziono państwa o nazwie: " + targetName);
            return;
        }

        if (nation.getId().equals(targetNation.getId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz zmieniać relacji z własnym państwem!");
            return;
        }

        switch (typeStr) {
            case "sojusz", "ally" -> diplomacyManager.handleAllyCommand(player, nation, targetNation);
            case "nap", "pakt" -> diplomacyManager.handleNAPCommand(player, nation, targetNation);
            case "neutralny", "neutral" -> diplomacyManager.handleNeutralCommand(player, nation, targetNation);
            case "rywal", "rival", "wrog" -> diplomacyManager.handleRivalCommand(player, nation, targetNation);
            default -> player.sendMessage(ChatColor.RED + "Nieznany typ relacji! Wybierz: sojusz, nap, neutralny, rywal.");
        }
    }

    private void handleSurrender(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }

        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca państwa może ogłosić bezwarunkową kapitulację!");
            return;
        }

        warManager.handleCapitulation(player, nation);
    }

    private void handleSiegeWindow(Player player, String[] args) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
            return;
        }

        NationRole role = nation.getRole(player.getUniqueId());
        if (role != NationRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Tylko Władca państwa może modyfikować dzienne okno oblężnicze!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&eAktualne okno oblężenia bastionu: &f"
                            + String.format("%02d:00 - %02d:00", nation.getSiegeWindowStartHour(), nation.getSiegeWindowEndHour()) + "\n"
                            + "&7Użycie: &f/panstwo oknowojenne <godzina_start 0-23> <godzina_koniec 0-23>\n"
                            + "&7Okno obronne musi trwać dokładnie od 3 do 4 godzin (np. 18 22)."));
            return;
        }

        try {
            int start = Integer.parseInt(args[1]);
            int end = Integer.parseInt(args[2]);

            if (start < 0 || start > 23 || end < 0 || end > 23) {
                player.sendMessage(ChatColor.RED + "Godzina musi być liczbą w przedziale od 0 do 23!");
                return;
            }

            int duration = end - start;
            if (duration < 0) duration += 24;

            if (duration < 3 || duration > 4) {
                player.sendMessage(ChatColor.RED + "Okno oblężnicze musi trwać od 3 do 4 godzin (podano: " + duration + "h)!");
                return;
            }

            nation.setSiegeWindowStartHour(start);
            nation.setSiegeWindowEndHour(end);
            nationManager.saveNations();

            player.sendMessage(ChatColor.GREEN + "Pomyślnie ustawiono okno podatności Bastionu na: "
                    + ChatColor.GOLD + String.format("%02d:00 - %02d:00", start, end) + ChatColor.GREEN + "!");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Podaj poprawne godziny liczbowe!");
        }
    }

    private void handleDrumroll(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) return;

        PaoloGeneral general = paoloManager.getGeneral(nation.getId());
        if (general == null) {
            player.sendMessage(ChatColor.RED + "Twoje państwo nie posiada obecnie aktywnego wodza polowego (Paolo lub Kłota Napoleona)!");
            return;
        }

        paoloManager.activateDrumrollOfWar(player, general);
    }

    private void handleTribute(Player player) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) return;

        for (PirateRaid raid : pirateRaidManager.getActiveRaids()) {
            if (raid.getTargetNationId().equals(nation.getId()) && !raid.isCompleted()) {
                if (pirateRaidManager.payTribute(player, raid)) {
                    player.sendMessage(ChatColor.GREEN + "Pomyślnie opłacono haracz i ocalono infrastrukturę!");
                }
                return;
            }
        }
        player.sendMessage(ChatColor.YELLOW + "Brak aktywnego desantu piratów żądającego haraczu.");
    }

    public void handleTerritory(Player player) {
        org.bukkit.Location loc = player.getLocation();
        Nation at = territoryManager.getNationAt(loc);
        if (at == null) {
            at = chunkClaimManager.getNationAtChunk(loc.getWorld().getName(), loc.getChunk().getX(), loc.getChunk().getZ());
        }
        Nation playerNation = nationManager.getNationByPlayer(player.getUniqueId());

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "\n&6&l=== Status Terytorium Wyspy ==="));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Koordynaty: &fX: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ() + " &8(" + loc.getWorld().getName() + " | Chunk: " + loc.getChunk().getX() + ", " + loc.getChunk().getZ() + ")"));

        if (at == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Aktualna strefa: &8[&7DZICZ&8] &7- Ziemia Niczyja"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7Jurysdykcja: &eBrak ochrony (można asymilować: &6/panstwo claim&e)."));
        } else {
            boolean isOwn = (playerNation != null && playerNation.getId().equals(at.getId()));
            if (isOwn) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Aktualna strefa: &a&lOJCZYZNA &8[&2" + at.getName() + " &8(&a" + at.getTag() + "&8)&8]"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Ustrój: &f" + at.getRegime().getDisplayName() + " &8| &7Mury: &e" + at.getWallTier().getDisplayName()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Ochrona terytorialna: &aAktywna &7(własne terytorium państwowe)"));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Aktualna strefa: &c&lOBCE PAŃSTWO &8[&4" + at.getName() + " &8(&c" + at.getTag() + "&8)&8]"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Ustrój: &f" + at.getRegime().getDisplayName() + " &8| &7Mury: &e" + at.getWallTier().getDisplayName()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&7Ochrona terytorialna: &cAktywna &7(budowanie zablokowane poza wojną)"));
            }
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l================================="));
    }

    private void handleDisband(Player player, String[] args) {
        Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnego państwa!");
            return;
        }
        if (!nation.getLeaderUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Tylko Władca (Lider) państwa może je rozwiązać!");
            return;
        }
        // Wymagane potwierdzenie: /panstwo rozwiaz POTWIERDZ
        if (args.length < 2 || !args[1].equalsIgnoreCase("POTWIERDZ")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&4&l[UWAGA] Rozwiązanie państwa jest operacją NIEODWRACALNĄ!\n"
                    + "&cWszyscy obywatele zostaną wyrzuceni, Skarbiec Narodowy zostanie opróżniony.\n"
                    + "&eAby potwierdzić, wpisz: &f/panstwo rozwiaz POTWIERDZ\n"));
            return;
        }

        String nationName = nation.getName();
        if (mapManager != null) {
            mapManager.removeNation(nation);
        }
        nationManager.disbandNation(nation);

        org.bukkit.Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4&l[UPADEK PAŃSTWA] &cPaństwo &f" + nationName
                + " &czostało oficjalnie rozwiązane przez swojego Władcę.\n"
                + "&7Jego tereny powróciły do dzikiej natury wyspy...\n"));
    }

    private void handleTestHunger(Player player) {
        int foodLevel = player.getFoodLevel();
        float saturation = player.getSaturation();
        float exhaustion = player.getExhaustion();
        org.bukkit.Difficulty diff = player.getWorld().getDifficulty();
        org.bukkit.GameMode gm = player.getGameMode();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6&l======== [ DIAGNOSTYKA GŁODU ] ========\n"
                + "&ePoziom głodu (FoodLevel): &f" + foodLevel + "/20\n"
                + "&eNasycenie (Saturation): &f" + String.format("%.2f", saturation) + "\n"
                + "&eWyczerpanie (Exhaustion): &f" + String.format("%.2f", exhaustion) + "\n"
                + "&ePoziom trudności świata: &f" + diff.name() + (diff == org.bukkit.Difficulty.PEACEFUL ? " &c(PEACEFUL - GŁÓD WYŁĄCZONY W MINECRAFT!)" : " &a(Głód włączony)") + "\n"
                + "&eTryb gracza (GameMode): &f" + gm.name() + (gm != org.bukkit.GameMode.SURVIVAL && gm != org.bukkit.GameMode.ADVENTURE ? " &c(W tym trybie głód nie spada!)" : " &a(OK)") + "\n"
                + "&7Zeruję nasycenie i odejmuję 2 punkty głodu do testu...\n"));

        player.setSaturation(0f);
        player.setFoodLevel(Math.max(0, foodLevel - 2));
        player.setExhaustion(3.9f);
        player.sendMessage(ChatColor.GREEN + "Nasycenie wyzerowane do 0.0, pasek głodu ustawiony na " + player.getFoodLevel() + "/20! Pobiegaj lub poskacz, pasek natychmiast zacznie spadać.");
    }

    private void sendHelp(Player player) {
        String lang = (languageManager != null) ? languageManager.getCurrentLanguage() : "en";

        if ("en".equalsIgnoreCase(lang)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l=== Sovereignty & Steel: Island Realm (1.21.4) ===\n"
                    + "&e/country bank &7(or &e/bank&7) - &fNational Treasury, finances & Upkeep\n"
                    + "&e/country vault [1-5|upgrade|autosell] &7- &fMulti-page Storage & Auto-Sell\n"
                    + "&e/country deposit <amount> &7- &fDeposit funds to National Treasury\n"
                    + "&e/country withdraw <amount> &7- &fWithdraw funds (Leader/Officer only)\n"
                    + "&e/country claim &7(or &e/claim&7) - &fAssimilate chunk for nation with cooldown\n"
                    + "&e/country unclaim &7(or &e/unclaim&7) - &fRelinquish current chunk\n"
                    + "&e/country wall &7- &fManage self-repairing defensive perimeter walls\n"
                    + "&e/country regime &7- &fChoose political doctrine (8 doctrines available)\n"
                    + "&e/country tech &7- &fNational technology research tree (Tier I - V)\n"
                    + "&e/country color <#HEX> &7- &fChange nation border color on web maps\n"
                    + "&e/country territory &7(or &e/teren&7) - &fInspect current chunk ownership\n"
                    + "&e/country recipes &7(or &e/crafting&7) - &fBrowse all custom items & machines\n"
                    + "&e/country create <name> <tag> &7- &fFound a new island nation & receive Bastion Core\n"
                    + "&e/country invite <player> &7- &fInvite a player to your nation\n"
                    + "&e/country join <name> &7- &fAccept an invitation & join a nation\n"
                    + "&e/country leave &7- &fLeave your current nation\n"
                    + "&e/country disband CONFIRM &7- &4Permanently&7 dissolve your nation\n"
                    + "&e/country info [name] &7- &fInspect economy, GDP, fortifications & military\n"
                    + "&e/country mint <copper|gold> <amount> &7- &fMint national currency coins\n"
                    + "&e/country top &7- &fOpen island rankings leaderboard\n"
                    + "&e/country war <nation> &7- &fDeclare war on a rival realm\n"
                    + "&e/country relation <ally|nap|neutral|rival> <nation> &7- &fDiplomatic pacts\n"
                    + "&e/country surrender &7- &fSurrender war (cede 25% land, 70% treasury, tribute)\n"
                    + "&e/country siegewindow <from> <to> &7- &fConfigure bastion defense window\n"
                    + "&e/tc <msg> &7- &fNation chat | &e/ac <msg> &7- &fAllied chat\n"
                    + "&e/country general &7(or &e/country drumroll&7) - &fGeneral Paolo Or'e War March ability\n"
                    + "&e/country tribute &7- &fPay tribute to pirate fleets\n"
                    + "&e/country trash &7(or &e/trash&7) - &fWaste disposal bin\n"
                    + "&e/country sit &7(or &e/sit&7) - &fSit down on stairs, chairs, or ground\n"
                    + "&e/country author &7- &fPlugin author credentials & verification\n"
                    + "&8-----------------------------------------------------\n"
                    + "&7Sovereignty & Steel • Developed & Crafted by &6&lfilipGFF1\n"
                    + "&6&l====================================================\n"));
        } else if ("es".equalsIgnoreCase(lang)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l=== Sovereignty & Steel: Island Realm (1.21.4) ===\n"
                    + "&e/pais banco &7(o &e/banco&7) - &fTesorería Nacional, finanzas y mantenimiento\n"
                    + "&e/pais tesoro [1-5|mejorar|autosell] &7- &fBóvedas de almacenamiento y venta automática\n"
                    + "&e/pais depositar <cantidad> &7- &fDepositar dinero en la Tesorería\n"
                    + "&e/pais retirar <cantidad> &7- &fRetirar dinero de la Tesorería (Gobernante/Oficial)\n"
                    + "&e/pais reclamar &7(o &e/claim&7) - &fReclamar chunk actual para la nación\n"
                    + "&e/pais renunciar &7(o &e/unclaim&7) - &fRenunciar al chunk actual\n"
                    + "&e/pais muro &7- &fAdministrar murallas defensivas autorreparables\n"
                    + "&e/pais regimen &7- &fElegir doctrina política nacional (8 doctrinas)\n"
                    + "&e/pais tecnologia &7- &fÁrbol de investigación tecnológica (Nivel I - V)\n"
                    + "&e/pais color <#HEX> &7- &fCambiar color en mapas web (Dynmap, Squaremap, etc.)\n"
                    + "&e/pais territorio &7(o &e/teren&7) - &fInspeccionar el territorio actual\n"
                    + "&e/pais recetas &7(o &e/crafting&7) - &fVer recetas y máquinas personalizadas\n"
                    + "&e/pais crear <nombre> <tag> &7- &fFundar una nueva nación y recibir el Bastión\n"
                    + "&e/pais invitar <jugador> &7- &fInvitar a un jugador a tu nación\n"
                    + "&e/pais unirse <nombre> &7- &fAceptar invitación y unirse a una nación\n"
                    + "&e/pais abandonar &7- &fAbandonar tu nación actual\n"
                    + "&e/pais disolver CONFIRMAR &7- &4Disolver permanentemente&7 tu nación\n"
                    + "&e/pais info [nombre] &7- &fInspeccionar economía, PIB, murallas y ejército\n"
                    + "&e/pais ceca <cobre|oro> <cantidad> &7- &fAcuñar monedas nacionales\n"
                    + "&e/pais top &7- &fAbrir la tabla de posiciones de la isla\n"
                    + "&e/pais guerra <nación> &7- &fDeclarar la guerra a un reino rival\n"
                    + "&e/pais relacion <alianza|pna|neutral|rival> <nación> &7- &fPactos diplomáticos\n"
                    + "&e/pais rendirse &7- &fRendirse en guerra (ceder 25% tierra, 70% tesoro, tributo)\n"
                    + "&e/pais ventana <desde> <hasta> &7- &fConfigurar ventana de asedio de bastión\n"
                    + "&e/tc <msg> &7- &fChat de nación | &e/ac <msg> &7- &fChat de aliados\n"
                    + "&e/pais general &7(o &e/pais tambor&7) - &fHabilidad de marcha del General Paolo\n"
                    + "&e/pais tributo &7- &fPagar tributo a las flotas piratas\n"
                    + "&e/pais basura &7(o &e/trash&7) - &fContenedor de eliminación de residuos\n"
                    + "&e/pais sentarse &7(o &e/sit&7) - &fSentarse en escaleras, sillas o el suelo\n"
                    + "&e/pais autor &7- &fCredenciales oficiales del plugin y desarrollador\n"
                    + "&8-----------------------------------------------------\n"
                    + "&7Sovereignty & Steel • Desarrollado por &6&lfilipGFF1\n"
                    + "&6&l====================================================\n"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l=== Sovereignty & Steel: Island Realm (1.21.4) ===\n"
                    + "&e/panstwo bank &7(lub &e/bank&7) - &fSkarbiec Narodowy Vault, finanse i Upkeep\n"
                    + "&e/panstwo skarbiec [1-5|ulepsz|autosell] &7- &fWielostronicowy Skarbiec Krajowy i Auto-Sell\n"
                    + "&e/panstwo wplac <kwota> &7- &fWpłata ze swojego portfela do Skarbca\n"
                    + "&e/panstwo wyplac <kwota> &7- &fWypłata ze Skarbca (tylko Władca/Oficer)\n"
                    + "&e/panstwo claim &7(lub &e/claim&7) - &fPłatna asymilacja obecnego chunka z cooldownem\n"
                    + "&e/panstwo unclaim &7(lub &e/unclaim&7) - &fZrzeczenie się obecnego chunka\n"
                    + "&e/panstwo mur &7- &fZarządzanie samobudującymi się murami obronnymi\n"
                    + "&e/panstwo ustroj &7- &fWybór ustroju politycznego państwa (8 doktryn)\n"
                    + "&e/panstwo upgrady &7- &fDrzewko technologiczne państwa (Tier I - V)\n"
                    + "&e/panstwo kolor <#HEX> &7- &fZmiana barw na mapach (Squaremap, Dynmap, BlueMap, Pl3xMap)\n"
                    + "&e/panstwo teren &7(lub &e/teren&7) - &fSprawdza aktualne terytorium (państwo / dzicz)\n"
                    + "&e/panstwo receptury &7(lub &e/crafting&7) - &fPokazuje jak craftować wszystkie przedmioty\n"
                    + "&e/panstwo stworz <nazwa> <tag> &7- Zakłada państwo (" + vaultEconomyManager.format(CREATE_NATION_COST) + ") i daje Rdzeń Bastionu\n"
                    + "&e/panstwo zapros <gracz> &7- &fWysyła zaproszenie do państwa (tylko Władca/Oficer)\n"
                    + "&e/panstwo dolacz <nazwa> &7- Dołącza do państwa (wymaga zaproszenia)\n"
                    + "&e/panstwo opusc &7- Opuszcza obecne państwo\n"
                    + "&e/panstwo rozwiaz POTWIERDZ &7- &4Nieodwracalne&7 rozwiązanie państwa (tylko Władca)\n"
                    + "&e/panstwo info [nazwa] &7- Pokazuje stan gospodarki, PKB i wojska\n"
                    + "&e/panstwo mennica <miedz|zloto> <ilosc> &7- Bicie monet\n"
                    + "&e/panstwo top &7- Otwiera ranking wyspy\n"
                    + "&e/panstwo wojna <państwo> &7- Wypowiedzenie wojny\n"
                    + "&e/panstwo relacja <sojusz|nap|neutralny|rywal> <państwo> &7- &fRelacje międzynarodowe\n"
                    + "&e/panstwo kapituluj &7- &fKapitulacja w wojnie (oddanie 25% ziemi, 70% skarbca, trybut)\n"
                    + "&e/panstwo oknowojenne <od> <do> &7- &fUstawia okno oblężnicze bastionu (3-4h)\n"
                    + "&e/tc <wiadomość> &7- &fCzat państwowy | &e/ac <wiadomość> &7- &fCzat sojuszniczy\n"
                    + "&e/panstwo wodz &7(lub &e/panstwo werbel&7) - &fZdolność bojowa wodza (Werbel Wojny / Szarża Gwardii)\n"
                    + "&e/panstwo haracz &7- Opłacenie haraczu najeźdźcom pirackim\n"
                    + "&e/panstwo kosz &7(lub &e/kosz&7) - &fKosz na zbędne przedmioty (utylizacja)\n"
                    + "&e/panstwo sit &7(lub &e/sit&7) - &fSiadanie na ziemi lub schodkach\n"
                    + "&e/panstwo testglod &7- &fDiagnostyka głodu i reset nasycenia do testu\n"
                    + "&e/panstwo author &7- &fInformacje o autorze pluginu\n"
                    + "&8-----------------------------------------------------\n"
                    + "&7Sovereignty & Steel • Autorski plugin stworzony przez &6&lfilipGFF1\n"
                    + "&6&l====================================================\n"));
        }
    }

    private static final List<String> MAIN_SUBCOMMANDS = Arrays.asList(
            "bank", "skarbiec", "vault", "wplac", "deposit", "wyplac", "withdraw", "claim", "unclaim",
            "mur", "wall", "ustroj", "regime", "upgrady", "tech", "kolor", "color",
            "teren", "territory", "receptury", "crafting", "recipes", "kosz", "trash", "sit",
            "stworz", "create", "zapros", "invite", "dolacz", "join", "opusc", "leave", "rozwiaz", "disband",
            "info", "author", "top", "leaderboard", "mennica", "mint", "wojna", "war", "relacja", "relation",
            "kapituluj", "surrender", "oknowojenne", "wodz", "general", "werbel", "szarza",
            "haracz", "tribute", "testglod", "pomoc", "help", "ayuda"
    );

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            org.bukkit.util.StringUtil.copyPartialMatches(args[0], MAIN_SUBCOMMANDS, matches);
            java.util.Collections.sort(matches);
            return matches;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            List<String> candidates = new ArrayList<>();
            if (sub.equals("zapros")) {
                candidates = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (sub.equals("dolacz") || sub.equals("wojna")) {
                candidates = nationManager.getAllNations().stream().map(Nation::getName).toList();
            } else if (sub.equals("relacja") || sub.equals("relacje") || sub.equals("dyplomacja")) {
                candidates = Arrays.asList("sojusz", "nap", "neutralny", "rywal");
            } else if (sub.equals("mennica")) {
                candidates = Arrays.asList("miedz", "zloto");
            } else if (sub.equals("skarbiec")) {
                candidates = Arrays.asList("1", "2", "3", "4", "5", "ulepsz", "menu", "autosell", "sprzedaj");
            } else if (sub.equals("kolor") || sub.equals("color")) {
                candidates = Arrays.asList("#FF5733", "#3388FF", "#2ECC71", "#9B59B6", "#F1C40F");
            } else if (sub.equals("wplac") || sub.equals("wyplac")) {
                candidates = Arrays.asList("100", "500", "1000", "5000");
            } else if (sub.equals("oknowojenne") || sub.equals("okno")) {
                candidates = Arrays.asList("18", "19", "20");
            }
            List<String> matches = new ArrayList<>();
            org.bukkit.util.StringUtil.copyPartialMatches(args[1], candidates, matches);
            java.util.Collections.sort(matches);
            return matches;
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            List<String> candidates = new ArrayList<>();
            if (sub.equals("relacja") || sub.equals("relacje") || sub.equals("dyplomacja")) {
                candidates = nationManager.getAllNations().stream().map(Nation::getName).toList();
            } else if (sub.equals("oknowojenne") || sub.equals("okno")) {
                candidates = Arrays.asList("21", "22", "23");
            }
            List<String> matches = new ArrayList<>();
            org.bukkit.util.StringUtil.copyPartialMatches(args[2], candidates, matches);
            java.util.Collections.sort(matches);
            return matches;
        }
        return new ArrayList<>();
    }
}
