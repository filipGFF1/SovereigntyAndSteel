package pl.sovereignty.manager;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.sovereignty.model.Nation;

public class VaultEconomyManager {
    private final Plugin plugin;
    private Economy economy;

    public VaultEconomyManager(Plugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Brak pluginu Vault na serwerze! Finanse będą symulowane.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
            plugin.getLogger().info("Pomyślnie zintegrowano z silnikiem ekonomii Vault (" + economy.getName() + ")!");
            return true;
        }

        plugin.getLogger().warning("Vault został wykryty, ale brak zarejestrowanego providera ekonomii (np. EssentialsX).");
        return false;
    }

    public boolean isHooked() {
        return economy != null;
    }

    public String format(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("%.2f $", amount);
    }

    public double getPlayerBalance(Player player) {
        if (economy != null && player != null) {
            return economy.getBalance(player);
        }
        return 10000.0; // fallback dla testów
    }

    public boolean hasPlayerBalance(Player player, double amount) {
        if (amount <= 0) return true;
        if (economy != null && player != null) {
            return economy.has(player, amount);
        }
        return true;
    }

    public boolean chargePlayer(Player player, double amount) {
        if (amount <= 0) return true;
        if (economy != null && player != null) {
            if (!economy.has(player, amount)) {
                return false;
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        }
        return true;
    }

    public boolean depositPlayer(Player player, double amount) {
        if (amount <= 0) return true;
        if (economy != null && player != null) {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        }
        return true;
    }

    /**
     * Wpłata środków z osobistego portfela gracza do Skarbca Narodowego państwa (/panstwo wplac <kwota>)
     */
    public boolean depositToNationBank(Nation nation, Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Kwota wpłaty musi być większa od zera!");
            return false;
        }

        if (economy != null) {
            if (!economy.has(player, amount)) {
                player.sendMessage(ChatColor.RED + "Nie posiadasz wystarczających środków w portfelu! Posiadasz: "
                        + ChatColor.YELLOW + format(economy.getBalance(player)));
                return false;
            }

            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Błąd transakcji Vault: " + response.errorMessage);
                return false;
            }
        }

        nation.depositBank(amount);
        player.sendMessage(ChatColor.GREEN + "Wpłacono " + ChatColor.GOLD + format(amount)
                + ChatColor.GREEN + " do Skarbca Narodowego państwa " + nation.getName() + "!");
        player.sendMessage(ChatColor.GRAY + "Aktualny stan konta państwa: " + ChatColor.YELLOW + format(nation.getBankBalance()));
        return true;
    }

    /**
     * Wypłata środków ze Skarbca Narodowego na osobiste konto gracza (/panstwo wyplac <kwota>)
     * Dostępna dla Władcy (Leader) i Ministrów/Oficerów
     */
    public boolean withdrawFromNationBank(Nation nation, Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Kwota wypłaty musi być większa od zera!");
            return false;
        }

        if (nation.getBankBalance() < amount) {
            player.sendMessage(ChatColor.RED + "W Skarbcu Narodowym brak wystarczających funduszy! Stan: "
                    + ChatColor.YELLOW + format(nation.getBankBalance()));
            return false;
        }

        if (economy != null) {
            EconomyResponse response = economy.depositPlayer(player, amount);
            if (!response.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Błąd transakcji Vault: " + response.errorMessage);
                return false;
            }
        }

        nation.withdrawBank(amount);
        player.sendMessage(ChatColor.GREEN + "Pobrano " + ChatColor.GOLD + format(amount)
                + ChatColor.GREEN + " ze Skarbca Narodowego do Twojego portfela!");
        player.sendMessage(ChatColor.GRAY + "Pozostały stan konta państwa: " + ChatColor.YELLOW + format(nation.getBankBalance()));
        return true;
    }

    /**
     * Sprawdzenie i bezpośrednie pobranie funduszy ze Skarbca Narodowego (np. za claim, mur, technologie)
     */
    public boolean chargeNationBank(Nation nation, double amount) {
        if (nation.getBankBalance() >= amount) {
            nation.withdrawBank(amount);
            return true;
        }
        return false;
    }
}
