package com.tomkeuper.bedwars.arena;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.events.player.PlayerStatChangeEvent;
import com.tomkeuper.bedwars.api.events.team.TeamEliminatedEvent;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.stats.IModeStats;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.arena.tasks.ReJoinTask;
import com.tomkeuper.bedwars.configuration.Sounds;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.shop.ShopCache;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;

public class ReJoin {

    private UUID player;
    private String displayName;
    private IArena arena;
    private ITeam bwt;
    private final ArrayList<ShopCache.CachedItem> permanentsAndNonDowngradables = new ArrayList<>();

    private static final List<ReJoin> reJoinList = new ArrayList<>();

    /**
     * Make rejoin possible for a player
     */
    public ReJoin(Player player, IArena arena, ITeam bwt, List<ShopCache.CachedItem> cachedArmor) {
        ReJoin rj = getPlayer(player);
        if (rj != null) {
            rj.destroy(true);
        }
        if (bwt == null) return;
        if (bwt.isBedDestroyed()) return;
        this.bwt = bwt;
        this.player = player.getUniqueId();
        this.displayName = player.getDisplayName();
        this.arena = arena;
        reJoinList.add(this);
        BedWars.debug("ReJoin criado para " + player.getName() + " " + player.getUniqueId() + " at " + arena.getArenaName());
        // the whole team is gone: it keeps its bed until the rejoin time runs out
        if (bwt.getMembers().isEmpty()) new ReJoinTask(arena, bwt);
        this.permanentsAndNonDowngradables.addAll(cachedArmor);

        // auto scale runs in every server type now, but redis only exists in bungee mode
        if (BedWars.getRedisConnection() != null) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "RC");
            json.addProperty("uuid", player.getUniqueId().toString());
            json.addProperty("arena_id", arena.getWorldName());
            json.addProperty("server", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));

            BedWars.getRedisConnection().sendMessage(json.toString());
        }
    }

    /**
     * Check if a player has stored data
     */
    public static boolean exists(@NotNull Player pl) {
        BedWars.debug("Verificação de existência do ReJoin " + pl.getUniqueId());
        for (ReJoin rj : getReJoinList()) {
            BedWars.debug("Verificação de existência do ReJoin, varredura da lista: " + rj.getPl().toString());
            if (rj.getPl().equals(pl.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a player ReJoin
     */
    @Nullable
    public static ReJoin getPlayer(@NotNull Player player) {
        BedWars.debug("ReJoin getPlayer " + player.getUniqueId());
        for (ReJoin rj : getReJoinList()) {
            if (rj.getPl().equals(player.getUniqueId())) {
                return rj;
            }
        }
        return null;
    }

    /**
     * Check if can reJoin
     */
    public boolean canReJoin() {
        BedWars.debug("ReJoin canReJoin  check.");
        if (arena == null) {
            BedWars.debug("ReJoin canReJoin: a arena é nula " + player.toString());
            destroy(true);
            return false;
        }
        if (arena.getStatus() == GameState.restarting) {
            BedWars.debug("ReJoin canReJoin: o status é reiniciando " + player.toString());
            destroy(true);
            return false;
        }
        if (bwt == null) {
            BedWars.debug("ReJoin canReJoin: bwt é nulo " + player.toString());
            destroy(true);
            return false;
        }
        if (bwt.isBedDestroyed()) {
            BedWars.debug("ReJoin canReJoin: a cama foi destruída " + player.toString());
            destroy(false);
            return false;
        }
        return true;
    }

    /**
     * Make a player re-join the arena
     */
    public boolean reJoin(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL) {
            Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(true);
                player.setFlying(true);
            }, 20L);
        }
        return arena.reJoin(player);
    }

    /**
     * Destroy data and rejoin possibility
     */
    public void destroy(boolean destroyTeam) {
        BedWars.debug("ReJoin destruído para " + player.toString());
        reJoinList.remove(this);
        if (BedWars.getRedisConnection() != null){
            JsonObject json = new JsonObject();
            json.addProperty("type", "RD");
            json.addProperty("uuid", player.toString());
            json.addProperty("server", BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
            BedWars.getRedisConnection().sendMessage(json.toString());
        }

        if (bwt != null && destroyTeam && bwt.getMembers().isEmpty()) {
            ReJoinTask task = ReJoinTask.getTask(bwt);
            if (task != null) task.destroy();
            bwt.setBedDestroyed(true);
            if (bwt != null) {
                for (Player p2 : arena.getPlayers()) {
                    BedWars.plugin.adventure().player(p2).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("%bw_team_color%", bwt.getColor().chat().toString())
                            .replace("%bw_team_name%", bwt.getDisplayName(Language.getPlayerLanguage(p2)))));
                }
                for (Player p2 : arena.getSpectators()) {
                    BedWars.plugin.adventure().player(p2).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p2, Messages.TEAM_ELIMINATED_CHAT).replace("%bw_team_color%", bwt.getColor().chat().toString())
                            .replace("%bw_team_name%", bwt.getDisplayName(Language.getPlayerLanguage(p2)))));
                }
            }
            arena.checkWinner();
        }
    }

    /**
     * Get Player
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * Get player team
     */
    public ITeam getBedWarsTeam() {
        return bwt;
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    public ReJoinTask getTask() {
        return ReJoinTask.getTask(bwt);
    }

    /**
     * Check if a team has members that left the game and can still come back.
     */
    public static boolean hasPending(ITeam team) {
        for (ReJoin rj : reJoinList) {
            if (rj.bwt == team) return true;
        }
        return false;
    }

    /**
     * Eliminate the disconnected members of a team. Used when their bed is broken while they are away, and when the
     * whole team stayed away for longer than the rejoin time.
     *
     * @param killer the player who broke the bed and gets a final kill for every disconnected member, or null when the
     *               rejoin time ran out.
     */
    public static void eliminateDisconnected(@NotNull IArena arena, @NotNull ITeam team, @Nullable Player killer) {
        ReJoinTask task = ReJoinTask.getTask(team);
        if (task != null) task.destroy();

        List<ReJoin> disconnected = new ArrayList<>();
        for (ReJoin rj : reJoinList) {
            if (rj.arena == arena && rj.bwt == team) disconnected.add(rj);
        }

        if (!team.isBedDestroyed()) team.setBedDestroyed(true);

        ITeam killerTeam = killer == null ? null : arena.getTeam(killer);
        for (ReJoin rj : disconnected) {
            BedWars.debug("ReJoin: eliminando " + rj.getPl() + " desconectado do time " + team.getName());
            rj.destroy(false);
            rj.addEliminationStats(killer != null);
            if (killer != null) rj.rewardFinalKill(killer, killerTeam);
        }

        if (!team.getMembers().isEmpty()) return;
        Bukkit.getPluginManager().callEvent(new TeamEliminatedEvent(arena, team));
        for (Player p : arena.getWorld().getPlayers()) {
            String msg = getMsg(p, Messages.TEAM_ELIMINATED_CHAT)
                    .replace("%bw_team_color%", team.getColor().chat().toString())
                    .replace("%bw_team_name%", team.getDisplayName(Language.getPlayerLanguage(p)));
            BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(msg));
        }
        Bukkit.getScheduler().runTask(BedWars.plugin, arena::checkWinner);
    }

    private void rewardFinalKill(@NotNull Player killer, @Nullable ITeam killerTeam) {
        arena.addPlayerKill(killer, true, Bukkit.getPlayer(player));
        Sounds.playSound(ConfigPath.SOUNDS_KILL, killer);

        for (Player on : arena.getWorld().getPlayers()) {
            Language lang = Language.getPlayerLanguage(on);
            String msg = getMsg(on, Messages.PLAYER_DIE_DISCONNECTED_FINAL_KILL)
                    .replace("%bw_player_color%", bwt.getColor().chat().toString())
                    .replace("%bw_player%", displayName)
                    .replace("%bw_playername%", displayName)
                    .replace("%bw_team%", bwt.getDisplayName(lang))
                    .replace("%bw_killer_color%", killerTeam == null ? "" : killerTeam.getColor().chat().toString())
                    .replace("%bw_killer_playername%", killer.getName())
                    .replace("%bw_killer_name%", killer.getDisplayName())
                    .replace("%bw_killer_team_name%", killerTeam == null ? "" : killerTeam.getDisplayName(lang));
            BedWars.plugin.adventure().player(on).sendMessage(ChatFormatting.parseLegacyMini(msg));
        }

        IPlayerStats killerStats = BedWars.getStatsManager().getUnsafe(killer.getUniqueId());
        if (killerStats == null) return;
        PlayerStatChangeEvent ev = new PlayerStatChangeEvent(killer, arena, PlayerStatChangeEvent.StatType.FINAL_KILLS);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;
        killerStats.setFinalKills(killerStats.getFinalKills() + 1);
        IModeStats killerMode = killerStats.getModeStats(arena.getGroup().toLowerCase());
        killerMode.setFinalKills(killerMode.getFinalKills() + 1);
    }

    /**
     * The player is not in the arena anymore, so the final death and the loss go straight to the stats: the loaded
     * ones when the player is still on this server, the stored ones otherwise.
     */
    private void addEliminationStats(boolean bedLost) {
        String mode = arena.getGroup().toLowerCase();
        IPlayerStats loaded = BedWars.getStatsManager().getUnsafe(player);
        if (loaded != null) {
            applyEliminationStats(loaded, mode, bedLost);
            Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> BedWars.getRemoteDatabase().saveStats(loaded));
            return;
        }
        UUID uuid = player;
        Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
            IPlayerStats stored = BedWars.getRemoteDatabase().fetchStats(uuid);
            applyEliminationStats(stored, mode, bedLost);
            BedWars.getRemoteDatabase().saveStats(stored);
        });
    }

    private static void applyEliminationStats(@NotNull IPlayerStats stats, String mode, boolean bedLost) {
        IModeStats modeStats = stats.getModeStats(mode);
        stats.setFinalDeaths(stats.getFinalDeaths() + 1);
        modeStats.setFinalDeaths(modeStats.getFinalDeaths() + 1);
        stats.setLosses(stats.getLosses() + 1);
        modeStats.setLosses(modeStats.getLosses() + 1);
        if (bedLost) {
            stats.setBedsLost(stats.getBedsLost() + 1);
            modeStats.setBedsLost(modeStats.getBedsLost() + 1);
        }
    }

    public UUID getPl() {
        return player;
    }

    @SuppressWarnings("WeakerAccess")
    public List<ShopCache.CachedItem> getPermanentsAndNonDowngradables() {
        return permanentsAndNonDowngradables;
    }

    public static List<ReJoin> getReJoinList() {
        return Collections.unmodifiableList(reJoinList);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof ReJoin)) return false;
        ReJoin reJoin = (ReJoin) o;
        return reJoin.getPl().equals(getPl());
    }
}
