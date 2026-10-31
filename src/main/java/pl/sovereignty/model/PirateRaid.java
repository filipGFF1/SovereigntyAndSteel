package pl.sovereignty.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PirateRaid {
    private final UUID raidId;
    private final UUID targetNationId;
    private final Location landingLocation;
    private final Location tributeBeaconLocation;
    private final List<UUID> pirateUuids;
    private final long startTime;
    private boolean tributePaid;
    private boolean completed;
    private final boolean megaOp;

    public PirateRaid(UUID raidId, UUID targetNationId, Location landingLocation, Location tributeBeaconLocation) {
        this(raidId, targetNationId, landingLocation, tributeBeaconLocation, false);
    }

    public PirateRaid(UUID raidId, UUID targetNationId, Location landingLocation, Location tributeBeaconLocation, boolean megaOp) {
        this.raidId = raidId;
        this.targetNationId = targetNationId;
        this.landingLocation = landingLocation;
        this.tributeBeaconLocation = tributeBeaconLocation;
        this.pirateUuids = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
        this.tributePaid = false;
        this.completed = false;
        this.megaOp = megaOp;
    }

    public UUID getRaidId() {
        return raidId;
    }

    public UUID getTargetNationId() {
        return targetNationId;
    }

    public Location getLandingLocation() {
        return landingLocation;
    }

    public Location getTributeBeaconLocation() {
        return tributeBeaconLocation;
    }

    public List<UUID> getPirateUuids() {
        return pirateUuids;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isTributePaid() {
        return tributePaid;
    }

    public void setTributePaid(boolean tributePaid) {
        this.tributePaid = tributePaid;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isMegaOp() {
        return megaOp;
    }

    public boolean isExpired() {
        // Tribute window lasts 3 minutes (180,000 ms)
        return System.currentTimeMillis() - startTime > 180_000L;
    }
}
