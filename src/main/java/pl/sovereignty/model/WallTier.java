package pl.sovereignty.model;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum WallTier {
    NONE(
            "Brak Fortyfikacji",
            Material.BARRIER,
            0.0,
            0,
            Arrays.asList("&7Otwarte granice bez murów obronnych.", "&8Niskie bezpieczeństwo przed infiltracją.")
    ),
    TIER_1_WOODEN_PALISADE(
            "Palisada Drewniana (Tier 1)",
            Material.OAK_FENCE,
            2.5, // 2.5 $ za metr bieżący (obniżone z 5.0 $)
            100, // punkty strukturalne
            Arrays.asList(
                    "&7Płotki dębowe i zaostrzone bale.",
                    "&eKoszt: &62.5 $ &7za metr bieżący granicy.",
                    "&fWysokość: 4 bloki.",
                    "&aAutomatycznie przesuwa się na nowo wykupione chunki!"
            )
    ),
    TIER_2_STONE_WALL(
            "Kamienny Mur Obronny (Tier 2)",
            Material.STONE_BRICKS,
            10.0, // 10 $ za metr bieżący (obniżone z 25.0 $)
            350,
            Arrays.asList(
                    "&7Bruk, kamienne cegły i blanki strzelnicze.",
                    "&eKoszt: &610.0 $ &7za metr bieżący granicy.",
                    "&fWysokość: 4 bloki z blankami dla łuczników.",
                    "&aAutomatycznie przesuwa się na nowo wykupione chunki!"
            )
    ),
    TIER_3_COPPER_DEEPSLATE(
            "Mur Głazowo-Miedziany (Tier 3)",
            Material.DEEPSLATE_BRICKS,
            25.0, // 25 $ za metr bieżący (obniżone z 70.0 $)
            800,
            Arrays.asList(
                    "&7Wzmocniona głębinówka i płyty z miedzi.",
                    "&eKoszt: &625.0 $ &7za metr bieżący granicy.",
                    "&bOdporny na podstawowe ładunki wybuchowe i tarany.",
                    "&aAutomatycznie przesuwa się na nowo wykupione chunki!"
            )
    );

    private final String displayName;
    private final Material icon;
    private final double costPerMeter;
    private final int healthPoints;
    private final List<String> lore;

    WallTier(String displayName, Material icon, double costPerMeter, int healthPoints, List<String> lore) {
        this.displayName = displayName;
        this.icon = icon;
        this.costPerMeter = costPerMeter;
        this.healthPoints = healthPoints;
        this.lore = lore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public double getCostPerMeter() {
        return costPerMeter;
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public List<String> getLore() {
        return lore;
    }
}
