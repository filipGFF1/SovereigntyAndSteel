package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;

import java.util.ArrayList;
import java.util.List;

public class MapManager {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final ChunkClaimManager chunkClaimManager;
    private final VaultEconomyManager economyManager;

    private final List<MapHook> hooks = new ArrayList<>();
    private SquaremapHook squaremapHook;
    private DynmapHook dynmapHook;
    private BlueMapHook blueMapHook;
    private Pl3xMapHook pl3xMapHook;

    public MapManager(Plugin plugin, NationManager nationManager, TerritoryManager territoryManager,
                      ChunkClaimManager chunkClaimManager, VaultEconomyManager economyManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.chunkClaimManager = chunkClaimManager;
        this.economyManager = economyManager;
    }

    public void init() {
        hooks.clear();

        // 1. Squaremap
        if (Bukkit.getPluginManager().isPluginEnabled("squaremap")) {
            try {
                this.squaremapHook = new SquaremapHook(plugin, nationManager, territoryManager, chunkClaimManager, economyManager);
                this.squaremapHook.init();
                hooks.add(squaremapHook);
            } catch (Throwable t) {
                plugin.getLogger().warning("Nie udało się załadować wsparcia Squaremap: " + t.getMessage());
            }
        }

        // 2. Dynmap
        if (Bukkit.getPluginManager().isPluginEnabled("dynmap")) {
            try {
                this.dynmapHook = new DynmapHook(plugin, nationManager, territoryManager, chunkClaimManager, economyManager);
                this.dynmapHook.init();
                hooks.add(dynmapHook);
            } catch (Throwable t) {
                plugin.getLogger().warning("Nie udało się załadować wsparcia Dynmap: " + t.getMessage());
            }
        }

        // 3. BlueMap
        if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            try {
                this.blueMapHook = new BlueMapHook(plugin, nationManager, territoryManager, chunkClaimManager, economyManager);
                this.blueMapHook.init();
                hooks.add(blueMapHook);
            } catch (Throwable t) {
                plugin.getLogger().warning("Nie udało się załadować wsparcia BlueMap: " + t.getMessage());
            }
        }

        // 4. Pl3xMap
        if (Bukkit.getPluginManager().isPluginEnabled("Pl3xMap")) {
            try {
                this.pl3xMapHook = new Pl3xMapHook(plugin, nationManager, territoryManager, chunkClaimManager, economyManager);
                this.pl3xMapHook.init();
                hooks.add(pl3xMapHook);
            } catch (Throwable t) {
                plugin.getLogger().warning("Nie udało się załadować wsparcia Pl3xMap: " + t.getMessage());
            }
        }

        if (hooks.isEmpty()) {
            plugin.getLogger().info("Nie wykryto żadnego silnika map (Squaremap, Dynmap, BlueMap, Pl3xMap) - integracje mapowe wyłączone.");
        } else {
            plugin.getLogger().info("Aktywne integracje mapowe: " + getEnabledMapNames());
        }
    }

    public void updateAll() {
        for (MapHook hook : hooks) {
            try {
                if (hook.isEnabled()) {
                    hook.updateAll();
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Błąd aktualizacji mapy " + hook.getName() + ": " + t.getMessage());
            }
        }
    }

    public void updateNation(Nation nation) {
        for (MapHook hook : hooks) {
            try {
                if (hook.isEnabled()) {
                    hook.updateNation(nation);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Błąd aktualizacji państwa " + nation.getName() + " na mapie " + hook.getName() + ": " + t.getMessage());
            }
        }
    }

    public void removeNation(Nation nation) {
        for (MapHook hook : hooks) {
            try {
                if (hook.isEnabled()) {
                    hook.removeNation(nation);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Błąd usuwania państwa " + nation.getName() + " z mapy " + hook.getName() + ": " + t.getMessage());
            }
        }
    }

    public void updateStructures() {
        for (MapHook hook : hooks) {
            try {
                if (hook.isEnabled()) {
                    hook.updateStructures();
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Błąd aktualizacji struktur na mapie " + hook.getName() + ": " + t.getMessage());
            }
        }
    }

    public void shutdown() {
        for (MapHook hook : hooks) {
            try {
                hook.shutdown();
            } catch (Throwable ignored) {}
        }
        hooks.clear();
    }

    public boolean hasAnyEnabled() {
        for (MapHook hook : hooks) {
            if (hook.isEnabled()) return true;
        }
        return false;
    }

    public String getEnabledMapNames() {
        List<String> active = new ArrayList<>();
        for (MapHook hook : hooks) {
            if (hook.isEnabled()) {
                active.add(hook.getName());
            }
        }
        return active.isEmpty() ? "Brak" : String.join(", ", active);
    }

    public List<MapHook> getHooks() {
        return hooks;
    }

    public SquaremapHook getSquaremapHook() {
        return squaremapHook;
    }

    public DynmapHook getDynmapHook() {
        return dynmapHook;
    }

    public BlueMapHook getBlueMapHook() {
        return blueMapHook;
    }

    public Pl3xMapHook getPl3xMapHook() {
        return pl3xMapHook;
    }
}
