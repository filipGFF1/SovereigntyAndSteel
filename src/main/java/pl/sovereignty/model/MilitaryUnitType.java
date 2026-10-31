package pl.sovereignty.model;

public enum MilitaryUnitType {
    SHIELDBEARER("Kohorta Tarczowników", 30.0, "Blokuje wąskie przesmyki wyspy, tarcza i miecz"),
    CROSSBOWMAN("Kompania Kuszników", 20.0, "Wystrzeliwuje ładunki wiatru zrzucające wrogów z klifów"),
    SAPPER("Saperzy", 24.0, "Naprawia machiny oblężnicze i stawia drewniane zapory");

    private final String displayName;
    private final double baseHealth;
    private final String description;

    MilitaryUnitType(String displayName, double baseHealth, String description) {
        this.displayName = displayName;
        this.baseHealth = baseHealth;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseHealth() {
        return baseHealth;
    }

    public String getDescription() {
        return description;
    }
}
