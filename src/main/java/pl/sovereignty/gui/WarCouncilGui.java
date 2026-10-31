package pl.sovereignty.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sovereignty.manager.PaoloManager;
import pl.sovereignty.model.GeneralType;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.PaoloGeneral;

import java.util.Arrays;

public class WarCouncilGui {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Stół Narad Wojennych";

    public static void open(Player player, Nation nation, PaoloManager paoloManager) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        PaoloGeneral general = paoloManager.getGeneral(nation.getId());

        // 1. Slot 11: Generał Paolo Or'e
        if (general != null && general.getType() == GeneralType.PAOLO_ORE) {
            ItemStack active = new ItemStack(Material.MACE);
            ItemMeta aMeta = active.getItemMeta();
            if (aMeta != null) {
                aMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Generał Paolo Or'e jest Aktywny!");
                aMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Twój potężny wódz żelaza dowodzi armią na polu bitwy.",
                        ChatColor.GRAY + "Rola: Ciężka defensywa, rozbijanie tarcz, Ostatni Szaniec.",
                        " ",
                        ChatColor.RED + "Pamiętaj o zasadzie Permadeath!",
                        ChatColor.YELLOW + "Użyj Buławy Marszałkowskiej, aby kierować uderzeniami."
                ));
                active.setItemMeta(aMeta);
            }
            inv.setItem(11, active);
        } else if (general != null) {
            ItemStack busy = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = busy.getItemMeta();
            if (bMeta != null) {
                bMeta.setDisplayName(ChatColor.RED + "Generał Paolo Or'e (Niedostępny)");
                bMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Twoim obecnym wodzem jest: " + general.getType().getDisplayName(),
                        ChatColor.GRAY + "Państwo może mieć tylko jednego aktywnego generała."
                ));
                busy.setItemMeta(bMeta);
            }
            inv.setItem(11, busy);
        } else {
            ItemStack summon = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta sMeta = summon.getItemMeta();
            if (sMeta != null) {
                sMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Kontrakt: Generał Paolo Or'e");
                sMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Wódz Młota, Tarczy i Żelaznego Przełamania.",
                        ChatColor.DARK_AQUA + "Atrybuty: " + ChatColor.WHITE + "150 HP, Ciężki Młot, Ostatni Szaniec (60% redukcji)",
                        " ",
                        ChatColor.YELLOW + "Wymagane zasoby w ekwipunku:",
                        ChatColor.WHITE + "- 16 Sztabek Żelaza",
                        ChatColor.WHITE + "- 8 Sztabek Złota",
                        ChatColor.WHITE + "- 1 Dzwon (Bell)",
                        ChatColor.WHITE + "- 32 Chleby",
                        " ",
                        ChatColor.RED + "Zasada Permadeath: śmierć wodza jest definitywna!",
                        ChatColor.GREEN + "Kliknij LPM, aby podpisać kontrakt i wezwać Paolo!"
                ));
                summon.setItemMeta(sMeta);
            }
            inv.setItem(11, summon);
        }

        // 2. Slot 13: Informacje o Sztabie Generalnym
        ItemStack info = new ItemStack(Material.SHIELD);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Sztab Generalny Nacji");
            iMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Wybierz legendarnego dowódcę dla swojego państwa:",
                    ChatColor.WHITE + "• " + ChatColor.GOLD + "Paolo Or'e" + ChatColor.GRAY + " - twarda defensywa i walka w zwarciu",
                    ChatColor.WHITE + "• " + ChatColor.YELLOW + "Kłot Napoleon Ciepły" + ChatColor.GRAY + " - manewr, Siła I i artyleria",
                    " ",
                    general != null
                            ? ChatColor.GREEN + "Aktywny wódz: " + general.getType().getDisplayName()
                            : ChatColor.YELLOW + "Brak aktywnego wodza polowego."
            ));
            info.setItemMeta(iMeta);
        }
        inv.setItem(13, info);

        // 3. Slot 15: Generał Kłot Napoleon Ciepły
        if (general != null && general.getType() == GeneralType.KLOT_NAPOLEON) {
            ItemStack active = new ItemStack(Material.GOLDEN_SWORD);
            ItemMeta aMeta = active.getItemMeta();
            if (aMeta != null) {
                aMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Generał Kłot Napoleon Ciepły jest Aktywny!");
                aMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Cesarz taktycznych manewrów dowodzi Wielką Armią!",
                        ChatColor.GRAY + "Rola: Błyskawiczny marsz, Siła I, salwy artyleryjskie, ciepło.",
                        " ",
                        ChatColor.RED + "Pamiętaj o zasadzie Permadeath!",
                        ChatColor.YELLOW + "Użyj Buławy Marszałkowskiej, aby wskazywać cele artylerii."
                ));
                active.setItemMeta(aMeta);
            }
            inv.setItem(15, active);
        } else if (general != null) {
            ItemStack busy = new ItemStack(Material.BARRIER);
            ItemMeta bMeta = busy.getItemMeta();
            if (bMeta != null) {
                bMeta.setDisplayName(ChatColor.RED + "Generał Kłot Napoleon Ciepły (Niedostępny)");
                bMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Twoim obecnym wodzem jest: " + general.getType().getDisplayName(),
                        ChatColor.GRAY + "Państwo może mieć tylko jednego aktywnego generała."
                ));
                busy.setItemMeta(bMeta);
            }
            inv.setItem(15, busy);
        } else {
            ItemStack summon = new ItemStack(Material.SPYGLASS);
            ItemMeta sMeta = summon.getItemMeta();
            if (sMeta != null) {
                sMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Kontrakt: Generał Kłot Napoleon Ciepły");
                sMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Ciepły Taktyk, Błyskawiczny Manewr i Ostrzał Artyleryjski.",
                        ChatColor.DARK_AQUA + "Atrybuty: " + ChatColor.WHITE + "120 HP, Prędkość II, Siła I, Płonąca Szabla",
                        ChatColor.GOLD + "Aura Ciepła: " + ChatColor.WHITE + "+Prędkość I dla armii i odporność na mróz",
                        ChatColor.RED + "Wielka Bateria: " + ChatColor.WHITE + "Wybuchowa salwa artyleryjska co 5 ciosów",
                        ChatColor.YELLOW + "Manewr Austerlitz: " + ChatColor.WHITE + "Odwrót z Prędkością III i Regeneracją przy <35% HP",
                        " ",
                        ChatColor.YELLOW + "Wymagane zasoby w ekwipunku:",
                        ChatColor.WHITE + "- 24 Sztabki Złota",
                        ChatColor.WHITE + "- 16 Prochu (Gunpowder)",
                        ChatColor.WHITE + "- 1 Luneta (Spyglass)",
                        ChatColor.WHITE + "- 32 Chleby",
                        " ",
                        ChatColor.RED + "Zasada Permadeath: śmierć wodza jest definitywna!",
                        ChatColor.GREEN + "Kliknij LPM, aby podpisać kontrakt i wezwać Napoleona!"
                ));
                summon.setItemMeta(sMeta);
            }
            inv.setItem(15, summon);
        }

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName(" ");
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }
}
