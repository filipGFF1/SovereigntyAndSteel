package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.NationClaimChunk;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.model.WallTier;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;
import java.util.*;

public class SquaremapHook implements MapHook {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final ChunkClaimManager chunkClaimManager;
    private final VaultEconomyManager economyManager;
    private boolean enabled = false;

    private final Map<String, SimpleLayerProvider> territoryProviders = new HashMap<>();
    private final Map<String, SimpleLayerProvider> wallProviders = new HashMap<>();
    private final Map<String, SimpleLayerProvider> structureProviders = new HashMap<>();

    public SquaremapHook(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager,
                         ChunkClaimManager chunkClaimManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.chunkClaimManager = chunkClaimManager;
        this.economyManager = economyManager;
    }

    public void init() {
        initSquaremap();
    }

    private void initSquaremap() {
        if (Bukkit.getPluginManager().getPlugin("squaremap") == null) {
            plugin.getLogger().info("Squaremap nie jest obecny na serwerze - mapa WWW wyłączona.");
            return;
        }

        try {
            Squaremap api = SquaremapProvider.get();
            for (MapWorld mapWorld : api.mapWorlds()) {
                String worldName = mapWorld.identifier().asString();

                // 1. Warstwa terytoriów państw
                SimpleLayerProvider tProvider = SimpleLayerProvider.builder("Terytoria Państw")
                        .showControls(true)
                        .defaultHidden(false)
                        .build();
                Key tKey = Key.of("sovereignty_territory_" + worldName);
                if (!mapWorld.layerRegistry().hasEntry(tKey)) {
                    mapWorld.layerRegistry().register(tKey, tProvider);
                }
                territoryProviders.put(worldName, tProvider);

                // 2. Warstwa murów obronnych
                SimpleLayerProvider wProvider = SimpleLayerProvider.builder("Mury Obronne")
                        .showControls(true)
                        .defaultHidden(false)
                        .build();
                Key wKey = Key.of("sovereignty_walls_" + worldName);
                if (!mapWorld.layerRegistry().hasEntry(wKey)) {
                    mapWorld.layerRegistry().register(wKey, wProvider);
                }
                wallProviders.put(worldName, wProvider);

                // 3. Warstwa struktur przemysłowych
                SimpleLayerProvider sProvider = SimpleLayerProvider.builder("Sektory Przemysłowe")
                        .showControls(true)
                        .defaultHidden(false)
                        .build();
                Key sKey = Key.of("sovereignty_structures_" + worldName);
                if (!mapWorld.layerRegistry().hasEntry(sKey)) {
                    mapWorld.layerRegistry().register(sKey, sProvider);
                }
                structureProviders.put(worldName, sProvider);
            }

            this.enabled = true;
            plugin.getLogger().info("Pomyślnie zintegrowano Sovereignty z silnikiem Squaremap!");
            updateAll();
        } catch (Throwable t) {
            plugin.getLogger().warning("Nie udało się załadować integracji Squaremap: " + t.getMessage());
            this.enabled = false;
        }
    }

    @Override
    public String getName() {
        return "Squaremap";
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
    public void removeNation(Nation nation) {
        if (!enabled) return;
        try {
            for (Map.Entry<String, SimpleLayerProvider> entry : territoryProviders.entrySet()) {
                SimpleLayerProvider provider = entry.getValue();
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    provider.removeMarker(Key.of("claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ()));
                }
            }
            for (Map.Entry<String, SimpleLayerProvider> entry : wallProviders.entrySet()) {
                SimpleLayerProvider provider = entry.getValue();
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    provider.removeMarker(Key.of("wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ()));
                }
            }
        } catch (Throwable ignored) {}
    }

    public void updateNation(Nation nation) {
        if (!enabled) return;

        try {
            Color nationColor = parseHexColor(nation.getMapColorHex());
            Color fillColor = new Color(nationColor.getRed(), nationColor.getGreen(), nationColor.getBlue(), 80);
            Color strokeColor = nationColor;

            // Rysowanie każdego chunka nacji
            for (Map.Entry<String, SimpleLayerProvider> entry : territoryProviders.entrySet()) {
                String worldName = entry.getKey();
                SimpleLayerProvider provider = entry.getValue();

                int idx = 0;
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    if (!chunk.getWorld().equalsIgnoreCase(worldName)) continue;

                    Key markerKey = Key.of("claim_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                    Point p1 = Point.of(chunk.getX() * 16, chunk.getZ() * 16);
                    Point p2 = Point.of(chunk.getX() * 16 + 16, chunk.getZ() * 16 + 16);

                    String tooltipHtml = buildNationTooltipHtml(nation);
                    MarkerOptions options = MarkerOptions.builder()
                            .fillColor(fillColor)
                            .strokeColor(strokeColor)
                            .strokeWeight(2)
                            .clickTooltip(tooltipHtml)
                            .hoverTooltip("Państwo: " + nation.getName() + " [" + nation.getTag() + "]")
                            .build();

                    provider.addMarker(markerKey, Marker.rectangle(p1, p2).markerOptions(options));
                    idx++;
                }
            }

            // Rysowanie murów obronnych nacji
            updateWallsForNation(nation);
        } catch (Throwable ignored) {}
    }

    private void updateWallsForNation(Nation nation) {
        if (!enabled) return;

        for (Map.Entry<String, SimpleLayerProvider> entry : wallProviders.entrySet()) {
            String worldName = entry.getKey();
            SimpleLayerProvider provider = entry.getValue();

            if (nation.getWallTier() == WallTier.NONE) {
                // Usuń markery murów
                for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                    if (chunk.getWorld().equalsIgnoreCase(worldName)) {
                        provider.removeMarker(Key.of("wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ()));
                    }
                }
                continue;
            }

            Color wallColor = (nation.getWallTier() == WallTier.TIER_3_COPPER_DEEPSLATE) ? Color.CYAN :
                    (nation.getWallTier() == WallTier.TIER_2_STONE_WALL ? Color.LIGHT_GRAY : new Color(160, 82, 45));

            for (NationClaimChunk chunk : nation.getClaimedChunks()) {
                if (!chunk.getWorld().equalsIgnoreCase(worldName)) continue;

                Key markerKey = Key.of("wall_" + nation.getId() + "_" + chunk.getX() + "_" + chunk.getZ());
                Point p1 = Point.of(chunk.getX() * 16, chunk.getZ() * 16);
                Point p2 = Point.of(chunk.getX() * 16 + 16, chunk.getZ() * 16 + 16);

                MarkerOptions options = MarkerOptions.builder()
                        .strokeColor(wallColor)
                        .strokeWeight(4)
                        .hoverTooltip("Mur Obronny: " + nation.getWallTier().getDisplayName())
                        .build();

                provider.addMarker(markerKey, Marker.rectangle(p1, p2).markerOptions(options));
            }
        }
    }

    public void updateStructures() {
        if (!enabled) return;

        for (Map.Entry<String, SimpleLayerProvider> entry : structureProviders.entrySet()) {
            String worldName = entry.getKey();
            SimpleLayerProvider provider = entry.getValue();

            for (TerritoryStructure s : territoryManager.getAllStructures()) {
                if (!s.getLocation().getWorld().getName().equalsIgnoreCase(worldName)) continue;

                Key markerKey = Key.of("struct_" + s.getId());
                Point p = Point.of(s.getLocation().getBlockX(), s.getLocation().getBlockZ());

                Nation owner = nationManager.getNation(s.getNationId());
                String tag = (owner != null) ? owner.getTag() : "???";

                MarkerOptions options = MarkerOptions.builder()
                        .hoverTooltip(s.getType().getDisplayName() + " [" + tag + "]")
                        .build();

                provider.addMarker(markerKey, Marker.circle(p, 4.0).markerOptions(options));
            }
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

    private Color parseHexColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return new Color(51, 136, 255);
        }
    }

    public void shutdown() {
        if (!enabled) return;
        try {
            Squaremap api = SquaremapProvider.get();
            for (MapWorld mapWorld : api.mapWorlds()) {
                String worldName = mapWorld.identifier().asString();
                mapWorld.layerRegistry().unregister(Key.of("sovereignty_territory_" + worldName));
                mapWorld.layerRegistry().unregister(Key.of("sovereignty_walls_" + worldName));
                mapWorld.layerRegistry().unregister(Key.of("sovereignty_structures_" + worldName));
            }
        } catch (Throwable ignored) {}
    }
}
