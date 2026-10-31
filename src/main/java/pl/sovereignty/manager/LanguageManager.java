package pl.sovereignty.manager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.sovereignty.SovereigntyPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Multi-language Localization Engine for Sovereignty & Steel.
 * Supports English (en), Polish (pl), and Spanish (es).
 * Defaults to English (en).
 *
 * Official Author: filipGFF1
 */
public class LanguageManager {

    public static final String AUTHOR = "filipGFF1";
    public static final String BUILD_SIGNATURE = "SOVEREIGNTY-STEEL-v1.0.0-FILIPGFF1-ORIGINAL";
    public static final String COPYRIGHT = "© 2026 filipGFF1. All rights reserved.";

    private final SovereigntyPlugin plugin;
    private FileConfiguration messages;
    private FileConfiguration fallbackMessages;
    private String currentLanguage = "en";

    public LanguageManager(SovereigntyPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        saveDefaultLanguageFiles();
        loadLanguage();
    }

    private void saveDefaultLanguageFiles() {
        String[] languages = {"messages_en.yml", "messages_pl.yml", "messages_es.yml"};
        for (String langFile : languages) {
            File file = new File(plugin.getDataFolder(), langFile);
            if (!file.exists()) {
                plugin.saveResource(langFile, false);
            }
        }
    }

    public void loadLanguage() {
        this.currentLanguage = plugin.getConfig().getString("language", "en").toLowerCase();
        if (!currentLanguage.equals("en") && !currentLanguage.equals("pl") && !currentLanguage.equals("es")) {
            plugin.getLogger().warning("Unsupported language '" + currentLanguage + "'! Falling back to 'en' (English).");
            currentLanguage = "en";
        }

        File langFile = new File(plugin.getDataFolder(), "messages_" + currentLanguage + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().info("Generating default " + langFile.getName() + "...");
            plugin.saveResource("messages_" + currentLanguage + ".yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(langFile);

        // Always load English as internal fallback
        InputStream fallbackStream = plugin.getResource("messages_en.yml");
        if (fallbackStream != null) {
            this.fallbackMessages = YamlConfiguration.loadConfiguration(new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
        }

        plugin.getLogger().info("Successfully loaded language: " + currentLanguage.toUpperCase() + " (Author: " + AUTHOR + ")");
    }

    public String getRaw(String path, String def) {
        if (messages != null && messages.contains(path)) {
            return messages.getString(path, def);
        }
        if (fallbackMessages != null && fallbackMessages.contains(path)) {
            return fallbackMessages.getString(path, def);
        }
        return def;
    }

    public String getRaw(String path) {
        return getRaw(path, path);
    }

    public String get(String path) {
        return color(getRaw(path));
    }

    public String get(String path, String def) {
        return color(getRaw(path, def));
    }

    public String format(String path, Object... replacements) {
        String msg = get(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String key = "{" + replacements[i] + "}";
            String val = String.valueOf(replacements[i + 1]);
            msg = msg.replace(key, val);
        }
        return msg;
    }

    public void send(CommandSender sender, String path, Object... replacements) {
        sender.sendMessage(format(path, replacements));
    }

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void sendAuthorInfo(CommandSender sender) {
        sender.sendMessage(color(
                "\n&6&l====================================================\n" +
                " &6&l⚔ SOVEREIGNTY & STEEL: ISLAND REALM ⚔\n" +
                " &7Official Minecraft 1.21.4 Island & Nation Experience\n" +
                " \n" +
                " &eOfficial Author: &f&l" + AUTHOR + "\n" +
                " &eActive Language: &a" + currentLanguage.toUpperCase() + " &7[en, pl, es]\n" +
                " &eBuild Signature: &b" + BUILD_SIGNATURE + "\n" +
                " &7" + COPYRIGHT + "\n" +
                " &6&l====================================================\n"
        ));
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
