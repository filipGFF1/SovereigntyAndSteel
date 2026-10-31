package pl.sovereignty.model;

public enum MilitaryFormation {
    SHIELD_WALL("Mur Tarcz", "Zwięzły szyk obronny, +40% redukcji obrażeń, spowolnienie"),
    CHARGE("Szarża", "Agresywny atak w zwarciu, +20% prędkości poruszania się i bonus do impetu"),
    RETREAT("Odwrót do Fortu", "Błyskawiczny odwrót do najbliższego bastionu lub koszar");

    private final String displayName;
    private final String description;

    MilitaryFormation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public MilitaryFormation next() {
        return switch (this) {
            case SHIELD_WALL -> CHARGE;
            case CHARGE -> RETREAT;
            case RETREAT -> SHIELD_WALL;
        };
    }
}
