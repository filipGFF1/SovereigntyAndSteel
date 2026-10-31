package pl.sovereignty.manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.TerritoryStructure;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RespawnWaveManager implements Listener {
    private final Plugin plugin;
    private final NationManager nationManager;
    private final TerritoryManager territoryManager;
    private final WarManager warManager;

    public static final long WAVE_COOLDOWN_MS = 45_000L; // 45 sekund

    // Gracz -> Czas zwolnienia z kolejki falowej
    private final Map<UUID, Long> waitingQueue = new ConcurrentHashMap<>();
    private final Map<UUID, Location> respawnTargets = new ConcurrentHashMap<>();

    public RespawnWaveManager(Plugin plugin, NationManager nationManager,
                              TerritoryManager territoryManager, WarManager warManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.territoryManager = territoryManager;
        this.warManager = warManager;

        startWaveScheduler();
    }

    private void startWaveScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : waitingQueue.entrySet()) {
                UUID pUuid = entry.getKey();
                Player p = Bukkit.getPlayer(pUuid);
                if (p == null || !p.isOnline()) {
                    waitingQueue.remove(pUuid);
                    respawnTargets.remove(pUuid);
                    continue;
                }

                long remainingMs = entry.getValue() - now;
                if (remainingMs <= 0) {
                    // Fala nadeszła!
                    waitingQueue.remove(pUuid);
                    Location target = respawnTargets.remove(pUuid);

                    p.setGameMode(GameMode.SURVIVAL);
                    if (target != null) {
                        p.teleport(target);
                    }
                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&lFALA ODRODZEŃ!"),
                            ChatColor.translateAlternateColorCodes('&', "&fWracasz do obrony twierdzy!"), 10, 40, 10);
                    p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.2f, 1.0f);
                } else {
                    int secs = (int) (remainingMs / 1000) + 1;
                    p.sendActionBar(ChatColor.translateAlternateColorCodes('&',
                            "&c&l[FALA ODRODZEŃ] &eKolejna fala bojowa przy Bastionie za: &f" + secs + "s"));
                }
            }
        }, 20L, 20L); // co 1 sekundę
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Nation playerNation = nationManager.getNationByPlayer(victim.getUniqueId());
        if (playerNation == null) return;

        Location deathLoc = victim.getLocation();
        Nation landOwner = territoryManager.getNationAt(deathLoc);

        // Sprawdź czy to zgon na terenie ojczystym lub sojuszniczym podczas aktywnej wojny
        if (landOwner != null && warManager.isAtWarWithAny(playerNation.getId())) {
            TerritoryStructure bastion = territoryManager.getNationBastion(playerNation.getId());
            if (bastion != null && bastion.getLocation().getWorld() != null) {
                long releaseTime = System.currentTimeMillis() + WAVE_COOLDOWN_MS;
                waitingQueue.put(victim.getUniqueId(), releaseTime);
                respawnTargets.put(victim.getUniqueId(), bastion.getLocation().clone().add(0.5, 1.0, 0.5));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (waitingQueue.containsKey(player.getUniqueId())) {
            Location target = respawnTargets.get(player.getUniqueId());
            if (target != null) {
                event.setRespawnLocation(target);
            }

            // Przełącz w tryb spectator na czas fali odrodzeń
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (waitingQueue.containsKey(player.getUniqueId())) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&c&l[FALA ODRODZEŃ] &ePodczas oblężenia odradzasz się falami przy Bastionie (kolejka 45s). Nie możesz natychmiast wracać do walki!"));
                }
            }, 2L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (waitingQueue.containsKey(p.getUniqueId())) {
            p.setGameMode(GameMode.SURVIVAL);
            waitingQueue.remove(p.getUniqueId());
            respawnTargets.remove(p.getUniqueId());
        }
    }

    public void cleanup() {
        for (UUID pUuid : waitingQueue.keySet()) {
            Player p = Bukkit.getPlayer(pUuid);
            if (p != null && p.isOnline()) {
                p.setGameMode(GameMode.SURVIVAL);
            }
        }
        waitingQueue.clear();
        respawnTargets.clear();
    }
}
