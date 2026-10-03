package com.tomkeuper.bedwars.commands.stats;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.commands.bedwars.subcmds.regular.CmdStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

/**
 * /stats opens your own stats menu, /stats &lt;player&gt; opens someone else's.
 * <p>
 * A player on this server is read from the loaded stats. Anyone else is looked up by the uuid Bukkit knows for the
 * name and read from the database, so it works for players who already joined this server at least once.
 */
public class StatsCommand extends BukkitCommand {

    private static final long COOLDOWN_MILLIS = 3000L;

    public StatsCommand(String name) {
        super(name);
        setAliases(Arrays.asList("estatisticas"));
    }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (s instanceof ConsoleCommandSender) {
            s.sendMessage("Este comando é apenas para jogadores!");
            return true;
        }
        Player viewer = (Player) s;

        // same rule as /bw stats: not while playing, unless spectating
        IArena arena = Arena.getArenaByPlayer(viewer);
        if (arena != null && arena.getStatus() != GameState.starting && arena.getStatus() != GameState.waiting
                && !arena.isSpectator(viewer)) {
            viewer.sendMessage("§cVocê não pode ver estatísticas durante a partida.");
            return true;
        }

        if (onCooldown(viewer)) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase(viewer.getName())) {
            Misc.openStatsGUI(viewer);
            return true;
        }

        String name = args[0];
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            IPlayerStats loaded = BedWars.getStatsManager().getUnsafe(online.getUniqueId());
            if (loaded != null) {
                Misc.openStatsGUI(viewer, online, loaded, null);
                return true;
            }
        }

        UUID viewerId = viewer.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(name);
            UUID uuid = target.getUniqueId();

            IPlayerStats stats = BedWars.getRemoteDatabase().hasStats(uuid) ? BedWars.getRemoteDatabase().fetchStats(uuid) : null;
            Player stillOnline = Bukkit.getPlayer(viewerId);
            if (stillOnline == null) return;

            if (stats == null) {
                Bukkit.getScheduler().runTask(BedWars.plugin, () ->
                        stillOnline.sendMessage("§cNenhuma estatística encontrada para §f" + name + "§c."));
                return;
            }
            if (stats.getName() == null || stats.getName().isEmpty()) {
                stats.setName(target.getName() != null ? target.getName() : name);
            }

            Object[] levelData = null;
            try {
                levelData = BedWars.getRemoteDatabase().getLevelData(uuid);
            } catch (Exception ignored) {
                // the level placeholders just come out empty
            }
            Misc.openStatsGUI(stillOnline, null, stats, levelData);
        });
        return true;
    }

    private boolean onCooldown(Player player) {
        Long last = CmdStats.getStatsCoolDown().get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < COOLDOWN_MILLIS) return true;
        CmdStats.getStatsCoolDown().put(player.getUniqueId(), now);
        return false;
    }
}
