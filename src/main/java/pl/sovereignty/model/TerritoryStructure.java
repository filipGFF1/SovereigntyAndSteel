package pl.sovereignty.model;

import org.bukkit.Location;

import java.util.UUID;

public class TerritoryStructure {
    private final UUID id;
    private final UUID nationId;
    private final StructureType type;
    private final Location location;
    private boolean supplyConnected;
    private double health;
    private final double maxHealth;
    private UUID assignedNpcUuid;
    private long lastProductionTime;

    public TerritoryStructure(UUID id, UUID nationId, StructureType type, Location location) {
        this.id = id;
        this.nationId = nationId;
        this.type = type;
        this.location = location;
        this.supplyConnected = true;
        this.maxHealth = switch (type) {
            case BASTION_CORE -> 1000.0;
            case WATCHTOWER -> 500.0;
            case BARRACKS, WAR_COUNCIL -> 350.0;
            default -> 200.0;
        };
        this.health = this.maxHealth;
        this.lastProductionTime = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNationId() {
        return nationId;
    }

    public StructureType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isSupplyConnected() {
        return supplyConnected;
    }

    public void setSupplyConnected(boolean supplyConnected) {
        this.supplyConnected = supplyConnected;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(this.maxHealth, health));
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public UUID getAssignedNpcUuid() {
        return assignedNpcUuid;
    }

    public void setAssignedNpcUuid(UUID assignedNpcUuid) {
        this.assignedNpcUuid = assignedNpcUuid;
    }

    public long getLastProductionTime() {
        return lastProductionTime;
    }

    public void setLastProductionTime(long lastProductionTime) {
        this.lastProductionTime = lastProductionTime;
    }
}
