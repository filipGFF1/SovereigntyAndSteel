package pl.sovereignty.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum TechnologyTier {
    TIER_1_PRIMITIVE(
            1,
            "Prymitywna Osada (Poziom I)",
            Material.WOODEN_HOE,
            0.0,
            0.30, // 30% wydajności
            Arrays.asList(
                    "&7Początkowy poziom rozwoju technicznego.",
                    "&8Wydajność: &c30%",
                    "&7- Kopalnia: wyłącznie bruk i węgiel w wolnym cyklu (60s).",
                    "&7- Tartak: podstawowy zasięg (15 bloków), brak autoodsadzania.",
                    "&a- Farma: zbiór pszenicy, marchwi, ziemniaków i buraków (1 plon)."
            )
    ),
    TIER_2_STEEL_TOOLS(
            2,
            "Narzędzia Stalowe (Poziom II)",
            Material.IRON_AXE,
            1000.0, // 1 000 $ (obniżone z 5 000 $)
            0.55, // 55% wydajności
            Arrays.asList(
                    "&7Wprowadzenie hartowanej stali i leśnictwa.",
                    "&8Wydajność: &e55%",
                    "&a- Tartak: zasięg 20 bloków + automatyczne sadzenie lasu.",
                    "&a- Kopalnia: wydobycie rudy miedzi (zwiększone zyski).",
                    "&a- Farma: obsługa trzciny cukrowej i słodkich jagód.",
                    "&eKoszt odblokowania: &61 000 $"
            )
    ),
    TIER_3_EARLY_MECHANIZATION(
            3,
            "Mechanizacja Wczesna (Poziom III)",
            Material.PISTON,
            2500.0, // 2 500 $ (obniżone z 15 000 $)
            0.80, // 80% wydajności
            Arrays.asList(
                    "&7Proste przekładnie, kołowroty i nawozy.",
                    "&8Wydajność: &a80%",
                    "&a- Tartak: zasięg 25 bloków + szansa na jabłka.",
                    "&a- Farma: dynie, arbuzy, kakao, brodawki i plon x2.",
                    "&b- Kopalnia: żelazo, lapis oraz rzadka szansa na Diamenty (3%)!",
                    "&eKoszt odblokowania: &62 500 $"
            )
    ),
    TIER_4_INDUSTRIAL_REVOLUTION(
            4,
            "Rewolucja Przemysłowa (Poziom IV)",
            Material.BLAST_FURNACE,
            6000.0, // 6 000 $ (obniżone z 40 000 $)
            1.00, // 100% pełna norma docelowa
            Arrays.asList(
                    "&7Parowe napędy, piece hutnicze i pełna automatyzacja.",
                    "&8Wydajność: &b100% (Pełna Norma)",
                    "&a- Tartak: zasięg 30 bloków, zbalansowana wycinka i nawożenie mączką.",
                    "&b- Kopalnia: złoto, szmaragdy i regularne wydobycie Diamentów (8%)!",
                    "&a- Farma: pełny areał, gotowy chleb, ciasta i złote marchewki.",
                    "&eKoszt odblokowania: &66 000 $"
            )
    ),
    TIER_5_MASTER_CRAFTSMANSHIP(
            5,
            "Rzemiosło Mistrzowskie (Poziom V)",
            Material.NETHER_STAR,
            15000.0, // 15 000 $ (obniżone ze 100 000 $)
            1.15, // 115% wydajności (z Monarchią 120%)
            Arrays.asList(
                    "&7Elitarna wiedza inżynieryjna wyspy (Endgame).",
                    "&8Wydajność: &d115% &7(do &a120% &7z Monarchią)",
                    "&a- Tartak: mistrzowski zasięg 35 bloków i wysoka efektywność.",
                    "&b- Kopalnia: maksymalna prędkość, 1-2 Diamenty (15%) i bogate złoża.",
                    "&a- Farma: podwojona szansa na złote marchewki i chleb.",
                    "&a- Najwyższy prestiż cywilizacyjny na wyspie.",
                    "&eKoszt odblokowania: &615 000 $"
            )
    );

    private final int level;
    private final String displayName;
    private final Material icon;
    private final double cost;
    private final double yieldMultiplier;
    private final List<String> lore;

    TechnologyTier(int level, String displayName, Material icon, double cost, double yieldMultiplier, List<String> lore) {
        this.level = level;
        this.displayName = displayName;
        this.icon = icon;
        this.cost = cost;
        this.yieldMultiplier = yieldMultiplier;
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

    public double getCost() {
        return cost;
    }

    public double getYieldMultiplier() {
        return yieldMultiplier;
    }

    public List<String> getLore() {
        return lore;
    }

    public TechnologyTier getNext() {
        return switch (this) {
            case TIER_1_PRIMITIVE -> TIER_2_STEEL_TOOLS;
            case TIER_2_STEEL_TOOLS -> TIER_3_EARLY_MECHANIZATION;
            case TIER_3_EARLY_MECHANIZATION -> TIER_4_INDUSTRIAL_REVOLUTION;
            case TIER_4_INDUSTRIAL_REVOLUTION -> TIER_5_MASTER_CRAFTSMANSHIP;
            case TIER_5_MASTER_CRAFTSMANSHIP -> null;
        };
    }
}
