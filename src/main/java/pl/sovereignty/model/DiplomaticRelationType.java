package pl.sovereignty.model;

import org.bukkit.ChatColor;

public enum DiplomaticRelationType {
    ALLY("&a&lSOJUSZ", "&a", true),
    NAP("&e&lPAKT O NIEAGRESJI", "&e", true),
    NEUTRAL("&7NEUTRALNOŚĆ", "&7", false),
    RIVAL("&c&lRYWAL / WRÓG", "&c", false),
    TRUCE("&b&lROZEJM", "&b", false);

    private final String displayName;
    private final String colorCode;
    private final boolean requiresMutualConsent;

    DiplomaticRelationType(String displayName, String colorCode, boolean requiresMutualConsent) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.requiresMutualConsent = requiresMutualConsent;
    }

    public String getDisplayName() {
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    public String getColorCode() {
        return ChatColor.translateAlternateColorCodes('&', colorCode);
    }

    public boolean isRequiresMutualConsent() {
        return requiresMutualConsent;
    }
}
