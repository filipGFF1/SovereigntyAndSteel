package pl.sovereignty.model;

import org.bukkit.Location;

import java.util.UUID;

public class PaoloGeneral {
    private final UUID nationId;
    private final GeneralType type;
    private UUID entityUuid;
    private Location lastStandStandardLocation;
    private long drumrollActivatedTimestamp;
    private boolean lastStandTriggered;
    private int synchronizedHitCounter;

    public PaoloGeneral(UUID nationId, UUID entityUuid) {
        this(nationId, entityUuid, GeneralType.PAOLO_ORE);
    }

    public PaoloGeneral(UUID nationId, UUID entityUuid, GeneralType type) {
        this.nationId = nationId;
        this.entityUuid = entityUuid;
        this.type = (type != null ? type : GeneralType.PAOLO_ORE);
        this.drumrollActivatedTimestamp = 0;
        this.lastStandTriggered = false;
        this.synchronizedHitCounter = 0;
    }

    public UUID getNationId() {
        return nationId;
    }

    public GeneralType getType() {
        return type;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public Location getLastStandStandardLocation() {
        return lastStandStandardLocation;
    }

    public void setLastStandStandardLocation(Location lastStandStandardLocation) {
        this.lastStandStandardLocation = lastStandStandardLocation;
    }

    public boolean isDrumrollActive() {
        return System.currentTimeMillis() - drumrollActivatedTimestamp < 30_000L; // 30 seconds
    }

    public void activateDrumroll() {
        this.drumrollActivatedTimestamp = System.currentTimeMillis();
    }

    public boolean isLastStandTriggered() {
        return lastStandTriggered;
    }

    public void setLastStandTriggered(boolean lastStandTriggered) {
        this.lastStandTriggered = lastStandTriggered;
    }

    public int incrementHitCounter() {
        this.synchronizedHitCounter++;
        return this.synchronizedHitCounter;
    }
}
