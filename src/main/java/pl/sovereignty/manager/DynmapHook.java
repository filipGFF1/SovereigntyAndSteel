package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.model.WallTier;

import java.util.*;

public class DynmapHook implements MapHook {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final ChunkClaimManager chunkClaimManager;
    private final VaultEconomyManager economyManager;
    private boolean enabled = false;

    private MarkerAPI markerApi;
    private MarkerSet territorySet;
    private MarkerSet wallSet;
    private MarkerSet structureSet;

    public DynmapHook(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager,
                      ChunkClaimManager chunkClaimManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.chunkClaimManager = chunkClaimManager;
        this.economyManager = economyManager;
    }

    @Override
    public String getName() {
        return "Dynmap";
    }

    @Override
    public void init() {
        Plugin dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null || !dynmapPlugin.isEnabled()) {
            plugin.getLogger().info("Dynmap nie jest obecny na serwerze - warstwa Dynmap wyłączona.");
            return;
        }

        try {
            DynmapAPI dynmapApi = (DynmapAPI) dynmapPlugin;
            this.markerApi = dynmapApi.getMarkerAPI();
            if (markerApi == null) {
                plugin.getLogger().warning("Nie udało się uzyskać MarkerAPI z pluginu Dynmap!");
                return;
            }

            // 1. Terytoria Państw
            this.territorySet = markerApi.getMarkerSet("sovereignty_territory");
            if (territorySet == null) {
                territorySet = markerApi.createMarkerSet("sovereignty_territory", "Terytoria Państw", null, false);
            } else {
                territorySet.setMarkerSetLabel("Terytoria Państw");
            }
            if (territorySet != null) {
                territorySet.setHideByDefault(false);
                territorySet.setLayerPriority(10);
            }

            // 2. Mury Obronne
            this.wallSet = markerApi.getMarkerSet("sovereignty_walls");
            if (wallSet == null) {
                wallSet = markerApi.createMarkerSet("sovereignty_walls", "Mury Obronne", null, false);
            } else {
                wallSet.setMarkerSetLabel("Mury Obronne");
            }
            if (wallSet != null) {
                wallSet.setHideByDefault(false);
                wallSet.setLayerPriority(11);
            }

            // 3. Sektory Przemysłowe
            this.structureSet = markerApi.getMarkerSet("sovereignty_structures");
            if (structureSet == null) {
                structureSet = markerApi.createMarkerSet("sovereignty_structures", "Sektory Przemysłowe", null, false);
            } else {
                structureSet.setMarkerSetLabel("Sektory Przemysłowe");
            }
            if (structureSet != null) {
                structureSet.setHideByDefault(false);
                structureSet.setLayerPriority(12);
            }

            this.enabled = true;
            plugin.getLogger().info("Pomyślnie zintegrowano Sovereignty z silnikiem Dynmap!");
            updateAll();
        } catch (Throwable t) {
            plugin.getLogger().warning("Błąd inicjalizacji DynmapHook: " + t.getMessage());
            this.enabled = false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void updateAll() {
        if (!enabled) return;
        for (Nation nation : nationManager.getAllNations()) {
            updateNation(nation);
        }
        updateStructures();
    }

    @Override
    public void updateNation(Nation nation) {
        if (!enabled || territorySet == null) return;

        try {
            int colorInt = parseColorInt(nation.getMapColorHex());
            String tooltipHtml = buildNationTooltipHtml(nation);

            // Obsługa chunków
            for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                String markerId = "claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                double[] x = new double[]{ chunk.getX() * 16.0, chunk.getX() * 16.0 + 16.0 };
                double[] z = new double[]{ chunk.getZ() * 16.0, chunk.getZ() * 16.0 + 16.0 };

                AreaMarker marker = territorySet.findAreaMarker(markerId);
                if (marker == null) {
                    marker = territorySet.createAreaMarker(markerId, nation.getName() + " [" + nation.getTag() + "]",
                            false, chunk.getWorld(), x, z, false);
                } else {
                    marker.setCornerLocations(x, z);
                }

                if (marker != null) {
                    marker.setLineStyle(2, 1.0, colorInt);
                    marker.setFillStyle(0.35, colorInt);
                    marker.setDescription(tooltipHtml);
                }
            }

            // Obsługa murów
            updateWallsForNation(nation);
        } catch (Throwable ignored) {}
    }

    private void updateWallsForNation(Nation nation) {
        if (!enabled || wallSet == null) return;

        int wallColorInt = (nation.getWallTier() == WallTier.TIER_3_COPPER_DEEPSLATE) ? 0x00FFFF :
                (nation.getWallTier() == WallTier.TIER_2_STONE_WALL ? 0xD3D3D3 : 0xA0522D);

        for (NationClaimChunk chunk : nation.getClaimedChunks()) {
            String wallMarkerId = "wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
            AreaMarker marker = wallSet.findAreaMarker(wallMarkerId);

            if (nation.getWallTier() == WallTier.NONE) {
                if (marker != null) marker.deleteMarker();
                continue;
            }

            double[] x = new double[]{ chunk.getX() * 16.0, chunk.getX() * 16.0 + 16.0 };
            double[] z = new double[]{ chunk.getZ() * 16.0, chunk.getZ() * 16.0 + 16.0 };

            if (marker == null) {
                marker = wallSet.createAreaMarker(wallMarkerId, "Mur: " + nation.getWallTier().getDisplayName(),
                        false, chunk.getWorld(), x, z, false);
            } else {
                marker.setCornerLocations(x, z);
            }

            if (marker != null) {
                marker.setLineStyle(4, 1.0, wallColorInt);
                marker.setFillStyle(0.0, 0x000000);
                marker.setDescription("<b>Mur Obronny:</b> " + nation.getWallTier().getDisplayName());
            }
        }
    }

    @Override
    public void removeNation(Nation nation) {
        if (!enabled) return;
        try {
            if (territorySet != null) {
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    AreaMarker m = territorySet.findAreaMarker("claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                    if (m != null) m.deleteMarker();
                }
            }
            if (wallSet != null) {
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    AreaMarker m = wallSet.findAreaMarker("wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                    if (m != null) m.deleteMarker();
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void updateStructures() {
        if (!enabled || structureSet == null) return;

        try {
            for (TerritoryStructure s : territoryManager.getAllStructures()) {
                String markerId = "struct_" + s.getId();
                CircleMarker marker = structureSet.findCircleMarker(markerId);

                Nation owner = nationManager.getNation(s.getNationId());
                String tag = (owner != null) ? owner.getTag() : "???";
                String worldName = s.getLocation().getWorld().getName();
                double x = s.getLocation().getX();
                double y = s.getLocation().getY();
                double z = s.getLocation().getZ();

                if (marker == null) {
                    marker = structureSet.createCircleMarker(markerId, s.getType().getDisplayName() + " [" + tag + "]",
                            false, worldName, x, y, z, 4.0, 4.0, false);
                } else {
                    marker.setCenter(worldName, x, y, z);
                }

                if (marker != null) {
                    marker.setLineStyle(2, 0.9, 0xFFA500);
                    marker.setFillStyle(0.5, 0xFFFF00);
                    marker.setDescription("<b>" + s.getType().getDisplayName() + "</b> [" + tag + "]<br/>Pozycja: " + (int)x + ", " + (int)y + ", " + (int)z);
                }
            }
        } catch (Throwable ignored) {}
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

    private int parseColorInt(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0x3388FF;
        }
    }

    @Override
    public void shutdown() {
        if (!enabled) return;
        try {
            if (territorySet != null) territorySet.deleteMarkerSet();
            if (wallSet != null) wallSet.deleteMarkerSet();
            if (structureSet != null) structureSet.deleteMarkerSet();
        } catch (Throwable ignored) {}
    }
}
