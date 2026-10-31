package pl.sovereignty.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.sovereignty.manager.*;
import pl.sovereignty.model.Nation;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminSovereigntyCommand implements CommandExecutor, TabCompleter {
    private final NationManager nationManager;
    private final EconomyManager economyManager;
    private final SiegeMachineManager machineManager;
    private final PirateRaidManager pirateRaidManager;
    private final RestorationManager restorationManager;
    private final CustomItemRegistry items;
    private final MapManager mapManager;

    public AdminSovereigntyCommand(NationManager nationManager, EconomyManager economyManager,
                                  SiegeMachineManager machineManager, PirateRaidManager pirateRaidManager,
                                  RestorationManager restorationManager, CustomItemRegistry items,
                                  MapManager mapManager) {
        this.nationManager = nationManager;
        this.economyManager = economyManager;
        this.machineManager = machineManager;
        this.pirateRaidManager = pirateRaidManager;
        this.restorationManager = restorationManager;
        this.items = items;
        this.mapManager = mapManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("sovereignty.admin")) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień administratora!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "daj" -> handleGive(sender, args);
            case "rajd", "raid" -> handleRaid(sender, args);
            case "usun", "disband" -> handleAdminDisband(sender, args);
            case "pkb" -> {
                economyManager.calculateGlobalGdp(machineManager.getAllMachines().size());
                sender.sendMessage(ChatColor.GREEN + "[SovereigntyAdmin] Przeliczono globalne PKB i wyłoniono Władcę Handlu Wyspy!");
            }
            case "odbudowa" -> {
                int count = restorationManager.getPendingRestorationsCount();
                restorationManager.startGradualRestoration();
                sender.sendMessage(ChatColor.GREEN + "[SovereigntyAdmin] Uruchomiono samoczynną rekonstrukcję terenu (" + count + " bloków w kolejce)!");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /sa daj <przedmiot> [gracz]");
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Gracz nie jest online!");
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ChatColor.RED + "Musisz podać cel z konsoli!");
            return;
        }

        String itemName = args[1].toLowerCase();
        ItemStack is = switch (itemName) {
            case "bastion" -> items.getBastionCore();
            case "straznica" -> items.getWatchtower();
            case "baraki" -> items.getBarracksCore();
            case "tartak" -> items.getSawmillTotem();
            case "kopalnia" -> items.getMiningHead();
            case "farma" -> items.getStateFarm();
            case "skarbiec", "vault" -> items.getNationalVault();
            case "stol" -> items.getWarCouncilTable();
            case "taran" -> items.getCrateRam();
            case "balista" -> items.getCrateBallista();
            case "mozdzierz" -> items.getCrateMortar();
            case "klucz" -> items.getEngineeringWrench();
            case "bulawa" -> items.getMarshalMace();
            case "rog" -> items.getWarHorn();
            case "moneta_miedz" -> items.getCopperCoin(16);
            case "moneta_zloto" -> items.getGoldDucat(8);
            case "sztandar" -> items.getBrokenRhythmStandard();
            case "skrzynia_lupu" -> items.getForeignLootChest();
            default -> null;
        };

        if (is == null) {
            sender.sendMessage(ChatColor.RED + "Nieznany identyfikator przedmiotu: " + itemName);
            return;
        }

        target.getInventory().addItem(is);
        sender.sendMessage(ChatColor.GREEN + "Przekazano przedmiot " + itemName + " graczowi " + target.getName() + "!");
    }

    private void handleRaid(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /sa rajd <nazwa_panstwa> [mega]");
            return;
        }

        Nation nation = nationManager.getNationByName(args[1]);
        if (nation == null) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono państwa o nazwie: " + args[1]);
            return;
        }

        boolean isMega = (args.length >= 3 && args[2].equalsIgnoreCase("mega"));

        Location landingLoc = pirateRaidManager.findRaidLandingLocation(nation);
        if (landingLoc == null) {
            for (java.util.UUID mUuid : nation.getMembers().keySet()) {
                Player p = Bukkit.getPlayer(mUuid);
                if (p != null && p.isOnline()) {
                    landingLoc = p.getLocation();
                    break;
                }
            }
        }

        if (landingLoc == null) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono odpowiedniej lokalizacji desantu ani graczy online w państwie " + nation.getName() + "!");
            return;
        }

        pirateRaidManager.triggerPirateRaid(nation, landingLoc, isMega);
        sender.sendMessage(ChatColor.GREEN + "Pomyślnie uruchomiono " + (isMega ? ChatColor.RED + "MEGA OP RAID" : "desant piracki") + ChatColor.GREEN + " na państwo " + nation.getName() + "!");
    }

    private void handleAdminDisband(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /sa usun <nazwa_panstwa>");
            return;
        }
        Nation nation = nationManager.getNationByName(args[1]);
        if (nation == null) {
            sender.sendMessage(ChatColor.RED + "[SovereigntyAdmin] Nie znaleziono państwa o nazwie: " + args[1]);
            return;
        }
        String nationName = nation.getName();
        if (mapManager != null) {
            mapManager.removeNation(nation);
        }
        nationManager.disbandNation(nation);
        org.bukkit.Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&4&l[ADMINISTRACJA] &cPaństwo &f" + nationName
                + " &czostało przymusowo rozwiązane przez administratora serwera.\n"));
        sender.sendMessage(ChatColor.GREEN + "[SovereigntyAdmin] Pomyślnie usunięto państwo: " + nationName);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&c&l=== Panel Administratora Sovereignty & Steel ===\n"
                + "&e/sa daj <przedmiot> [gracz] &7- Daje customowy komponent/machinę\n"
                + "&e/sa rajd <państwo> [mega] &7- Wymusza natychmiastowy desant piratów (standard lub mega op)\n"
                + "&e/sa usun <państwo> &7- Przymusowe rozwiązanie państwa przez admina\n"
                + "&e/sa pkb &7- Wymusza przeliczenie globalnego PKB i wyłonienie Mocarstwa\n"
                + "&e/sa odbudowa &7- Ręczne uruchomienie samoczynnej regeneracji zniszczeń wojennych\n"
                + "&c&l==============================================\n"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("daj", "rajd", "usun", "pkb", "odbudowa", "pomoc");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("daj")) {
            return Arrays.asList("bastion", "straznica", "baraki", "tartak", "kopalnia", "farma", "skarbiec", "stol", "taran", "balista", "mozdzierz", "klucz", "bulawa", "rog", "moneta_miedz", "moneta_zloto", "sztandar", "skrzynia_lupu");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("rajd") || args[0].equalsIgnoreCase("raid") || args[0].equalsIgnoreCase("usun"))) {
            return nationManager.getAllNations().stream().map(Nation::getName).toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("rajd") || args[0].equalsIgnoreCase("raid"))) {
            return Arrays.asList("standard", "mega");
        }
        return new ArrayList<>();
    }
}
