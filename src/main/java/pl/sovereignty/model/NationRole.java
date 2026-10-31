package pl.sovereignty.model;

public enum NationRole {
    LEADER("Władca", 3),
    OFFICER("Generał / Oficer", 2),
    CITIZEN("Obywatel", 1);

    private final String displayName;
    private final int powerLevel;

    NationRole(String displayName, int powerLevel) {
        this.displayName = displayName;
        this.powerLevel = powerLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPowerLevel() {
        return powerLevel;
    }
}
