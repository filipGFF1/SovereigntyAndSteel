package pl.sovereignty.model;

import org.bukkit.Location;

import java.util.UUID;

public class SiegeMachine {
    private final UUID id;
    private final UUID nationId;
    private final SiegeMachineType type;
    private Location location;
    private UUID interactionEntityUuid;
    private UUID displayEntityUuid;
    private UUID textDisplayUuid;
    private double health;
    private int wrenchHits;
    private long lastFiredTime;

    public SiegeMachine(UUID id, UUID nationId, SiegeMachineType type, Location location) {
        this.id = id;
        this.nationId = nationId;
        this.type = type;
        this.location = location;
        this.health = type.getMaxHealth();
        this.wrenchHits = 0;
        this.lastFiredTime = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNationId() {
        return nationId;
    }

    public SiegeMachineType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getInteractionEntityUuid() {
        return interactionEntityUuid;
    }

    public void setInteractionEntityUuid(UUID interactionEntityUuid) {
        this.interactionEntityUuid = interactionEntityUuid;
    }

    public UUID getDisplayEntityUuid() {
        return displayEntityUuid;
    }

    public void setDisplayEntityUuid(UUID displayEntityUuid) {
        this.displayEntityUuid = displayEntityUuid;
    }

    public UUID getTextDisplayUuid() {
        return textDisplayUuid;
    }

    public void setTextDisplayUuid(UUID textDisplayUuid) {
        this.textDisplayUuid = textDisplayUuid;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(this.type.getMaxHealth(), health));
    }

    public int getWrenchHits() {
        return wrenchHits;
    }

    public void setWrenchHits(int wrenchHits) {
        this.wrenchHits = wrenchHits;
    }

    public int incrementWrenchHits() {
        this.wrenchHits++;
        return this.wrenchHits;
    }

    public long getLastFiredTime() {
        return lastFiredTime;
    }

    public void setLastFiredTime(long lastFiredTime) {
        this.lastFiredTime = lastFiredTime;
    }
}
