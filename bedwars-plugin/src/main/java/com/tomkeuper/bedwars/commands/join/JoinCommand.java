/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

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
