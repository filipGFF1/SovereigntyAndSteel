package pl.sovereignty.model;

import org.bukkit.Material;

public enum SiegeMachineType {
    BATTERING_RAM("Mobilny Taran", Material.OAK_LOG, 250.0, "Wyważa wrota, bramy i palisady"),
    BALLISTA("Balista Obronna", Material.CROSSBOW, 180.0, "Działo obronne; bełty przebijają do 3 wrogów"),
    MORTAR("Moździerz Stromotorowy", Material.COPPER_BLOCK, 200.0, "Miotacz wybuchowych kul z bruku i prochu");

    private final String displayName;
    private final Material icon;
    private final double maxHealth;
    private final String description;

    SiegeMachineType(String displayName, Material icon, double maxHealth, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.maxHealth = maxHealth;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public String getDescription() {
        return description;
    }
}
