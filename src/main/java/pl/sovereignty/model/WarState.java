package pl.sovereignty.model;

import java.util.UUID;

public class WarState {
    private final UUID attackerNationId;
    private final UUID defenderNationId;
    private final long warDeclaredTimestamp;
    private boolean siegeActive;
    private long siegeStartTimestamp;
    private boolean concluded;

    public WarState(UUID attackerNationId, UUID defenderNationId) {
        this.attackerNationId = attackerNationId;
        this.defenderNationId = defenderNationId;
        this.warDeclaredTimestamp = System.currentTimeMillis();
        // Warmup: 5 minutes (300,000 ms) before full siege starts
        this.siegeStartTimestamp = this.warDeclaredTimestamp + 300_000L;
        this.siegeActive = false;
        this.concluded = false;
    }

    public UUID getAttackerNationId() {
        return attackerNationId;
    }

    public UUID getDefenderNationId() {
        return defenderNationId;
    }

    public long getWarDeclaredTimestamp() {
        return warDeclaredTimestamp;
    }

    public boolean isSiegeActive() {
        return siegeActive;
    }

    public void setSiegeActive(boolean siegeActive) {
        this.siegeActive = siegeActive;
    }

    public long getSiegeStartTimestamp() {
        return siegeStartTimestamp;
    }

    public void setSiegeStartTimestamp(long siegeStartTimestamp) {
        this.siegeStartTimestamp = siegeStartTimestamp;
    }

    public boolean isConcluded() {
        return concluded;
    }

    public void setConcluded(boolean concluded) {
        this.concluded = concluded;
    }

    public boolean isParticipant(UUID nationId) {
        return attackerNationId.equals(nationId) || defenderNationId.equals(nationId);
    }
}
