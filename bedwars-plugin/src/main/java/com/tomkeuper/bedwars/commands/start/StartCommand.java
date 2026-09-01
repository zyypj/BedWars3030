package com.tomkeuper.bedwars.commands.start;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.configuration.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standalone shortcut for the force-start sub command.
 * <p>
 * {@code /iniciar} shortens the starting countdown of the arena the player is in and
 * {@code /iniciar debug} additionally starts the countdown of an arena that is still waiting,
 * which is only meant for operators testing a map on their own.
 */
public class StartCommand extends BukkitCommand {

    private static final String DEBUG_ARGUMENT = "debug";

    public StartCommand(String name, List<String> aliases) {
        super(name);
        setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender s, String st, String[] args) {
        if (s instanceof ConsoleCommandSender) return true;
        Bukkit.dispatchCommand(s, BedWars.mainCmd + " start" + (args.length == 0 ? "" : " " + String.join(" ", args)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender s, String alias, String[] args) throws IllegalArgumentException {
        if (args.length != 1 || !canUseDebug(s)) return Collections.emptyList();
        List<String> matches = new ArrayList<>(1);
        StringUtil.copyPartialMatches(args[0], Collections.singletonList(DEBUG_ARGUMENT), matches);
        return matches;
    }

    /**
     * The debug argument is reserved for operators that are alone in a waiting arena,
     * mirroring the condition the sub command itself enforces.
     */
    private static boolean canUseDebug(CommandSender s) {
        if (!(s instanceof Player) || !s.isOp()) return false;
        if (!s.hasPermission(Permissions.PERMISSION_ALL) && !s.hasPermission(Permissions.PERMISSION_FORCESTART)) return false;
        IArena arena = Arena.getArenaByPlayer((Player) s);
        return arena != null && arena.getStatus() == GameState.waiting;
    }
}
