package pl.sovereignty.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.sovereignty.model.Nation;
import pl.sovereignty.model.StructureType;
import pl.sovereignty.model.TerritoryStructure;
import pl.sovereignty.model.VaultTier;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VaultManager {
    private final Plugin plugin;
    private final TerritoryManager territoryManager;
    private final Map<UUID, List<Inventory>> nationVaultPages = new HashMap<>();

    public static final String VAULT_TITLE_PREFIX = "§6§lSkarbiec: §e[";

    public VaultManager(Plugin plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
    }

    public Inventory getVaultInventory(Nation nation) {
        return getVaultInventory(nation, 0);
    }

    public Inventory getVaultInventory(Nation nation, int pageIndex) {
        if (nation == null) return null;
        if (pageIndex < 0) pageIndex = 0;
        int maxPages = nation.getVaultTier().getPages();
        if (pageIndex >= maxPages) {
            pageIndex = maxPages - 1;
        }

        final int targetPage = pageIndex;
        List<Inventory> pages = nationVaultPages.computeIfAbsent(nation.getId(), id -> new ArrayList<>());

        while (pages.size() <= targetPage) {
            int pIndex = pages.size();
            String cleanName = nation.getName().length() > 8 ? nation.getName().substring(0, 8) : nation.getName();
            String title = VAULT_TITLE_PREFIX + nation.getTag() + "] #" + (pIndex + 1) + " §8" + cleanName;
            if (title.length() > 32) {
                title = title.substring(0, 32);
            }
            pages.add(Bukkit.createInventory(null, 54, title));
        }

        return pages.get(targetPage);
    }

    public boolean hasNationalVault(Nation nation) {
        if (nation == null) return false;
        for (TerritoryStructure s : territoryManager.getNationStructures(nation.getId())) {
            if (s.getType() == StructureType.NATIONAL_VAULT) {
                return true;
            }
        }
        return false;
    }

    public TerritoryStructure getNationalVaultStructure(Nation nation) {
        if (nation == null) return null;
        for (TerritoryStructure s : territoryManager.getNationStructures(nation.getId())) {
            if (s.getType() == StructureType.NATIONAL_VAULT) {
                return s;
            }
        }
        return null;
    }

    /**
     * Bezpośrednie przekazanie urobku/plonów do Skarbca Krajowego.
     * Przesyła surowce po kolei do odblokowanych stron (skrzyń) skarbca.
     * W Tier 5 przy włączonym Auto-Sell nadmiar jest natychmiast spieniężany do banku!
     */
    public boolean addItemToVault(Nation nation, ItemStack item) {
        if (item == null || item.getAmount() <= 0) return false;
        TerritoryStructure vaultStruct = getNationalVaultStructure(nation);
        if (vaultStruct == null) {
            return false;
        }

        int maxPages = nation.getVaultTier().getPages();
        ItemStack currentItem = item;

        // 1. Zapełnianie kolejnych odblokowanych skrzyń skarbca
        for (int p = 0; p < maxPages; p++) {
            Inventory inv = getVaultInventory(nation, p);
            HashMap<Integer, ItemStack> leftover = inv.addItem(currentItem);
            if (leftover.isEmpty()) {
                return true;
            }
            currentItem = leftover.values().iterator().next();
        }

        // 2. Jeśli wszystkie skrzynie są pełne:
        if (currentItem != null && currentItem.getAmount() > 0) {
            // Moduł AUTO-SELL na poziomie Tier 5
            if (nation.getVaultTier().hasAutoSell() && nation.isVaultAutoSell()) {
                double earned = sellItem(nation, currentItem);
                if (earned > 0) {
                    Location loc = vaultStruct.getLocation().clone().add(0.5, 1.0, 0.5);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 4, 0.2, 0.2, 0.2, 0);
                    }
                    return true;
                }
            }

            // Standardowe upuszczenie nadmiaru przy skarbcu
            Location loc = vaultStruct.getLocation().clone().add(0.5, 1.0, 0.5);
            if (loc.getWorld() != null) {
                loc.getWorld().dropItemNaturally(loc, currentItem);
            }
        }
        return true;
    }

    /**
     * Tabela wyceny surowców dla modułu Auto-Sell.
     */
    public double getItemSellPrice(Material mat) {
        String name = mat.name();

        // Kruszce i minerały
        if (name.contains("COBBLESTONE") || name.contains("DEEPSLATE") || name.contains("STONE") || name.contains("ANDESITE") || name.contains("DIORITE") || name.contains("GRANITE")) return 0.03;
        if (name.contains("COAL") || name.contains("CHARCOAL")) return 0.25;
        if (name.contains("COPPER")) return 0.35;
        if (name.contains("IRON")) return 0.65;
        if (name.contains("GOLD")) return 1.30;
        if (name.contains("REDSTONE")) return 0.15;
        if (name.contains("LAPIS")) return 0.25;
        if (name.contains("EMERALD")) return 2.50;
        if (name.contains("DIAMOND")) return 5.50;
        if (name.contains("NETHERITE")) return 35.00;

        // Drewno i roślinność (znerfiony auto-sell)
        if (name.endsWith("_LOG") || name.endsWith("_WOOD")) return 0.08;
        if (name.endsWith("_PLANKS")) return 0.02;
        if (name.endsWith("_SAPLING") || name.contains("PROPAGULE")) return 0.03;
        if (name.equals("BAMBOO")) return 0.01;
        if (name.equals("APPLE")) return 0.10;
        if (name.equals("STICK")) return 0.01;

        // Rolnictwo i żywność
        if (name.equals("WHEAT")) return 0.20;
        if (name.equals("BREAD")) return 0.50;
        if (name.equals("CARROT") || name.equals("POTATO") || name.equals("BEETROOT")) return 0.15;
        if (name.equals("SUGAR_CANE") || name.equals("MELON_SLICE") || name.equals("PUMPKIN")) return 0.15;

        // Koralowce i rzadkości
        if (name.contains("CORAL")) return 0.80;
        if (name.contains("FROGLIGHT")) return 1.20;

        return 0.05; // Domyślna minimalna stawka auto-sell
    }

    /**
     * Spienięża dany przedmiot i zasila konto bankowe państwa.
     */
    public double sellItem(Nation nation, ItemStack item) {
        if (item == null || item.getAmount() <= 0) return 0.0;
        double basePrice = getItemSellPrice(item.getType());
        double regimeMultiplier = nation.isIndebted() ? 1.0 : nation.getRegime().getProductionMultiplier();
        double totalEarned = basePrice * item.getAmount() * regimeMultiplier;

        nation.depositBank(totalEarned);
        return totalEarned;
    }

    /**
     * Spienięża zawartość wybranej skrzyni skarbca i zwraca zarobioną kwotę.
     */
    public double sellInventoryContents(Nation nation, Inventory inv) {
        if (nation == null || inv == null) return 0.0;
        double totalEarned = 0.0;
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() != Material.AIR) {
                totalEarned += sellItem(nation, stack);
                inv.setItem(i, null);
            }
        }
        return totalEarned;
    }

    /**
     * Spienięża zawartość WSZYSTKICH odblokowanych skrzyń skarbca nacji.
     */
    public double sellAllVaultContents(Nation nation) {
        if (nation == null) return 0.0;
        int maxPages = nation.getVaultTier().getPages();
        double totalEarned = 0.0;
        for (int p = 0; p < maxPages; p++) {
            Inventory inv = getVaultInventory(nation, p);
            totalEarned += sellInventoryContents(nation, inv);
        }
        return totalEarned;
    }

    public int getUsedSlots(Inventory inv) {
        if (inv == null) return 0;
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    public int getTotalUsedSlots(Nation nation) {
        List<Inventory> pages = nationVaultPages.get(nation.getId());
        if (pages == null) return 0;
        int count = 0;
        int maxPages = nation.getVaultTier().getPages();
        for (int i = 0; i < Math.min(pages.size(), maxPages); i++) {
            count += getUsedSlots(pages.get(i));
        }
        return count;
    }

    public void openVault(Player player, Nation nation) {
        openVault(player, nation, 0);
    }

    public void openVault(Player player, Nation nation, int pageIndex) {
        if (!hasNationalVault(nation)) {
            player.sendMessage(ChatColor.RED + "Twoje państwo nie posiada postawionego Skarbca Krajowego!");
            return;
        }

        int maxPages = nation.getVaultTier().getPages();
        if (pageIndex < 0 || pageIndex >= maxPages) {
            pageIndex = 0;
        }

        Inventory inv = getVaultInventory(nation, pageIndex);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.8f, 1.0f);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6&l[SKARBIEC KRAJOWY] &fOtworzono Skrzynię &e#" + (pageIndex + 1) + " &7(" + (pageIndex + 1) + "/" + maxPages + ") państwa &e"
                        + nation.getName() + " &7[" + nation.getTag() + "]&f!"));
    }

    public void saveVaults() {
        File file = new File(plugin.getDataFolder(), "vaults.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, List<Inventory>> entry : nationVaultPages.entrySet()) {
            UUID nationId = entry.getKey();
            List<Inventory> pages = entry.getValue();

            for (int p = 0; p < pages.size(); p++) {
                Inventory inv = pages.get(p);
                ItemStack[] contents = inv.getContents();
                List<Map<String, Object>> serialized = new ArrayList<>();
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                        Map<String, Object> map = contents[i].serialize();
                        map.put("__slot__", i);
                        serialized.add(map);
                    }
                }
                config.set("vaults." + nationId.toString() + ".page_" + p, serialized);
            }
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd zapisu vaults.yml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadVaults() {
        File file = new File(plugin.getDataFolder(), "vaults.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection("vaults");
        if (sec == null) return;

        nationVaultPages.clear();
        for (String idStr : sec.getKeys(false)) {
            try {
                UUID nationId = UUID.fromString(idStr);
                List<Inventory> pages = new ArrayList<>();

                if (sec.isConfigurationSection(idStr)) {
                    ConfigurationSection nationSec = sec.getConfigurationSection(idStr);
                    if (nationSec != null) {
                        List<String> pageKeys = new ArrayList<>(nationSec.getKeys(false));
                        pageKeys.sort(Comparator.comparingInt(k -> {
                            try {
                                return Integer.parseInt(k.replace("page_", ""));
                            } catch (Exception e) {
                                return 0;
                            }
                        }));

                        for (String pageKey : pageKeys) {
                            List<?> list = nationSec.getList(pageKey);
                            if (list == null) continue;

                            int pageNum = 1;
                            try {
                                pageNum = Integer.parseInt(pageKey.replace("page_", "")) + 1;
                            } catch (Exception ignored) {}

                            Inventory inv = Bukkit.createInventory(null, 54, VAULT_TITLE_PREFIX + idStr.substring(0, 4) + "] #" + pageNum);
                            for (Object obj : list) {
                                if (obj instanceof Map<?, ?> map) {
                                    Map<String, Object> rawMap = new HashMap<>((Map<String, Object>) obj);
                                    Object slotObj = rawMap.remove("__slot__");
                                    int slot = slotObj instanceof Number ? ((Number) slotObj).intValue() : -1;
                                    ItemStack stack = ItemStack.deserialize(rawMap);
                                    if (slot >= 0 && slot < 54) {
                                        inv.setItem(slot, stack);
                                    } else {
                                        inv.addItem(stack);
                                    }
                                }
                            }
                            pages.add(inv);
                        }
                    }
                } else {
                    List<?> list = sec.getList(idStr);
                    if (list != null) {
                        Inventory inv = Bukkit.createInventory(null, 54, VAULT_TITLE_PREFIX + idStr.substring(0, 4) + "] #1");
                        for (Object obj : list) {
                            if (obj instanceof Map<?, ?> map) {
                                Map<String, Object> rawMap = new HashMap<>((Map<String, Object>) obj);
                                Object slotObj = rawMap.remove("__slot__");
                                int slot = slotObj instanceof Number ? ((Number) slotObj).intValue() : -1;
                                ItemStack stack = ItemStack.deserialize(rawMap);
                                if (slot >= 0 && slot < 54) {
                                    inv.setItem(slot, stack);
                                } else {
                                    inv.addItem(stack);
                                }
                            }
                        }
                        pages.add(inv);
                    }
                }

                nationVaultPages.put(nationId, pages);
            } catch (Exception e) {
                plugin.getLogger().warning("Błąd wczytywania skarbca dla " + idStr + ": " + e.getMessage());
            }
        }
    }
}
