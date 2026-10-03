package com.tomkeuper.bedwars.arena.tasks;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.ReJoin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Eliminates a team once every member has been disconnected for longer than the rejoin time.
 * There is at most one task per team: it starts when the last member leaves and is cancelled as soon as one of them
 * comes back.
 */
public class ReJoinTask implements Runnable {

    private static final List<ReJoinTask> reJoinTasks = new ArrayList<>();

    private final IArena arena;
    private final ITeam bedWarsTeam;
    private final BukkitTask task;

    public ReJoinTask(IArena arena, ITeam bedWarsTeam) {
        ReJoinTask previous = getTask(bedWarsTeam);
        if (previous != null) previous.destroy();

        this.arena = arena;
        this.bedWarsTeam = bedWarsTeam;
        task = Bukkit.getScheduler().runTaskLater(BedWars.plugin, this, BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_REJOIN_TIME) * 20L);
        reJoinTasks.add(this);
    }

    @Override
    public void run() {
        reJoinTasks.remove(this);
        if (arena == null || bedWarsTeam == null || bedWarsTeam.getMembers() == null) return;
        if (arena.getStatus() != GameState.playing) return;
        if (bedWarsTeam.isBedDestroyed() || !bedWarsTeam.getMembers().isEmpty()) return;
        ReJoin.eliminateDisconnected(arena, bedWarsTeam, null);
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    public ITeam getTeam() {
        return bedWarsTeam;
    }

    /**
     * Destroy task
     */
    public void destroy() {
        reJoinTasks.remove(this);
        task.cancel();
    }

    /**
     * Get the pending elimination task of a team.
     */
    @Nullable
    public static ReJoinTask getTask(ITeam team) {
        for (ReJoinTask rjt : reJoinTasks) {
            if (rjt.getTeam() == team) return rjt;
        }
        return null;
    }

    /**
     * Get tasks list
     */
    @NotNull
    @Contract(pure = true)
    public static Collection<ReJoinTask> getReJoinTasks() {
        return Collections.unmodifiableCollection(reJoinTasks);
    }

    public void cancel() {
        task.cancel();
    }
}
