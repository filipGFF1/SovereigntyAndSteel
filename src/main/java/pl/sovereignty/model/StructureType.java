package pl.sovereignty.model;

import org.bukkit.Material;

public enum StructureType {
    BASTION_CORE("Rdzeń Bastionu", Material.CRYING_OBSIDIAN, 65, false),
    WATCHTOWER("Strażnica Graniczna", Material.STONE_BRICKS, 35, false),
    STATE_FARM("Centrum Rolnicze", Material.HAY_BLOCK, 20, true),
    SAWMILL_TOTEM("Totem Tartaku", Material.STRIPPED_OAK_WOOD, 15, true),
    MINING_SHAFT("Głowica Szybu Górniczego", Material.BLAST_FURNACE, 10, true),
    BARRACKS("Koszary Wojskowe", Material.WHITE_WOOL, 15, true),
    WAR_COUNCIL("Stół Narad Wojennych", Material.CRAFTING_TABLE, 10, false),
    NATIONAL_VAULT("Skarbiec Krajowy", Material.BARREL, 25, false);

    private final String displayName;
    private final Material defaultBlock;
    private final int influenceRadius;
    private final boolean requiresSupplyRoute;

    StructureType(String displayName, Material defaultBlock, int influenceRadius, boolean requiresSupplyRoute) {
        this.displayName = displayName;
        this.defaultBlock = defaultBlock;
        this.influenceRadius = influenceRadius;
        this.requiresSupplyRoute = requiresSupplyRoute;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getDefaultBlock() {
        return defaultBlock;
    }

    public int getInfluenceRadius() {
        return influenceRadius;
    }

    public boolean isRequiresSupplyRoute() {
        return requiresSupplyRoute;
    }
}
