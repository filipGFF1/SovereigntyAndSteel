package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.*;

public class EconomyManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final CustomItemRegistry items;
    private UUID tradeSovereignNationId = null;

    public EconomyManager(Plugin plugin, NationManager nationManager, CustomItemRegistry items) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.items = items;
    }

    /**
     * Kalkulacja Wskaźnika PKB (GDP) co 24 godziny w grze:
     * Algorytm sumuje: stan urobku w skarbcach, produkcję żywności i drewna, wartość aktywnych machin oraz obrót handlowy.
     */
    public void calculateGlobalGdp(int activeMachinesCount) {
        Nation highestGdpNation = null;
        double maxGdp = -1.0;

        for (Nation nation : nationManager.getAllNations()) {
            double gdp = (nation.getTreasury() * 1.0)
                    + (nation.getRawMaterialsStockpile() * 3.0)
                    + (nation.getFoodStockpile() * 1.5)
                    + (nation.getWoodStockpile() * 1.2)
                    + (activeMachinesCount * 30.0)
                    + (nation.getTradeVolume() * 1.0);

            nation.setGdp(Math.round(gdp * 10.0) / 10.0);

            if (gdp > maxGdp) {
                maxGdp = gdp;
                highestGdpNation = nation;
            }
        }

        if (highestGdpNation != null && (tradeSovereignNationId == null || !tradeSovereignNationId.equals(highestGdpNation.getId()))) {
            tradeSovereignNationId = highestGdpNation.getId();
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "\n&6&l[GLOBALNE MOCARSTWO] &ePaństwo &f" + highestGdpNation.getName() + " &e(" + highestGdpNation.getTag() + ") "
                    + "&ezostało nowym &6&lWładcą Handlu Wyspy&e! (PKB: &a" + highestGdpNation.getGdp() + "&e)\n"
                    + "&7Jako dominująca gospodarka, nacja otrzymuje 0.5% podatku tranzytowego ze wszystkich transakcji wyspy!\n"));
        }

        nationManager.saveNations();
    }

    public UUID getTradeSovereignNationId() {
        return tradeSovereignNationId;
    }

    public boolean isTradeSovereign(UUID nationId) {
        return tradeSovereignNationId != null && tradeSovereignNationId.equals(nationId);
    }

    /**
     * Mennica: Bicie monet z miedzi i złota.
     */
    public boolean mintCurrency(Player player, Nation nation, Material metalType, int metalAmount) {
        if (!player.getInventory().containsAtLeast(new ItemStack(metalType), metalAmount)) {
            player.sendMessage(ChatColor.RED + "Nie posiadasz wystarczającej ilości sztabek metalu do przetopienia w mennicy!");
            return false;
        }

        player.getInventory().removeItem(new ItemStack(metalType, metalAmount));

        if (metalType == Material.COPPER_INGOT) {
            // 1 Sztabka Miedzi = 4 Miedziane Grosze
            int mintedCoins = metalAmount * 4;
            player.getInventory().addItem(items.getCopperCoin(mintedCoins));
            nation.addMintedCurrency(mintedCoins);
            player.sendMessage(ChatColor.GREEN + "Wybito " + mintedCoins + " Miedzianych Groszy!");
        } else if (metalType == Material.GOLD_INGOT) {
            // 1 Sztabka Złota = 1 Złoty Dukat (wartość 10 Groszy)
            player.getInventory().addItem(items.getGoldDucat(metalAmount));
            nation.addMintedCurrency(metalAmount * 10.0);
            player.sendMessage(ChatColor.GOLD + "Wybito " + metalAmount + " Złotych Dukatów!");
        }

        // Sprawdź wskaźnik inflacji
        double inflation = nation.getInflationFactor();
        if (inflation > 1.2) {
            int percent = (int) Math.round((inflation - 1.0) * 100);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[OSTRZEŻENIE MENNICY] Nadmierna emisja bez pokrycia w surowcach wywołała &4inflację " + percent + "%&c! "
                    + "Koszty rekrutacji armii i machin wzrosły!"));
        }

        nationManager.saveNations();
        return true;
    }

    /**
     * Podatek tranzytowy od handlu:
     * 0.5% prowizji trafia do Władcy Handlu Wyspy.
     */
    public void processTradeTax(double amount) {
        if (tradeSovereignNationId == null) return;
        Nation sovereign = nationManager.getNation(tradeSovereignNationId);
        if (sovereign != null) {
            double tax = amount * 0.005; // 0.5%
            sovereign.addTreasury(tax);
            sovereign.addTradeVolume(amount);
        }
    }
}
