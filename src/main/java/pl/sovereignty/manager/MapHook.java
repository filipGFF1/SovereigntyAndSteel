package pl.sovereignty.manager;

import pl.sovereignty.model.Nation;

public interface MapHook {
    String getName();
    void init();
    void shutdown();
    void updateAll();
    void updateNation(Nation nation);
    void removeNation(Nation nation);
    void updateStructures();
    boolean isEnabled();
}
