package com.tomkeuper.bedwars.arena;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.util.FileUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the running games and makes sure every configured arena always has one
 * game left for players to join.
 * <p>
 * Every game runs in its own copy of the base map, living in a world folder named
 * {@code bw_<id>} where the id is 5 alphanumeric characters. The base map itself is never
 * played on, so the same arena can host as many concurrent games as the limits allow.
 */
public class ArenaManager {

    /**
     * Prefix of every per-game world folder. Anything under the world container starting with
     * this is owned by us, which is what makes the orphan cleanup on boot safe to run.
     */
    public static final String WORLD_PREFIX = "bw_";

    private static final int ID_LENGTH = 5;
    /** No l/1/i/0/o, so an id read off a sign or chat is never ambiguous. */
    private static final char[] ID_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();
    private static final int ID_MAX_ATTEMPTS = 128;
    private static final long ARENA_LIST_CACHE_MS = 60_000L;

    private final Random random = new Random();
    private final Set<String> usedIds = ConcurrentHashMap.newKeySet();
    private volatile List<String> configuredArenas;
    private volatile long configuredArenasAt;

    /**
     * Generate the world name for a new game, e.g. {@code bw_a7f3k}.
     * The id is unique among the games this session has handed out and does not collide with
     * a world that is already loaded or already on disk.
     */
    public @NotNull String generateGameID() {
        for (int attempt = 0; attempt < ID_MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(ID_LENGTH);
            for (int i = 0; i < ID_LENGTH; i++) {
                sb.append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]);
            }
            String id = sb.toString();
            if (!usedIds.add(id)) continue;
            String worldName = WORLD_PREFIX + id;
            if (Bukkit.getWorld(worldName) != null || new File(Bukkit.getWorldContainer(), worldName).exists()) {
                // keep the id reserved so we do not try it again this session
                continue;
            }
            return worldName;
        }
        throw new IllegalStateException("Não foi possível gerar um id de partida livre após " + ID_MAX_ATTEMPTS + " tentativas.");
    }

    /**
     * The 5 character id of a game, without the world prefix.
     */
    public static @NotNull String getGameId(@NotNull String worldName) {
        return worldName.startsWith(WORLD_PREFIX) ? worldName.substring(WORLD_PREFIX.length()) : worldName;
    }

    /**
     * Give an id back once its game is gone, so long running servers can reuse it.
     */
    public void releaseGameID(String worldName) {
        if (worldName == null) return;
        usedIds.remove(getGameId(worldName));
    }

    /**
     * Max concurrent games of the same arena. 0 or less means unlimited.
     */
    public int getMaxGamesPerArena() {
        int limit = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_AUTO_SCALE_MAX_PER_ARENA);
        if (limit <= 0 && BedWars.getServerType() == ServerType.BUNGEE) {
            // fall back to the historical bungee-only key so existing setups keep their limit
            limit = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_AUTO_SCALE_LIMIT);
        }
        return limit;
    }

    /**
     * Max concurrent games across every arena. 0 or less means unlimited.
     * This is the one that keeps a server with many maps from loading a world per map per copy.
     */
    public int getMaxTotalGames() {
        return BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_AUTO_SCALE_MAX_TOTAL);
    }

    /**
     * Whether another game of this arena should be spun up right now.
     * <p>
     * The answer is no when a game of this arena is already joinable (waiting or starting) or
     * already being prepared, which is what keeps exactly one spare game per arena instead of
     * growing without bound.
     */
    public boolean canCreateGame(String arenaName) {
        if (!BedWars.autoscale || BedWars.isShuttingDown() || arenaName == null) return false;

        int totalLimit = getMaxTotalGames();
        if (totalLimit > 0 && Arena.getArenas().size() + Arena.getEnableQueue().size() >= totalLimit) return false;

        // bungee restarts the whole instance after N games, so never go past that budget
        if (BedWars.getServerType() == ServerType.BUNGEE
                && Arena.getGamesBeforeRestart() != -1
                && Arena.getArenas().size() >= Arena.getGamesBeforeRestart()) return false;

        for (IArena queued : Arena.getEnableQueue()) {
            // a copy is already being prepared, that is the spare
            if (queued.getArenaName().equalsIgnoreCase(arenaName)) return false;
        }

        int copies = 0;
        for (IArena arena : Arena.getArenas()) {
            if (!arena.getArenaName().equalsIgnoreCase(arenaName)) continue;
            copies++;
            GameState status = arena.getStatus();
            if (status == GameState.waiting || status == GameState.starting) return false;
        }

        int perArenaLimit = getMaxGamesPerArena();
        return perArenaLimit <= 0 || copies < perArenaLimit;
    }

    /**
     * Spin up another game of this arena if one is needed and the limits allow it.
     *
     * @return true when a new game was queued
     */
    public boolean ensureAvailability(String arenaName) {
        if (!canCreateGame(arenaName)) return false;
        new Arena(arenaName, null);
        return true;
    }

    /**
     * Walk every configured arena and top it back up. Used as a periodic safety net, so a game
     * that failed to load or an arena disabled by hand does not leave players stuck.
     * <p>
     * Creates at most one game per pass: world loading is serialised through the enable queue
     * anyway, and this keeps a server with many maps from queueing a burst of world unzips.
     */
    public boolean topUpArenas() {
        if (!BedWars.autoscale || BedWars.isShuttingDown()) return false;
        for (String arenaName : getConfiguredArenas()) {
            if (ensureAvailability(arenaName)) return true;
        }
        return false;
    }

    /**
     * Names of the arenas that have a configuration file, i.e. every map that can host a game.
     * <p>
     * Cached because the top up pass runs on the main thread on a timer and listing a directory
     * is disk I/O. New arenas show up within {@link #ARENA_LIST_CACHE_MS}, or immediately if
     * something calls {@link #invalidateConfiguredArenas()}.
     */
    public @NotNull List<String> getConfiguredArenas() {
        long now = System.currentTimeMillis();
        List<String> cached = configuredArenas;
        if (cached != null && now - configuredArenasAt < ARENA_LIST_CACHE_MS) return cached;

        File dir = new File(BedWars.plugin.getDataFolder(), "/Arenas");
        File[] files = dir.listFiles();
        List<String> names = new ArrayList<>(files == null ? 0 : files.length);
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    names.add(file.getName().substring(0, file.getName().length() - 4));
                }
            }
        }
        configuredArenas = names;
        configuredArenasAt = now;
        return names;
    }

    /**
     * Forget the cached arena list, e.g. after an arena was set up or deleted.
     */
    public void invalidateConfiguredArenas() {
        configuredArenas = null;
    }

    /**
     * Delete per-game world folders left behind by a crash. Safe because only games use the
     * {@link #WORLD_PREFIX} prefix, and any world still in use is loaded at this point.
     */
    public void cleanupOrphanWorlds() {
        File container = Bukkit.getWorldContainer();
        File[] worlds = container.listFiles();
        if (worlds == null) return;
        int removed = 0;
        for (File world : worlds) {
            if (!world.isDirectory() || !world.getName().startsWith(WORLD_PREFIX)) continue;
            if (Bukkit.getWorld(world.getName()) != null) continue;
            if (Arena.getArenaByIdentifier(world.getName()) != null) continue;
            FileUtil.delete(world);
            removed++;
        }
        if (removed > 0) {
            BedWars.plugin.getLogger().info("Limpeza de mundos de partida órfãos: " + removed + " removido(s).");
        }
    }

    /**
     * Games of an arena that players can still join, best candidate first.
     */
    public static @NotNull List<IArena> getJoinableGames(String arenaName) {
        List<IArena> joinable = new ArrayList<>();
        for (IArena arena : Arena.getArenasByName(arenaName)) {
            GameState status = arena.getStatus();
            if (status == GameState.waiting || status == GameState.starting) joinable.add(arena);
        }
        return Arena.getSorted(joinable);
    }

    /**
     * Whether a world belongs to a running game.
     */
    public static boolean isGameWorld(String worldName) {
        return worldName != null && worldName.startsWith(WORLD_PREFIX) && Objects.nonNull(Arena.getArenaByIdentifier(worldName));
    }
}
