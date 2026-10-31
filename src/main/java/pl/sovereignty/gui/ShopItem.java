package pl.sovereignty.gui;

import org.bukkit.Material;

public class ShopItem {
    private final Material material;
    private final String displayName;
    private final double buyPrice;
    private final double sellPrice;

    public ShopItem(Material material, String displayName, double buyPrice, double sellPrice) {
        this.material = material;
        this.displayName = displayName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public boolean canBuy() {
        return buyPrice > 0;
    }

    public boolean canSell() {
        return sellPrice > 0;
    }
}
