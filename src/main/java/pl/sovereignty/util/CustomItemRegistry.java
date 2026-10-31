package pl.sovereignty.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class CustomItemRegistry {
    private final Plugin plugin;

    public final NamespacedKey KEY_CUSTOM_ITEM;
    public final NamespacedKey KEY_STRUCTURE_TYPE;
    public final NamespacedKey KEY_MACHINE_TYPE;

    public CustomItemRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.KEY_CUSTOM_ITEM = new NamespacedKey(plugin, "custom_item_id");
        this.KEY_STRUCTURE_TYPE = new NamespacedKey(plugin, "structure_type");
        this.KEY_MACHINE_TYPE = new NamespacedKey(plugin, "machine_type");
    }

    private ItemStack createItem(Material material, String name, String itemId, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> formattedLore = lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList();
            meta.setLore(formattedLore);
            meta.getPersistentDataContainer().set(KEY_CUSTOM_ITEM, PersistentDataType.STRING, itemId);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getBastionCore() {
        ItemStack item = createItem(Material.CRYING_OBSIDIAN,
                "&5&lRdzeń Bastionu",
                "bastion_core",
                Arrays.asList(
                        "&7Serce każdego państwa na wyspie.",
                        "&7Wyznacza początkową strefę wpływów nacji.",
                        "&8Zasięg: 65 kratek wokół rdzenia",
                        "&6Postaw na ziemi, aby założyć stolicę!"
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "BASTION_CORE");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getWatchtower() {
        ItemStack item = createItem(Material.STONE_BRICKS,
                "&b&lStrażnica Nadbrzeżna i Graniczna",
                "watchtower",
                Arrays.asList(
                        "&7Fizyczna wieża rozszerzająca granice państwa.",
                        "&7Kontroluje plaże i szlaki morskie wyspy.",
                        "&8Zasięg: +35 kratek do terytorium",
                        "&ePobiera cło z obcych łodzi na wodach państwa!"
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "WATCHTOWER");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getBarracksCore() {
        ItemStack item = createItem(Material.WHITE_WOOL,
                "&c&lRdzeń Baraków",
                "barracks_core",
                Arrays.asList(
                        "&7Stawia namiot wojskowy i koszary.",
                        "&7Zwiększa limit armii o &a+6 NPC&7.",
                        "&eKliknij PPM na postawiony blok, aby rekrutować wojsko!",
                        "&8Wymaga połączenia Traktem Zaopatrzeniowym."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "BARRACKS");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getSawmillTotem() {
        ItemStack item = createItem(Material.STRIPPED_OAK_WOOD,
                "&6&lTotem Tartaku (Odnawialny Las)",
                "sawmill_totem",
                Arrays.asList(
                        "&7Ścina wyznaczone pnie oraz bambusy w promieniu totemu,",
                        "&7sadząc nowe sadzonki w ich miejscu.",
                        "&7Przesyła drewno i bambus prosto do Skarbca Krajowego.",
                        "&cRyzyko 5% wypadku: uzyskanie spalonego węgla!",
                        "&eMaksymalny limit: &c10 sztuk na państwo.",
                        "&8Działa w pełni autonomicznie prosto do Skarbca."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "SAWMILL_TOTEM");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getMiningHead() {
        ItemStack item = createItem(Material.BLAST_FURNACE,
                "&8&lGłowica Szybu Górniczego",
                "mining_head",
                Arrays.asList(
                        "&7Montowana w jaskini lub podziemiach (Y <= 50).",
                        "&7Powoli generuje urobek (kamień, węgiel, żelazo, złoto, diamenty)",
                        "&7prosto do leja, zużywając kilofy i pochodnie.",
                        "&eMaksymalny limit: &c10 sztuk na państwo.",
                        "&8Wymaga połączenia Traktem Zaopatrzeniowym."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "MINING_SHAFT");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getStateFarm() {
        ItemStack item = createItem(Material.HAY_BLOCK,
                "&e&lCentrum Rolnicze (Farma Państwowa)",
                "state_farm",
                Arrays.asList(
                        "&a+25 punktów Wskaźnika Dobrobytu za każdą wybudowaną farmę!",
                        "&7Autonomicznie generuje żywność i plony do Skarbca Krajowego",
                        "&7bez konieczności manualnego obsiewania grządek.",
                        "&eMaksymalny limit: &c10 sztuk na państwo.",
                        "&8Wymaga połączenia Traktem Zaopatrzeniowym."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "STATE_FARM");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getWarCouncilTable() {
        ItemStack item = createItem(Material.CRAFTING_TABLE,
                "&4&lStół Narad Wojennych",
                "war_council_table",
                Arrays.asList(
                        "&7Pozwala powołać legendarnego wodza: &6Generała Paolo Or'e&7.",
                        "&7Kontrakt: &f16 Żelaza, 8 Złota, 1 Dzwon, 32 Chleby&7.",
                        "&cObowiązuje bezwzględna zasada Permadeath!"
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "WAR_COUNCIL");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getNationalVault() {
        ItemStack item = createItem(Material.BARREL,
                "&6&lSkarbiec Krajowy",
                "national_vault",
                Arrays.asList(
                        "&7Centralny magazyn dóbr narodowych.",
                        "&7Gromadzi automatycznie plony z farm,",
                        "&7drewno z tartaków oraz urobek z kopalni!",
                        "&eKliknij PPM na postawiony blok, aby otworzyć zasoby państwa.",
                        "&8Pojemność: 54 sloty (Duża Skrzynia)."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_STRUCTURE_TYPE, PersistentDataType.STRING, "NATIONAL_VAULT");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getCrateRam() {
        ItemStack item = createItem(Material.BARREL,
                "&c&lSkrzynia: Taran",
                "crate_ram",
                Arrays.asList(
                        "&7Kliknij PPM w ziemię, aby postawić machinę.",
                        "&fMobilny taran na kołach; wyważa drewniane i żelazne wrota.",
                        "&eUderz 3x Kluczem Inżynierskim, aby zwinąć do skrzyni."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_MACHINE_TYPE, PersistentDataType.STRING, "BATTERING_RAM");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getCrateBallista() {
        ItemStack item = createItem(Material.BARREL,
                "&6&lSkrzynia: Balista",
                "crate_ballista",
                Arrays.asList(
                        "&7Kliknij PPM w ziemię, aby postawić machinę.",
                        "&fDziało obronne na mury; bełty przebijają do 3 wrogów w linii.",
                        "&eUderz 3x Kluczem Inżynierskim, aby zwinąć do skrzyni."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_MACHINE_TYPE, PersistentDataType.STRING, "BALLISTA");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getCrateMortar() {
        ItemStack item = createItem(Material.BARREL,
                "&b&lSkrzynia: Moździerz",
                "crate_mortar",
                Arrays.asList(
                        "&7Kliknij PPM w ziemię, aby postawić machinę.",
                        "&fArtyleria stromotorowa miotająca kule z bruku i prochu.",
                        "&eUderz 3x Kluczem Inżynierskim, aby zwinąć do skrzyni."
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_MACHINE_TYPE, PersistentDataType.STRING, "MORTAR");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getEngineeringWrench() {
        return createItem(Material.SHEARS,
                "&e&lKlucz Inżynierski",
                "engineering_wrench",
                Arrays.asList(
                        "&7Narzędzie mistrzów polowych.",
                        "&fTrzykrotne uderzenie w machinę oblężniczą",
                        "&fzwija ją z powrotem do skrzyni montażowej."
                ));
    }

    public ItemStack getMarshalMace() {
        return createItem(Material.MACE,
                "&6&lBuława Marszałkowska",
                "marshal_mace",
                Arrays.asList(
                        "&7Insygnium dowódcy polowego armii.",
                        "&eLPM / PPM w grunt: &fwyznacza cel marszu oddziałów (do 65 kratek).",
                        "&eShift + LPM: &fnatychmiastowa zbiórka wojska przy Tobie.",
                        "&ePPM / Cios we wroga: &fskupienie ognia i szarża wojsk na cel."
                ));
    }

    public ItemStack getWarHorn() {
        return createItem(Material.GOAT_HORN,
                "&c&lRóg Bojowy",
                "war_horn",
                Arrays.asList(
                        "&7Instrument wojenny do rozkazów taktycznych.",
                        "&ePPM: &fBłyskawiczna zmiana szyku wojsk:",
                        "&7- &bMur Tarcz &8(obrona żółwia, redukcja obrażeń)",
                        "&7- &cSzarża &8(+20% prędkości poruszania się)",
                        "&7- &eOdwrót do Fortu &8(odwrót do bastionu)"
                ));
    }

    public ItemStack getCopperCoin(int amount) {
        ItemStack item = createItem(Material.COPPER_INGOT,
                "&6Miedziany Grosz",
                "copper_coin",
                Arrays.asList(
                        "&7Waluta obiegowa wyspy bita w mennicy.",
                        "&8Wartość handlowa nacji."
                ));
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return item;
    }

    public ItemStack getGoldDucat(int amount) {
        ItemStack item = createItem(Material.GOLD_INGOT,
                "&e&lZłoty Dukat",
                "gold_ducat",
                Arrays.asList(
                        "&7Wysokowartościowa moneta państwowa.",
                        "&8Warta 10 Miedzianych Groszy."
                ));
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return item;
    }

    public ItemStack getBrokenRhythmStandard() {
        return createItem(Material.RED_BANNER,
                "&4&lZłamany Sztandar Rytmu",
                "broken_standard",
                Arrays.asList(
                        "&cŚwięte trofeum wojenne!",
                        "&7Pozostałość po poległym generale Paolo Or'e.",
                        "&7Świadectwo ostatecznej klęski wodza na wyspie.",
                        "&6Niezwykle cenny artefakt dla wrogiego państwa!"
                ));
    }

    public ItemStack getForeignLootChest() {
        return createItem(Material.ENDER_CHEST,
                "&9&lSkrzynia Zagranicznego Łupu",
                "foreign_loot_chest",
                Arrays.asList(
                        "&7Zrabowane przez piratów kosztowności z odległych krain.",
                        "&aNagroda za odparcie i rozbicie morskiego desantu!",
                        "&eKliknij PPM, aby otworzyć i zdobyć skarby."
                ));
    }

    public ItemStack getSuperFurnace() {
        return createItem(Material.BLAST_FURNACE,
                "&6&lSuper Piec Przemysłowy",
                "super_furnace",
                Arrays.asList(
                        "&7Zaawansowany piec hutniczy nowej generacji.",
                        "&ePrzepala 3 sztuki surowca jednocześnie na raz!",
                        "&cWymóg: Wszystkie 3 sloty muszą zawierać ten sam surowiec!",
                        "&8Wydajność: 3x szybszy przetop rud i surowców.",
                        "&6Postaw na ziemi i kliknij PPM, aby otworzyć panel hutniczy!"
                ));
    }

    public ItemStack getShortShield() {
        ItemStack item = createItem(Material.SHIELD,
                "&e&lKrótka Tarcza Bojowa (Puklerz)",
                "short_shield",
                Arrays.asList(
                        "&7Kompaktowa, zwinna tarcza wyspiarska do szybkich potyczek.",
                        "&a+10% Prędkości ruchu podczas trzymania w dłoni!",
                        "&bSkraca czas ochłonięcia po ciosie toporem o 50% (Parowanie)!",
                        "&6Autorski model taktyczny &8| &eCustomModelData: 1001",
                        "&7Twórca projektu: &f&lfilipGFF1"
                ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(1001);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isShortShield(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD) return false;
        String id = getCustomItemId(item);
        if ("short_shield".equals(id)) return true;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 1001) {
            return true;
        }
        return false;
    }

    public String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_CUSTOM_ITEM, PersistentDataType.STRING);
    }
}
