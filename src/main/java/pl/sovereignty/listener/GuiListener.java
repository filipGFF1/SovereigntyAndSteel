package pl.sovereignty.listener;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.sovereignty.gui.*;
import pl.sovereignty.manager.*;
import pl.sovereignty.model.*;
import pl.sovereignty.util.CustomItemRegistry;

public class GuiListener implements Listener {
    private final NationManager nationManager;
    private final MilitaryManager militaryManager;
    private final PaoloManager paoloManager;
    private final CustomItemRegistry items;
    private final WallManager wallManager;
    private final VaultEconomyManager economyManager;
    private final MapManager mapManager;
    private final VaultManager vaultManager;

    public GuiListener(NationManager nationManager, MilitaryManager militaryManager, PaoloManager paoloManager,
                       CustomItemRegistry items, WallManager wallManager, VaultEconomyManager economyManager,
                       MapManager mapManager, VaultManager vaultManager) {
        this.nationManager = nationManager;
        this.militaryManager = militaryManager;
        this.paoloManager = paoloManager;
        this.items = items;
        this.wallManager = wallManager;
        this.economyManager = economyManager;
        this.mapManager = mapManager;
        this.vaultManager = vaultManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 1. Spis Receptur: Menu główne
        if (title.equals(RecipeBookGui.MAIN_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            String recipeId = switch (slot) {
                case 10 -> "barracks_core";
                case 11 -> "sawmill_totem";
                case 12 -> "mining_head";
                case 13 -> "state_farm";
                case 14 -> "war_council_table";
                case 15 -> "crate_ram";
                case 16 -> "crate_ballista";
                case 19 -> "crate_mortar";
                case 20 -> "engineering_wrench";
                case 21 -> "national_vault";
                case 22 -> "marshal_mace";
                case 23, 24 -> "war_horn";
                default -> null;
            };

            if (recipeId != null) {
                RecipeBookGui.openRecipeDetail(player, recipeId, items);
            }
            return;
        }

        // 2. Podgląd siatki craftingu 3x3 danej receptury
        if (title.startsWith(RecipeBookGui.DETAIL_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 40) { // Powrót
                RecipeBookGui.openMainMenu(player, items);
            }
            return;
        }

        // 3. Top Leaderboard
        if (title.equals(TopLeaderboardGui.TITLE)) {
            event.setCancelled(true);
            return;
        }

        // 4. Koszary
        if (title.equals(BarracksGui.TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) return;

            if (slot == 11) {
                militaryManager.recruitUnit(player, nation, MilitaryUnitType.SHIELDBEARER, player.getLocation());
                player.closeInventory();
            } else if (slot == 13) {
                militaryManager.recruitUnit(player, nation, MilitaryUnitType.CROSSBOWMAN, player.getLocation());
                player.closeInventory();
            } else if (slot == 15) {
                militaryManager.recruitUnit(player, nation, MilitaryUnitType.SAPPER, player.getLocation());
                player.closeInventory();
            }
            return;
        }

        // 5. Stół Narad Wojennych
        if (title.equals(WarCouncilGui.TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            if (slot == 11) {
                player.closeInventory();
                paoloManager.summonPaolo(player, nation, player.getLocation());
            } else if (slot == 15) {
                player.closeInventory();
                paoloManager.summonNapoleon(player, nation, player.getLocation());
            }
            return;
        }

        // 6. Menu Ustroju Politycznego
        if (title.equals(PoliticalRegimeGui.TITLE)) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            NationRole role = nation.getRole(player.getUniqueId());
            if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zmienić ustrój państwa!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            int slot = event.getRawSlot();
            PoliticalRegime selected = PoliticalRegimeGui.getRegimeFromSlot(slot);

            if (selected == null) return;
            if (nation.getRegime() == selected) {
                player.sendMessage(ChatColor.YELLOW + "To jest już obecny ustrój Twojego państwa!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            double cost = (nation.getRegime() == PoliticalRegime.HEREDITARY_MONARCHY) ? PoliticalRegime.HEREDITARY_MONARCHY.getTransitionCost() : selected.getTransitionCost();
            if (nation.getBankBalance() < cost) {
                player.sendMessage(ChatColor.RED + "Brak wystarczających środków w Skarbcu Narodowym!");
                player.sendMessage(ChatColor.GRAY + "Wymagane: " + ChatColor.GOLD + economyManager.format(cost)
                        + ChatColor.GRAY + ", Stan: " + ChatColor.YELLOW + economyManager.format(nation.getBankBalance()));
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            nation.withdrawBank(cost);
            nation.setRegime(selected);
            nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                    "&6&l[DEKRET PAŃSTWOWY] &eWładca ogłosił nowy ustrój: &f&l" + selected.getDisplayName()
                            + "&e! (Pobrano ze skarbca: &6" + economyManager.format(cost) + "&e)"));
            nationManager.saveNations();
            if (mapManager != null) mapManager.updateNation(nation);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            PoliticalRegimeGui.open(player, nation, economyManager);
            return;
        }

        // 7. Drzewko Technologiczne
        if (title.equals(TechnologyUpgradeGui.TITLE)) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            NationRole role = nation.getRole(player.getUniqueId());
            if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zarządzać rozwojem technologicznym!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            int slot = event.getRawSlot();
            int targetLevel = slot - 10;
            if (targetLevel < 1 || targetLevel > 5) return;

            TechnologyTier current = nation.getTechTier();
            if (targetLevel <= current.getLevel()) {
                player.sendMessage(ChatColor.GREEN + "Ten poziom technologii został już zbadany i odblokowany.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                return;
            }

            TechnologyTier next = current.getNext();
            if (next == null || targetLevel != next.getLevel()) {
                player.sendMessage(ChatColor.RED + "Musisz najpierw opracować poprzedni poziom technologii!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            double cost = TechnologyUpgradeGui.getDiscountedCost(nation, next);
            if (nation.getBankBalance() < cost) {
                player.sendMessage(ChatColor.RED + "Brak wystarczających funduszy w Skarbcu Narodowym!");
                player.sendMessage(ChatColor.GRAY + "Wymagane: " + ChatColor.GOLD + economyManager.format(cost)
                        + ChatColor.GRAY + ", Stan: " + ChatColor.YELLOW + economyManager.format(nation.getBankBalance()));
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            nation.withdrawBank(cost);
            nation.setTechTier(next);
            nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                    "&b&l[POSTĘP NAUKOWY] &ePaństwo dokonało przełomu cywilizacyjnego: &f&l" + next.getDisplayName()
                            + "&e! (Inwestycja: &6" + economyManager.format(cost) + "&e)"));
            nationManager.saveNations();
            if (mapManager != null) mapManager.updateNation(nation);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            TechnologyUpgradeGui.open(player, nation, economyManager);
            return;
        }

        // 8. Zarządzanie Murami Obronnymi
        if (title.equals(WallMenuGui.TITLE)) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            NationRole role = nation.getRole(player.getUniqueId());
            if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zarządzać murami obronnymi!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            int slot = event.getRawSlot();
            WallTier chosenTier = switch (slot) {
                case 11 -> WallTier.TIER_1_WOODEN_PALISADE;
                case 13 -> WallTier.TIER_2_STONE_WALL;
                case 15 -> WallTier.TIER_3_COPPER_DEEPSLATE;
                case 22 -> WallTier.NONE;
                default -> null;
            };

            if (chosenTier == null) return;

            if (chosenTier == nation.getWallTier() && chosenTier != WallTier.NONE) {
                player.sendMessage(ChatColor.YELLOW + "Ten typ fortyfikacji obronnych jest już wzniesiony na granicach.");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            if (wallManager.buildOrUpgradeWall(player, nation, chosenTier)) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.9f, 1.0f);
                WallMenuGui.open(player, nation, wallManager, economyManager);
            }
            return;
        }

        // 9. Wybór Skrzyń Skarbca Krajowego
        if (title.equals(VaultSelectorGui.TITLE)) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            int slot = event.getRawSlot();
            // Kliknięcie w skrzynie 1..5 (sloty 11, 12, 13, 14, 15)
            if (slot >= 11 && slot <= 15) {
                int pageIndex = slot - 11;
                int unlocked = nation.getVaultTier().getPages();
                if (pageIndex < unlocked) {
                    vaultManager.openVault(player, nation, pageIndex);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Ta skrzynia skarbca jest zablokowana! Wykup ulepszenie w menu rozbudowy.");
                    VaultUpgradeGui.open(player, nation, economyManager);
                }
                return;
            }

            // Przejście do menu ulepszeń (slot 22)
            if (slot == 22) {
                VaultUpgradeGui.open(player, nation, economyManager);
                return;
            }

            // Opcje poziomu Tier 5 (Auto-Sell)
            if (nation.getVaultTier().hasAutoSell()) {
                // Przełącznik Auto-Sell (slot 24)
                if (slot == 24) {
                    NationRole role = nation.getRole(player.getUniqueId());
                    if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                        player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może zarządzać modułem Auto-Sell!");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                        return;
                    }

                    boolean newState = !nation.isVaultAutoSell();
                    nation.setVaultAutoSell(newState);
                    nationManager.saveNations();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9f, 1.2f);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6&l[AUTO-SELL] &fZmieniono stan modułu Auto-Sell na: " + (newState ? "&aWŁĄCZONY" : "&cWYŁĄCZONY")));
                    VaultSelectorGui.open(player, nation, vaultManager);
                    return;
                }

                // Natychmiastowa sprzedaż wszystkich skrzyń skarbca (slot 20)
                if (slot == 20) {
                    NationRole role = nation.getRole(player.getUniqueId());
                    if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                        player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może spieniężać surowce państwowe!");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                        return;
                    }

                    int totalPages = nation.getVaultTier().getPages();
                    double earned = vaultManager.sellAllVaultContents(nation);
                    if (earned > 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                        nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                                "&6&l[AUTO-SELL] &fSpieniężono surowce ze skrzyń skarbca (#1 - #" + totalPages + ") za łączną kwotę: &a+"
                                        + economyManager.format(earned) + " &7(zdeponowano w Skarbcu Narodowym)!"));
                        vaultManager.saveVaults();
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Skrzynie skarbca są puste lub nie zawierają surowców do skupu.");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                    }
                    VaultSelectorGui.open(player, nation, vaultManager);
                    return;
                }
            }
            return;
        }

        // 10. Ulepszenia Pojemności Skarbca Krajowego
        if (title.equals(VaultUpgradeGui.TITLE)) {
            event.setCancelled(true);
            Nation nation = nationManager.getNationByPlayer(player.getUniqueId());
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Musisz należeć do państwa!");
                return;
            }

            int slot = event.getRawSlot();
            if (slot == 22) { // Powrót
                VaultSelectorGui.open(player, nation, vaultManager);
                return;
            }

            int targetLevel = slot - 10;
            if (targetLevel < 1 || targetLevel > 5) return;

            NationRole role = nation.getRole(player.getUniqueId());
            if (role != NationRole.LEADER && role != NationRole.OFFICER) {
                player.sendMessage(ChatColor.RED + "Tylko Władca lub Oficer może finansować rozbudowę Skarbca Krajowego!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            VaultTier current = nation.getVaultTier();
            if (targetLevel <= current.getLevel()) {
                player.sendMessage(ChatColor.GREEN + "Ten poziom pojemności skarbca został już wykupiony.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                return;
            }

            VaultTier next = current.getNext();
            if (next == null || targetLevel != next.getLevel()) {
                player.sendMessage(ChatColor.RED + "Musisz najpierw rozbudować poprzedni poziom skarbca!");
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            double cost = VaultUpgradeGui.getDiscountedCost(nation, next);
            if (nation.getBankBalance() < cost) {
                player.sendMessage(ChatColor.RED + "Brak wystarczających funduszy w Skarbcu Narodowym!");
                player.sendMessage(ChatColor.GRAY + "Wymagane: " + ChatColor.GOLD + economyManager.format(cost)
                        + ChatColor.GRAY + ", Stan konta: " + ChatColor.YELLOW + economyManager.format(nation.getBankBalance()));
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.7f, 1.2f);
                return;
            }

            nation.withdrawBank(cost);
            nation.setVaultTier(next);
            nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                    "&6&l[ROZBUDOWA SKARBCA] &eWładca sfinansował powiększenie Skarbca Krajowego do: &f&l"
                            + next.getDisplayName() + "&e! Nowa pojemność: &6" + (next.getPages() * 54)
                            + " slotów &8(" + next.getPages() + " skrzyń)&e! (Koszt: &6" + economyManager.format(cost) + "&e)"));
            if (next.hasAutoSell()) {
                nationManager.broadcastToNation(nation, ChatColor.translateAlternateColorCodes('&',
                        "&a⭐ ODBLOKOWANO MODUŁ AUTO-SELL! &eNadmiar surowców ze skarbca będzie automatycznie spieniężany!"));
            }

            nationManager.saveNations();
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            VaultUpgradeGui.open(player, nation, economyManager);
            return;
        }
    }
}
