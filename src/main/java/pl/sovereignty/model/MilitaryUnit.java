package pl.sovereignty.model;

import org.bukkit.Location;

import java.util.UUID;

public class MilitaryUnit {
    private final UUID entityUuid;
    private final UUID nationId;
    private final MilitaryUnitType type;
    private MilitaryFormation formation;
    private Location commandTargetLocation;
    private UUID commandTargetEntity;
    private long panicUntilTimestamp;

    public MilitaryUnit(UUID entityUuid, UUID nationId, MilitaryUnitType type) {
        this.entityUuid = entityUuid;
        this.nationId = nationId;
        this.type = type;
        this.formation = MilitaryFormation.SHIELD_WALL;
        this.panicUntilTimestamp = 0;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public UUID getNationId() {
        return nationId;
    }

    public MilitaryUnitType getType() {
        return type;
    }

    public MilitaryFormation getFormation() {
        return formation;
    }

    public void setFormation(MilitaryFormation formation) {
        this.formation = formation;
    }

    public Location getCommandTargetLocation() {
        return commandTargetLocation;
    }

    public void setCommandTargetLocation(Location commandTargetLocation) {
        this.commandTargetLocation = commandTargetLocation;
    }

    public UUID getCommandTargetEntity() {
        return commandTargetEntity;
    }

    public void setCommandTargetEntity(UUID commandTargetEntity) {
        this.commandTargetEntity = commandTargetEntity;
    }

    public boolean isPanicking() {
        return System.currentTimeMillis() < panicUntilTimestamp;
    }

    public void setPanicDurationMs(long durationMs) {
        this.panicUntilTimestamp = System.currentTimeMillis() + durationMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MilitaryUnit that = (MilitaryUnit) o;
        return entityUuid.equals(that.entityUuid);
    }

    @Override
    public int hashCode() {
        return entityUuid.hashCode();
    }
}
