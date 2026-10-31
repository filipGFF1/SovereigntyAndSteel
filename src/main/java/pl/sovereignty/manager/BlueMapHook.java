package pl.sovereignty.manager;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.model.WallTier;

import java.util.function.Consumer;

public class BlueMapHook implements MapHook {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final ChunkClaimManager chunkClaimManager;
    private final VaultEconomyManager economyManager;
    private boolean enabled = false;

    private Consumer<BlueMapAPI> enableListener;
    private Consumer<BlueMapAPI> disableListener;
    private BlueMapAPI blueMapApi;

    public BlueMapHook(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager,
                       ChunkClaimManager chunkClaimManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.chunkClaimManager = chunkClaimManager;
        this.economyManager = economyManager;
    }

    @Override
    public String getName() {
        return "BlueMap";
    }

    @Override
    public void init() {
        if (Bukkit.getPluginManager().getPlugin("BlueMap") == null) {
            plugin.getLogger().info("BlueMap nie jest obecny na serwerze - mapa 3D BlueMap wyłączona.");
            return;
        }

        try {
            this.enableListener = api -> {
                this.blueMapApi = api;
                this.enabled = true;
                plugin.getLogger().info("Pomyślnie zintegrowano Sovereignty z silnikiem BlueMap 3D!");
                updateAll();
            };

            this.disableListener = api -> {
                this.blueMapApi = null;
                this.enabled = false;
            };

            BlueMapAPI.onEnable(enableListener);
            BlueMapAPI.onDisable(disableListener);

            // Jeśli BlueMap jest już załadowany
            BlueMapAPI.getInstance().ifPresent(api -> {
                this.blueMapApi = api;
                this.enabled = true;
                plugin.getLogger().info("Wykryto aktywną sesję BlueMap! Załadowano warstwy państw.");
                updateAll();
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Błąd inicjalizacji BlueMapHook: " + t.getMessage());
            this.enabled = false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && blueMapApi != null;
    }

    @Override
    public void updateAll() {
        if (!isEnabled()) return;
        for (Nation nation : nationManager.getAllNations()) {
            updateNation(nation);
        }
        updateStructures();
    }

    @Override
    public void updateNation(Nation nation) {
        if (!isEnabled()) return;

        try {
            Color nationFill = parseHexToColor(nation.getMapColorHex(), 0.35f);
            Color nationStroke = parseHexToColor(nation.getMapColorHex(), 1.0f);
            String tooltipHtml = buildNationTooltipHtml(nation);

            for (BlueMapMap map : blueMapApi.getMaps()) {
                String worldId = map.getWorld().getId();

                MarkerSet territorySet = map.getMarkerSets().computeIfAbsent("sovereignty_territory",
                        k -> MarkerSet.builder().label("Terytoria Państw").defaultHidden(false).build());
                MarkerSet wallSet = map.getMarkerSets().computeIfAbsent("sovereignty_walls",
                        k -> MarkerSet.builder().label("Mury Obronne").defaultHidden(false).build());

                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    if (!chunk.getWorld().equalsIgnoreCase(worldId) && !worldId.endsWith("/" + chunk.getWorld())) {
                        continue;
                    }

                    double minX = chunk.getX() * 16.0;
                    double minZ = chunk.getZ() * 16.0;
                    double maxX = minX + 16.0;
                    double maxZ = minZ + 16.0;

                    String claimMarkerId = "claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                    Shape shape = Shape.createRect(minX, minZ, maxX, maxZ);

                    ShapeMarker marker = ShapeMarker.builder()
                            .label(nation.getName() + " [" + nation.getTag() + "]")
                            .shape(shape, 64.0f)
                            .fillColor(nationFill)
                            .lineColor(nationStroke)
                            .lineWidth(2)
                            .detail(tooltipHtml)
                            .build();

                    territorySet.put(claimMarkerId, marker);

                    // Mury
                    String wallMarkerId = "wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                    if (nation.getWallTier() == WallTier.NONE) {
                        wallSet.remove(wallMarkerId);
                    } else {
                        Color wallColor = (nation.getWallTier() == WallTier.TIER_3_COPPER_DEEPSLATE) ?
                                new Color(0, 255, 255, 1.0f) :
                                (nation.getWallTier() == WallTier.TIER_2_STONE_WALL ?
                                        new Color(211, 211, 211, 1.0f) : new Color(160, 82, 45, 1.0f));

                        ShapeMarker wallMarker = ShapeMarker.builder()
                                .label("Mur Obronny: " + nation.getWallTier().getDisplayName())
                                .shape(shape, 65.0f)
                                .fillColor(new Color(0, 0, 0, 0.0f))
                                .lineColor(wallColor)
                                .lineWidth(4)
                                .detail("<b>Mur Obronny:</b> " + nation.getWallTier().getDisplayName())
                                .build();

                        wallSet.put(wallMarkerId, wallMarker);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void removeNation(Nation nation) {
        if (!isEnabled()) return;
        try {
            for (BlueMapMap map : blueMapApi.getMaps()) {
                MarkerSet territorySet = map.getMarkerSets().get("sovereignty_territory");
                MarkerSet wallSet = map.getMarkerSets().get("sovereignty_walls");

                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    String claimId = "claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                    String wallId = "wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                    if (territorySet != null) territorySet.remove(claimId);
                    if (wallSet != null) wallSet.remove(wallId);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void updateStructures() {
        if (!isEnabled()) return;

        try {
            for (BlueMapMap map : blueMapApi.getMaps()) {
                String worldId = map.getWorld().getId();
                MarkerSet structureSet = map.getMarkerSets().computeIfAbsent("sovereignty_structures",
                        k -> MarkerSet.builder().label("Sektory Przemysłowe").defaultHidden(false).build());

                for (TerritoryStructure s : territoryManager.getAllStructures()) {
                    String sWorld = s.getLocation().getWorld().getName();
                    if (!sWorld.equalsIgnoreCase(worldId) && !worldId.endsWith("/" + sWorld)) {
                        continue;
                    }

                    Nation owner = nationManager.getNation(s.getNationId());
                    String tag = (owner != null) ? owner.getTag() : "???";

                    String markerId = "struct_" + s.getId();
                    Vector3d pos = new Vector3d(s.getLocation().getX(), s.getLocation().getY() + 1.0, s.getLocation().getZ());

                    POIMarker poi = POIMarker.builder()
                            .label(s.getType().getDisplayName() + " [" + tag + "]")
                            .position(pos)
                            .detail("<b>" + s.getType().getDisplayName() + "</b> [" + tag + "]")
                            .build();

                    structureSet.put(markerId, poi);
                }
            }
        } catch (Throwable ignored) {}
    }

    private Color parseHexToColor(String hex, float alpha) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b, alpha);
        } catch (Exception e) {
            return new Color(51, 136, 255, alpha);
        }
    }

    private String buildNationTooltipHtml(Nation nation) {
        String leaderName = nation.getLeaderUuid() != null ?
                Bukkit.getOfflinePlayer(nation.getLeaderUuid()).getName() : "Brak";
        long remainingCooldown = chunkClaimManager.getRemainingCooldownSeconds(nation);
        String cooldownStr = remainingCooldown > 0 ? chunkClaimManager.formatRemainingTime(remainingCooldown) : "Gotowy do asymilacji!";

        return "<div style='font-family: Arial, sans-serif; padding: 6px; min-width: 200px;'>"
                + "<h2 style='margin: 0; color: " + nation.getMapColorHex() + "; font-size: 16px;'>" + nation.getName() + " [" + nation.getTag() + "]</h2>"
                + "<hr style='margin: 4px 0; border: 0; border-top: 1px solid #ccc;'/>"
                + "<b>Władca:</b> " + leaderName + "<br/>"
                + "<b>Ustrój:</b> " + nation.getRegime().getDisplayName() + "<br/>"
                + "<b>Chunki:</b> " + nation.getClaimedChunks().size() + "<br/>"
                + "<b>Skarbiec (Vault):</b> <span style='color: gold; font-weight: bold;'>" + economyManager.format(nation.getBankBalance()) + "</span><br/>"
                + "<b>Technologia:</b> " + nation.getTechTier().getDisplayName() + "<br/>"
                + "<b>Fortyfikacje:</b> " + nation.getWallTier().getDisplayName() + "<br/>"
                + "<b>Cooldown claimu:</b> " + cooldownStr + "<br/>"
                + (nation.isIndebted() ? "<span style='color: red; font-weight: bold;'>ZADŁUŻENIE (Dzień " + nation.getDebtDays() + "/3)</span>" : "")
                + "</div>";
    }

    @Override
    public void shutdown() {
        if (enableListener != null) BlueMapAPI.unregisterListener(enableListener);
        if (disableListener != null) BlueMapAPI.unregisterListener(disableListener);
        this.blueMapApi = null;
        this.enabled = false;
    }
}
