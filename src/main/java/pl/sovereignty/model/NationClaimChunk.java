package pl.sovereignty.model;

import java.util.Objects;
import java.util.UUID;

public class NationClaimChunk {
    private final String world;
    private final int x;
    private final int z;
    private final UUID nationId;
    private final long claimedAt;

    public NationClaimChunk(String world, int x, int z, UUID nationId) {
        this(world, x, z, nationId, System.currentTimeMillis());
    }

    public NationClaimChunk(String world, int x, int z, UUID nationId, long claimedAt) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.nationId = nationId;
        this.claimedAt = claimedAt;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public UUID getNationId() {
        return nationId;
    }

    public long getClaimedAt() {
        return claimedAt;
    }

    public String getKey() {
        return world + ":" + x + "," + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NationClaimChunk that)) return false;
        return x == that.x && z == that.z && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }
}
