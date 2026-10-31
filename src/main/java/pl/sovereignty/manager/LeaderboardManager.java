package pl.sovereignty.manager;

import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardManager {
    private final Plugin plugin;
    private final NationManager nationManager;

    public LeaderboardManager(Plugin plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
    }

    public List<Nation> getTopGdp(int limit) {
        List<Nation> list = new ArrayList<>(nationManager.getAllNations());
        list.sort(Comparator.comparingDouble(Nation::getGdp).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Nation> getTopMilitary(int limit) {
        List<Nation> list = new ArrayList<>(nationManager.getAllNations());
        list.sort(Comparator.comparingInt(Nation::getMilitaryScore).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Nation> getTopPirateSlayers(int limit) {
        List<Nation> list = new ArrayList<>(nationManager.getAllNations());
        list.sort(Comparator.comparingInt(Nation::getPiratesDefeated).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Nation> getTopWelfare(int limit) {
        List<Nation> list = new ArrayList<>(nationManager.getAllNations());
        list.sort(Comparator.comparingDouble(Nation::getWelfareScore).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }
}
