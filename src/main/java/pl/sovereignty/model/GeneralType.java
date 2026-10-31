package pl.sovereignty.model;

public enum GeneralType {
    PAOLO_ORE("Generał Paolo Or'e", "&6Generał Paolo Or'e", "Młot i Żelazna Obrona"),
    KLOT_NAPOLEON("Kłot Napoleon Ciepły", "&e&lGenerał Kłot Napoleon Ciepły", "Ciepły Taktyk i Błyskawiczny Manewr");

    private final String simpleName;
    private final String displayName;
    private final String doctrine;

    GeneralType(String simpleName, String displayName, String doctrine) {
        this.simpleName = simpleName;
        this.displayName = displayName;
        this.doctrine = doctrine;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDoctrine() {
        return doctrine;
    }
}
