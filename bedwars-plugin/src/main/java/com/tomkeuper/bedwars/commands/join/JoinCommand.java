package com.tomkeuper.bedwars.commands.join;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standalone shortcut for the join sub command, so players can type {@code /entrar <arena>}
 * instead of the longer {@code /<mainCmd> join <arena>}.
 */
public class JoinCommand extends BukkitCommand {

    public JoinCommand(String name, List<String> aliases) {
        super(name);
        setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender s, String st, String[] args) {
        if (s instanceof ConsoleCommandSender) return true;
        Bukkit.dispatchCommand(s, BedWars.mainCmd + " join" + (args.length == 0 ? "" : " " + String.join(" ", args)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender s, String alias, String[] args) throws IllegalArgumentException {
        if (args.length != 1) return Collections.emptyList();
        List<String> options = new ArrayList<>(BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS));
        options.add("random");
        for (IArena arena : Arena.getArenas()) {
            options.add(arena.getArenaName());
        }
        List<String> matches = new ArrayList<>(options.size());
        StringUtil.copyPartialMatches(args[0], options, matches);
        matches.sort(String.CASE_INSENSITIVE_ORDER);
        return matches;
    }
}
