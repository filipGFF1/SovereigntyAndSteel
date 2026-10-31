package pl.sovereignty.model;

import org.bukkit.Material;

public enum VaultTier {
    TIER_1_WOODEN(1, "Skarbiec Drewniany", Material.BARREL, 1, 0.0, false,
            new String[]{
                    "&7Podstawowy magazyn państwowy.",
                    "&7Pojemność: &e1 Skrzynia &8(54 sloty)",
                    "&7Standardowa przechowalnia surowców."
            }),
    TIER_2_REINFORCED_IRON(2, "Skarbiec Żelazny", Material.IRON_BLOCK, 2, 1500.0, false,
            new String[]{
                    "&7Wzmocniona konstrukcja okuta stalą.",
                    "&7Pojemność: &e2 Skrzynie &8(108 slotów)",
                    "&7Podwojona przestrzeń na urobek i plony."
            }),
    TIER_3_GILDED_GOLD(3, "Skarbiec Królewski (Złoty)", Material.GOLD_BLOCK, 3, 3500.0, false,
            new String[]{
                    "&7Królewska komora skarbowo-zasobowa.",
                    "&7Pojemność: &e3 Skrzynie &8(162 sloty)",
                    "&7Pojemny magazyn na minerały i drewno."
            }),
    TIER_4_DIAMOND_RESERVE(4, "Skarbiec Diamentowy", Material.DIAMOND_BLOCK, 4, 7500.0, false,
            new String[]{
                    "&7Potężny Skarbiec Narodowy mocarstwa.",
                    "&7Pojemność: &e4 Skrzynie &8(216 slotów)",
                    "&7Ogromna rezerwa surowcowa państwa."
            }),
    TIER_5_NETHERITE_VAULT(5, "Skarbiec Imperialny (Netheryt)", Material.NETHERITE_BLOCK, 5, 15000.0, true,
            new String[]{
                    "&7Mityczna, niezniszczalna skarbnica imperium.",
                    "&7Pojemność: &e5 Skrzyń &8(270 slotów)",
                    "&6⭐ MODUŁ AUTO-SELL: &aAutomatyczna sprzedaż",
                    "&7nadmiaru urobku wprost do Skarbca Narodowego!"
            });

    private final int level;
    private final String displayName;
    private final Material icon;
    private final int pages;
    private final double cost;
    private final boolean autoSell;
    private final String[] lore;

    VaultTier(int level, String displayName, Material icon, int pages, double cost, boolean autoSell, String[] lore) {
        this.level = level;
        this.displayName = displayName;
        this.icon = icon;
        this.pages = pages;
        this.cost = cost;
        this.autoSell = autoSell;
        this.lore = lore;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getPages() {
        return pages;
    }

    public double getCost() {
        return cost;
    }

    public boolean hasAutoSell() {
        return autoSell;
    }

    public String[] getLore() {
        return lore;
    }

    public VaultTier getNext() {
        return switch (this) {
            case TIER_1_WOODEN -> TIER_2_REINFORCED_IRON;
            case TIER_2_REINFORCED_IRON -> TIER_3_GILDED_GOLD;
            case TIER_3_GILDED_GOLD -> TIER_4_DIAMOND_RESERVE;
            case TIER_4_DIAMOND_RESERVE -> TIER_5_NETHERITE_VAULT;
            case TIER_5_NETHERITE_VAULT -> null;
        };
    }
}
