package com.tomkeuper.bedwars.commands.mapselector;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.mapselector.MapSelectorConfig;
import com.tomkeuper.bedwars.mapselector.MapSelectorUtils;
import com.tomkeuper.bedwars.mapselector.menu.SelectorMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code /bwmenu <mode>} opens the map selector for an arena group.
 * <p>
 * The mode argument accepts several groups separated by commas, which lets one menu cover more than one mode.
 */
public class MapSelectorCommand extends BukkitCommand {

    public MapSelectorCommand(String name, List<String> aliases) {
        super(name);
        setDescription("Abre o menu de mapas do BedWars");
        setUsage("/" + name + " <modo>");
        setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("Este comando é apenas para jogadores!");
            return true;
        }

        Player player = (Player) sender;
        MapSelectorConfig config = BedWars.getMapSelectorConfig();
        if (config == null) return true;

        if (args.length != 1) {
            player.sendMessage(config.getMessage(MapSelectorConfig.MSG_GROUP_MISSING));
            return true;
        }

        String group = args[0];
        if (!MapSelectorUtils.groupsExist(group)) {
            player.sendMessage(config.getMessage(MapSelectorConfig.MSG_GROUP_UNKNOWN));
            return true;
        }

        new SelectorMenu(player, group).open();
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return Collections.emptyList();

        String typed = args[0].toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String group : MapSelectorUtils.getExistingGroups()) {
            if (group.toLowerCase().startsWith(typed)) matches.add(group);
        }
        return matches;
    }
}
