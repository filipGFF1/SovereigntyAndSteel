package pl.sovereignty.model;

import java.util.*;

public class Nation {
    private final UUID id;
    private String name;
    private String tag;
    private UUID leaderUuid;
    private final Map<UUID, NationRole> members;
    private double treasury;
    private double foodStockpile;
    private double woodStockpile;
    private double rawMaterialsStockpile;
    private double mintedCurrency;
    private double tradeVolume;
    private double gdp;
    private int militaryScore;
    private int piratesDefeated;
    private int tributesPaid;
    private double customsTaxRate;
    private boolean starving;
    private final Set<UUID> structures;
    private final long createdAt;

    // Nowe systemy: Vault, Ustroje, Technologia, Mury, Squaremap, Chunki, Rozbudowa Skarbca
    private double bankBalance;
    private PoliticalRegime regime;
    private TechnologyTier techTier;
    private WallTier wallTier;
    private VaultTier vaultTier;
    private boolean vaultAutoSell;
    private String mapColorHex;
    private long lastClaimTimestamp;
    private boolean isIndebted;
    private int debtDays;
    private int farmCount;
    private final Set<NationClaimChunk> claimedChunks;
    private double raidThreatChance;
    private long lastRaidThreatIncrementTime;

    // Dyplomacja i Relacje
    private final Map<UUID, DiplomaticRelationType> relations;
    private final Map<UUID, Long> relationExpiries;
    private final Map<UUID, DiplomaticRelationType> pendingRelationOffers;

    // Okna Wojenne (Siege Window)
    private int siegeWindowStartHour;
    private int siegeWindowEndHour;

    // Bezpieczeństwo i Wojna
    private int declaredWarCount;
    private UUID overlordNationId;
    private int tributeDaysRemaining;

    public Nation(UUID id, String name, String tag, UUID leaderUuid) {
        this.id = id;
        this.name = name;
        this.tag = tag.toUpperCase();
        this.leaderUuid = leaderUuid;
        this.members = new HashMap<>();
        this.members.put(leaderUuid, NationRole.LEADER);
        this.treasury = 100.0;
        this.foodStockpile = 50.0;
        this.woodStockpile = 50.0;
        this.rawMaterialsStockpile = 20.0;
        this.mintedCurrency = 0.0;
        this.tradeVolume = 0.0;
        this.gdp = 100.0;
        this.militaryScore = 0;
        this.piratesDefeated = 0;
        this.tributesPaid = 0;
        this.customsTaxRate = 0.005; // 0.5%
        this.starving = false;
        this.structures = new HashSet<>();
        this.createdAt = System.currentTimeMillis();

        this.bankBalance = 2000.0; // Startowy budżet w Vault na rozruch
        this.regime = PoliticalRegime.HEREDITARY_MONARCHY;
        this.techTier = TechnologyTier.TIER_1_PRIMITIVE;
        this.wallTier = WallTier.NONE;
        this.vaultTier = VaultTier.TIER_1_WOODEN;
        this.vaultAutoSell = true;
        this.mapColorHex = "#3388FF";
        this.lastClaimTimestamp = 0L;
        this.isIndebted = false;
        this.debtDays = 0;
        this.farmCount = 0;
        this.claimedChunks = new HashSet<>();
        this.raidThreatChance = 0.0;
        this.lastRaidThreatIncrementTime = 0L;
        this.relations = new HashMap<>();
        this.relationExpiries = new HashMap<>();
        this.pendingRelationOffers = new HashMap<>();
        this.siegeWindowStartHour = 18;
        this.siegeWindowEndHour = 22;
        this.declaredWarCount = 0;
        this.overlordNationId = null;
        this.tributeDaysRemaining = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag.toUpperCase();
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
        this.members.put(leaderUuid, NationRole.LEADER);
    }

    public Map<UUID, NationRole> getMembers() {
        return members;
    }

    public boolean isMember(UUID playerUuid) {
        return members.containsKey(playerUuid);
    }

    public NationRole getRole(UUID playerUuid) {
        return members.get(playerUuid);
    }

    public void addMember(UUID playerUuid, NationRole role) {
        members.put(playerUuid, role);
    }

    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
    }

    public double getTreasury() {
        return treasury;
    }

    public void setTreasury(double treasury) {
        this.treasury = Math.max(0, treasury);
    }

    public void addTreasury(double amount) {
        this.treasury += amount;
    }

    public double getFoodStockpile() {
        return foodStockpile;
    }

    public void setFoodStockpile(double foodStockpile) {
        this.foodStockpile = Math.max(0, foodStockpile);
        this.starving = (this.foodStockpile <= 0);
    }

    public void addFood(double amount) {
        this.foodStockpile = Math.max(0, this.foodStockpile + amount);
        this.starving = (this.foodStockpile <= 0);
    }

    public double getWoodStockpile() {
        return woodStockpile;
    }

    public void setWoodStockpile(double woodStockpile) {
        this.woodStockpile = Math.max(0, woodStockpile);
    }

    public void addWood(double amount) {
        this.woodStockpile = Math.max(0, this.woodStockpile + amount);
    }

    public double getRawMaterialsStockpile() {
        return rawMaterialsStockpile;
    }

    public void setRawMaterialsStockpile(double rawMaterialsStockpile) {
        this.rawMaterialsStockpile = Math.max(0, rawMaterialsStockpile);
    }

    public void addRawMaterials(double amount) {
        this.rawMaterialsStockpile = Math.max(0, this.rawMaterialsStockpile + amount);
    }

    public double getMintedCurrency() {
        return mintedCurrency;
    }

    public void addMintedCurrency(double amount) {
        this.mintedCurrency += amount;
    }

    public double getTradeVolume() {
        return tradeVolume;
    }

    public void addTradeVolume(double amount) {
        this.tradeVolume += amount;
    }

    public double getGdp() {
        return gdp;
    }

    public void setGdp(double gdp) {
        this.gdp = gdp;
    }

    public int getMilitaryScore() {
        return militaryScore;
    }

    public void setMilitaryScore(int militaryScore) {
        this.militaryScore = militaryScore;
    }

    public void addMilitaryScore(int amount) {
        this.militaryScore += amount;
    }

    public int getPiratesDefeated() {
        return piratesDefeated;
    }

    public void incrementPiratesDefeated() {
        this.piratesDefeated++;
    }

    public int getTributesPaid() {
        return tributesPaid;
    }

    public void incrementTributesPaid() {
        this.tributesPaid++;
    }

    public double getCustomsTaxRate() {
        return customsTaxRate;
    }

    public void setCustomsTaxRate(double customsTaxRate) {
        this.customsTaxRate = Math.max(0.001, Math.min(0.20, customsTaxRate));
    }

    public boolean isStarving() {
        return starving;
    }

    public void setStarving(boolean starving) {
        this.starving = starving;
    }

    public Set<UUID> getStructures() {
        return structures;
    }

    public void addStructure(UUID structureId) {
        structures.add(structureId);
    }

    public void removeStructure(UUID structureId) {
        structures.remove(structureId);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Oblicza wskaźnik inflacji: jeśli wyemitowano zbyt dużo waluty bez pokrycia w surowcach,
     * koszty jednostek i machin wzrastają!
     */
    public double getInflationFactor() {
        double reserve = rawMaterialsStockpile + (treasury / 2.0) + 20.0;
        double ratio = mintedCurrency / reserve;
        if (ratio <= 1.0) {
            return 1.0;
        }
        // Np. ratio 2.0 -> 1.5x kosztów; ratio 4.0 -> 2.5x kosztów
        return Math.min(3.0, 1.0 + (ratio - 1.0) * 0.5);
    }

    /**
     * Wskaźnik dobrobytu (Prosperity Index):
     * Każda wybudowana Farma Państwowa (Centrum Rolnicze) daje bezpośrednio +25 pkt dobrobytu,
     * powiększony o zapasy żywności na mieszkańca, stabilną walutę i niskie podatki!
     */
    public double getWelfareScore() {
        double foodPerMember = foodStockpile / Math.max(1, members.size());
        double inflationPenalty = Math.max(0, (getInflationFactor() - 1.0) * 50.0);
        double taxPenalty = customsTaxRate * 100.0;
        double farmBonus = farmCount * 25.0; // Bezpośredni dobrobyt ze struktur farmy (+25 pkt za farmę)
        return Math.max(0, (foodPerMember * 2.0) + 50.0 - inflationPenalty - taxPenalty + farmBonus);
    }

    public int getFarmCount() {
        return farmCount;
    }

    public void setFarmCount(int farmCount) {
        this.farmCount = Math.max(0, farmCount);
    }

    public void incrementFarmCount() {
        this.farmCount++;
    }

    public void decrementFarmCount() {
        if (this.farmCount > 0) {
            this.farmCount--;
        }
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = Math.max(0.0, bankBalance);
    }

    public void depositBank(double amount) {
        this.bankBalance += Math.max(0.0, amount);
    }

    public boolean withdrawBank(double amount) {
        if (amount <= 0.0) return false;
        if (this.bankBalance >= amount) {
            this.bankBalance -= amount;
            return true;
        }
        return false;
    }

    public PoliticalRegime getRegime() {
        return regime != null ? regime : PoliticalRegime.HEREDITARY_MONARCHY;
    }

    public void setRegime(PoliticalRegime regime) {
        this.regime = regime != null ? regime : PoliticalRegime.HEREDITARY_MONARCHY;
    }

    public TechnologyTier getTechTier() {
        return techTier != null ? techTier : TechnologyTier.TIER_1_PRIMITIVE;
    }

    public void setTechTier(TechnologyTier techTier) {
        this.techTier = techTier != null ? techTier : TechnologyTier.TIER_1_PRIMITIVE;
    }

    public WallTier getWallTier() {
        return wallTier != null ? wallTier : WallTier.NONE;
    }

    public void setWallTier(WallTier wallTier) {
        this.wallTier = wallTier != null ? wallTier : WallTier.NONE;
    }

    public String getMapColorHex() {
        return mapColorHex != null ? mapColorHex : "#3388FF";
    }

    public void setMapColorHex(String mapColorHex) {
        if (mapColorHex != null && mapColorHex.startsWith("#")) {
            this.mapColorHex = mapColorHex.toUpperCase();
        }
    }

    public long getLastClaimTimestamp() {
        return lastClaimTimestamp;
    }

    public void setLastClaimTimestamp(long lastClaimTimestamp) {
        this.lastClaimTimestamp = lastClaimTimestamp;
    }

    public boolean isIndebted() {
        return isIndebted;
    }

    public void setIndebted(boolean indebted) {
        isIndebted = indebted;
    }

    public int getDebtDays() {
        return debtDays;
    }

    public void setDebtDays(int debtDays) {
        this.debtDays = Math.max(0, debtDays);
    }

    public Set<NationClaimChunk> getClaimedChunks() {
        return claimedChunks;
    }

    public void addClaimedChunk(NationClaimChunk chunk) {
        claimedChunks.add(chunk);
    }

    public void removeClaimedChunk(NationClaimChunk chunk) {
        claimedChunks.remove(chunk);
    }

    public boolean hasClaimedChunk(String world, int x, int z) {
        for (NationClaimChunk c : claimedChunks) {
            if (c.getX() == x && c.getZ() == z && c.getWorld().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    public VaultTier getVaultTier() {
        return vaultTier != null ? vaultTier : VaultTier.TIER_1_WOODEN;
    }

    public void setVaultTier(VaultTier vaultTier) {
        this.vaultTier = vaultTier != null ? vaultTier : VaultTier.TIER_1_WOODEN;
    }

    public boolean isVaultAutoSell() {
        return vaultAutoSell;
    }

    public void setVaultAutoSell(boolean vaultAutoSell) {
        this.vaultAutoSell = vaultAutoSell;
    }

    public double getRaidThreatChance() {
        return raidThreatChance;
    }

    public void setRaidThreatChance(double raidThreatChance) {
        this.raidThreatChance = Math.max(0.0, Math.min(100.0, raidThreatChance));
    }

    public long getLastRaidThreatIncrementTime() {
        return lastRaidThreatIncrementTime;
    }

    public void setLastRaidThreatIncrementTime(long lastRaidThreatIncrementTime) {
        this.lastRaidThreatIncrementTime = lastRaidThreatIncrementTime;
    }

    // --- System Dyplomacji ---
    public DiplomaticRelationType getRelation(UUID otherNationId) {
        if (otherNationId == null || otherNationId.equals(this.id)) return DiplomaticRelationType.NEUTRAL;
        Long expiry = relationExpiries.get(otherNationId);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            relations.remove(otherNationId);
            relationExpiries.remove(otherNationId);
            return DiplomaticRelationType.NEUTRAL;
        }
        return relations.getOrDefault(otherNationId, DiplomaticRelationType.NEUTRAL);
    }

    public void setRelation(UUID otherNationId, DiplomaticRelationType type, long durationMillis) {
        if (otherNationId == null || otherNationId.equals(this.id)) return;
        if (type == DiplomaticRelationType.NEUTRAL) {
            relations.remove(otherNationId);
            relationExpiries.remove(otherNationId);
        } else {
            relations.put(otherNationId, type);
            if (durationMillis > 0) {
                relationExpiries.put(otherNationId, System.currentTimeMillis() + durationMillis);
            } else {
                relationExpiries.remove(otherNationId);
            }
        }
        pendingRelationOffers.remove(otherNationId);
    }

    public boolean isAlly(UUID otherNationId) {
        return getRelation(otherNationId) == DiplomaticRelationType.ALLY;
    }

    public boolean isRival(UUID otherNationId) {
        return getRelation(otherNationId) == DiplomaticRelationType.RIVAL;
    }

    public boolean hasActiveNAP(UUID otherNationId) {
        return getRelation(otherNationId) == DiplomaticRelationType.NAP;
    }

    public boolean hasActiveTruce(UUID otherNationId) {
        return getRelation(otherNationId) == DiplomaticRelationType.TRUCE;
    }

    public Map<UUID, DiplomaticRelationType> getAllRelations() {
        return Collections.unmodifiableMap(relations);
    }

    public Map<UUID, Long> getRelationExpiries() {
        return relationExpiries;
    }

    public Map<UUID, DiplomaticRelationType> getPendingRelationOffers() {
        return pendingRelationOffers;
    }

    // --- Okna Wojenne (Siege Window) ---
    public int getSiegeWindowStartHour() {
        return siegeWindowStartHour;
    }

    public void setSiegeWindowStartHour(int siegeWindowStartHour) {
        this.siegeWindowStartHour = Math.max(0, Math.min(23, siegeWindowStartHour));
    }

    public int getSiegeWindowEndHour() {
        return siegeWindowEndHour;
    }

    public void setSiegeWindowEndHour(int siegeWindowEndHour) {
        this.siegeWindowEndHour = Math.max(0, Math.min(23, siegeWindowEndHour));
    }

    public boolean isWithinSiegeWindow() {
        int currentHour = java.time.LocalTime.now().getHour();
        if (siegeWindowStartHour <= siegeWindowEndHour) {
            return currentHour >= siegeWindowStartHour && currentHour < siegeWindowEndHour;
        } else {
            // Np. od 22 do 2 w nocy
            return currentHour >= siegeWindowStartHour || currentHour < siegeWindowEndHour;
        }
    }

    // --- Tarcza Początkowa (Peace Shield) & Wojny ---
    public int getDeclaredWarCount() {
        return declaredWarCount;
    }

    public void setDeclaredWarCount(int declaredWarCount) {
        this.declaredWarCount = Math.max(0, declaredWarCount);
    }

    public void incrementDeclaredWars() {
        this.declaredWarCount++;
    }

    public boolean isPeaceShieldActive() {
        long elapsed = System.currentTimeMillis() - createdAt;
        long duration = 48L * 3600L * 1000L; // 48 godzin
        return (elapsed < duration) && (claimedChunks.size() < 20) && (declaredWarCount == 0);
    }

    public double getPeaceShieldRemainingHours() {
        long elapsed = System.currentTimeMillis() - createdAt;
        long duration = 48L * 3600L * 1000L;
        if (elapsed >= duration || claimedChunks.size() >= 20 || declaredWarCount > 0) return 0.0;
        return (duration - elapsed) / (1000.0 * 3600.0);
    }

    // --- Wasalizacja i Trybut ---
    public UUID getOverlordNationId() {
        return overlordNationId;
    }

    public void setOverlordNationId(UUID overlordNationId) {
        this.overlordNationId = overlordNationId;
    }

    public int getTributeDaysRemaining() {
        return tributeDaysRemaining;
    }

    public void setTributeDaysRemaining(int tributeDaysRemaining) {
        this.tributeDaysRemaining = Math.max(0, tributeDaysRemaining);
    }
}
