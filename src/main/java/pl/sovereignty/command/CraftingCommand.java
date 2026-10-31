package pl.sovereignty.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.sovereignty.gui.RecipeBookGui;
import pl.sovereignty.util.CustomItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CraftingCommand implements CommandExecutor, TabCompleter {
    private final CustomItemRegistry items;

    public CraftingCommand(CustomItemRegistry items) {
        this.items = items;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Tylko gracze mogą przeglądać receptury craftingu!");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            String recipeId = switch (sub) {
                case "baraki", "barracks" -> "barracks_core";
                case "tartak", "sawmill" -> "sawmill_totem";
                case "kopalnia", "mining" -> "mining_head";
                case "taran", "ram" -> "crate_ram";
                case "balista", "ballista" -> "crate_ballista";
                case "mozdzierz", "mortar" -> "crate_mortar";
                case "stol", "rada", "council" -> "war_council_table";
                case "klucz", "wrench" -> "engineering_wrench";
                case "bulawa", "mace" -> "marshal_mace";
                case "rog", "horn" -> "war_horn";
                case "skarbiec", "vault", "sklad" -> "national_vault";
                case "farma", "farm", "centrum", "rolnictwo" -> "state_farm";
                default -> null;
            };

            if (recipeId != null) {
                RecipeBookGui.openRecipeDetail(player, recipeId, items);
                return true;
            }
        }

        RecipeBookGui.openMainMenu(player, items);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("baraki", "tartak", "kopalnia", "farma", "skarbiec", "taran", "balista", "mozdzierz", "stol", "klucz", "bulawa", "rog");
        }
        return new ArrayList<>();
    }
}
