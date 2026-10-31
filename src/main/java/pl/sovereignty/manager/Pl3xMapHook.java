package pl.sovereignty.manager;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Circle;
import net.pl3x.map.core.markers.marker.Rectangle;
import net.pl3x.map.core.markers.option.Fill;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Popup;
import net.pl3x.map.core.markers.option.Stroke;
import net.pl3x.map.core.markers.option.Tooltip;
import net.pl3x.map.core.world.World;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.model.WallTier;

import java.util.HashMap;
import java.util.Map;

public class Pl3xMapHook implements MapHook {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final ChunkClaimManager chunkClaimManager;
    private final VaultEconomyManager economyManager;
    private boolean enabled = false;

    private final Map<String, SimpleLayer> territoryLayers = new HashMap<>();
    private final Map<String, SimpleLayer> wallLayers = new HashMap<>();
    private final Map<String, SimpleLayer> structureLayers = new HashMap<>();

    public Pl3xMapHook(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager,
                       ChunkClaimManager chunkClaimManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.chunkClaimManager = chunkClaimManager;
        this.economyManager = economyManager;
    }

    @Override
    public String getName() {
        return "Pl3xMap";
    }

    @Override
    public void init() {
        if (Bukkit.getPluginManager().getPlugin("Pl3xMap") == null) {
            plugin.getLogger().info("Pl3xMap nie jest obecny na serwerze - mapa WWW Pl3xMap wyłączona.");
            return;
        }

        try {
            Pl3xMap api = Pl3xMap.api();
            for (World world : api.getWorldRegistry().values()) {
                String worldName = world.getName();

                // 1. Terytoria Państw
                SimpleLayer tLayer = (SimpleLayer) world.getLayerRegistry().get("sovereignty_territory");
                if (tLayer == null) {
                    tLayer = new SimpleLayer("sovereignty_territory", () -> "Terytoria Państw");
                    tLayer.setShowControls(true);
                    tLayer.setDefaultHidden(false);
                    tLayer.setPriority(10);
                    tLayer.setLiveUpdate(true);
                    world.getLayerRegistry().register(tLayer);
                }
                territoryLayers.put(worldName, tLayer);

                // 2. Mury Obronne
                SimpleLayer wLayer = (SimpleLayer) world.getLayerRegistry().get("sovereignty_walls");
                if (wLayer == null) {
                    wLayer = new SimpleLayer("sovereignty_walls", () -> "Mury Obronne");
                    wLayer.setShowControls(true);
                    wLayer.setDefaultHidden(false);
                    wLayer.setPriority(11);
                    wLayer.setLiveUpdate(true);
                    world.getLayerRegistry().register(wLayer);
                }
                wallLayers.put(worldName, wLayer);

                // 3. Sektory Przemysłowe
                SimpleLayer sLayer = (SimpleLayer) world.getLayerRegistry().get("sovereignty_structures");
                if (sLayer == null) {
                    sLayer = new SimpleLayer("sovereignty_structures", () -> "Sektory Przemysłowe");
                    sLayer.setShowControls(true);
                    sLayer.setDefaultHidden(false);
                    sLayer.setPriority(12);
                    sLayer.setLiveUpdate(true);
                    world.getLayerRegistry().register(sLayer);
                }
                structureLayers.put(worldName, sLayer);
            }

            this.enabled = true;
            plugin.getLogger().info("Pomyślnie zintegrowano Sovereignty z silnikiem Pl3xMap!");
            updateAll();
        } catch (Throwable t) {
            plugin.getLogger().warning("Błąd inicjalizacji Pl3xMapHook: " + t.getMessage());
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
        if (!enabled) return;

        try {
            int strokeArgb = parseHexColorArgb(nation.getMapColorHex(), 255);
            int fillArgb = parseHexColorArgb(nation.getMapColorHex(), 90);
            String tooltipHtml = buildNationTooltipHtml(nation);

            for (Map.Entry<String, SimpleLayer> entry : territoryLayers.entrySet()) {
                String worldName = entry.getKey();
                SimpleLayer provider = entry.getValue();

                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    if (!chunk.getWorld().equalsIgnoreCase(worldName)) continue;

                    String markerKey = "claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                    Point p1 = Point.of(chunk.getX() * 16.0, chunk.getZ() * 16.0);
                    Point p2 = Point.of(chunk.getX() * 16.0 + 16.0, chunk.getZ() * 16.0 + 16.0);

                    Options options = Options.builder()
                            .stroke(new Stroke(2, strokeArgb))
                            .fill(new Fill(fillArgb))
                            .tooltip(new Tooltip(nation.getName() + " [" + nation.getTag() + "]"))
                            .popup(new Popup(tooltipHtml))
                            .build();

                    Rectangle rect = Rectangle.of(markerKey, p1, p2);
                    rect.setOptions(options);
                    provider.addMarker(rect);
                }
            }

            // Mury
            updateWallsForNation(nation);
        } catch (Throwable ignored) {}
    }

    private void updateWallsForNation(Nation nation) {
        if (!enabled) return;

        int wallColorArgb = (nation.getWallTier() == WallTier.TIER_3_COPPER_DEEPSLATE) ?
                0xFF00FFFF : (nation.getWallTier() == WallTier.TIER_2_STONE_WALL ? 0xFFD3D3D3 : 0xFFA0522D);

        for (Map.Entry<String, SimpleLayer> entry : wallLayers.entrySet()) {
            String worldName = entry.getKey();
            SimpleLayer provider = entry.getValue();

            for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                if (!chunk.getWorld().equalsIgnoreCase(worldName)) continue;

                String markerKey = "wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ();
                if (nation.getWallTier() == WallTier.NONE) {
                    provider.removeMarker(markerKey);
                    continue;
                }

                Point p1 = Point.of(chunk.getX() * 16.0, chunk.getZ() * 16.0);
                Point p2 = Point.of(chunk.getX() * 16.0 + 16.0, chunk.getZ() * 16.0 + 16.0);

                Options options = Options.builder()
                        .stroke(new Stroke(4, wallColorArgb))
                        .fill(new Fill(false))
                        .tooltip(new Tooltip("Mur Obronny: " + nation.getWallTier().getDisplayName()))
                        .build();

                Rectangle rect = Rectangle.of(markerKey, p1, p2);
                rect.setOptions(options);
                provider.addMarker(rect);
            }
        }
    }

    @Override
    public void removeNation(Nation nation) {
        if (!enabled) return;
        try {
            for (SimpleLayer provider : territoryLayers.values()) {
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    provider.removeMarker("claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                }
            }
            for (SimpleLayer provider : wallLayers.values()) {
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    provider.removeMarker("wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void updateStructures() {
        if (!enabled) return;

        try {
            for (Map.Entry<String, SimpleLayer> entry : structureLayers.entrySet()) {
                String worldName = entry.getKey();
                SimpleLayer provider = entry.getValue();

                for (TerritoryStructure s : territoryManager.getAllStructures()) {
                    if (!s.getLocation().getWorld().getName().equalsIgnoreCase(worldName)) continue;

                    Nation owner = nationManager.getNation(s.getNationId());
                    String tag = (owner != null) ? owner.getTag() : "???";

                    String markerKey = "struct_" + s.getId();
                    Circle circle = Circle.of(markerKey, s.getLocation().getBlockX(), s.getLocation().getBlockZ(), 4.0);

                    Options options = Options.builder()
                            .stroke(new Stroke(2, 0xFFFFA500))
                            .fill(new Fill(0x80FFFF00))
                            .tooltip(new Tooltip(s.getType().getDisplayName() + " [" + tag + "]"))
                            .build();

                    circle.setOptions(options);
                    provider.addMarker(circle);
                }
            }
        } catch (Throwable ignored) {}
    }

    private int parseHexColorArgb(String hex, int alpha) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return ((alpha & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        } catch (Exception e) {
            return ((alpha & 0xFF) << 24) | 0x003388FF;
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
        if (!enabled) return;
        try {
            Pl3xMap api = Pl3xMap.api();
            for (World world : api.getWorldRegistry().values()) {
                world.getLayerRegistry().unregister("sovereignty_territory");
                world.getLayerRegistry().unregister("sovereignty_walls");
                world.getLayerRegistry().unregister("sovereignty_structures");
            }
        } catch (Throwable ignored) {}
    }
}
