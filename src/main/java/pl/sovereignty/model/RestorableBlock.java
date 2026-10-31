package pl.sovereignty.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class RestorableBlock {
    private final Location location;
    private final Material originalMaterial;
    private final BlockData originalBlockData;
    private final long destroyedTimestamp;

    public RestorableBlock(Location location, Material originalMaterial, BlockData originalBlockData) {
        this.location = location;
        this.originalMaterial = originalMaterial;
        this.originalBlockData = originalBlockData.clone();
        this.destroyedTimestamp = System.currentTimeMillis();
    }

    public Location getLocation() {
        return location;
    }

    public Material getOriginalMaterial() {
        return originalMaterial;
    }

    public BlockData getOriginalBlockData() {
        return originalBlockData;
    }

    public long getDestroyedTimestamp() {
        return destroyedTimestamp;
    }
}
