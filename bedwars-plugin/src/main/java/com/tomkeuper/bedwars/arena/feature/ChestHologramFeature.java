package com.tomkeuper.bedwars.arena.feature;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.*;

/**
 * Shows "click to deposit" above every chest and ender chest of a running map.
 * <p>
 * The chests are not part of the arena config, so they are found by scanning the map's chunks as they load.
 * The holograms are packet entities, which the client drops together with their chunk, so each player is only
 * sent the ones near them and gets them again after walking back from far enough to have lost them.
 */
public class ChestHologramFeature implements Listener {

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    // Sits the bottom line just above the lid, given how high a nametag floats over its armor stand.
    private static final double HEIGHT_OFFSET = -1.0;
    // Show and hide apart so a player standing at the edge does not make the hologram flicker.
    private static final double SHOW_DISTANCE_SQUARED = 32 * 32;
    private static final double HIDE_DISTANCE_SQUARED = 48 * 48;

    private static ChestHologramFeature instance;

    private final Map<IArena, Map<Long, ChestHolo>> chests = new HashMap<>();

    private ChestHologramFeature() {
        Bukkit.getPluginManager().registerEvents(this, BedWars.plugin);
        Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::tick, 20L, 10L);
    }

    public static void init() {
        if (instance == null) instance = new ChestHologramFeature();
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent e) {
        IArena arena = e.getArena();
        if (e.getNewState() == GameState.playing) {
            World world = arena.getWorld();
            if (world == null) return;
            for (Chunk chunk : world.getLoadedChunks()) scan(arena, chunk);
        } else if (e.getOldState() == GameState.playing) {
            clear(arena);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        IArena arena = Arena.getArenaByIdentifier(e.getWorld().getName());
        if (arena == null || arena.getStatus() != GameState.playing) return;
        scan(arena, e.getChunk());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        chests.keySet().removeIf(arena -> {
            if (arena.getWorld() != null && arena.getWorld() != e.getWorld()) return false;
            chests.get(arena).values().forEach(ChestHolo::remove);
            return true;
        });
    }

    /**
     * A respawn may reload the chunks around the player, taking the holograms with them, so send them again.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        for (Map<Long, ChestHolo> holos : chests.values()) {
            for (ChestHolo holo : holos.values()) holo.forget(e.getPlayer());
        }
    }

    private void scan(IArena arena, Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            Material type = state.getType();
            if (type != Material.CHEST && type != Material.ENDER_CHEST) continue;

            Block block = state.getBlock();
            Location location = block.getLocation().add(0.5, HEIGHT_OFFSET, 0.5);
            Block anchor = block;

            // A double chest gets a single hologram, centred between its halves and owned by the western/northern one.
            if (type == Material.CHEST) {
                for (BlockFace face : SIDES) {
                    Block other = block.getRelative(face);
                    if (other.getType() != Material.CHEST) continue;
                    location.add(face.getModX() * 0.5, 0, face.getModZ() * 0.5);
                    if (other.getX() < block.getX() || other.getZ() < block.getZ()) anchor = other;
                    break;
                }
            }

            chests.computeIfAbsent(arena, k -> new HashMap<>())
                    .computeIfAbsent(key(anchor), k -> new ChestHolo(location));
        }
    }

    private void clear(IArena arena) {
        Map<Long, ChestHolo> holos = chests.remove(arena);
        if (holos != null) holos.values().forEach(ChestHolo::remove);
    }

    private void tick() {
        for (Map.Entry<IArena, Map<Long, ChestHolo>> entry : chests.entrySet()) {
            World world = entry.getKey().getWorld();
            List<Player> viewers = world == null ? Collections.emptyList() : world.getPlayers();
            for (ChestHolo holo : entry.getValue().values()) holo.sync(viewers);
        }
    }

    private static long key(Block block) {
        return ((long) block.getX() & 0x3FFFFFF) << 38 | ((long) block.getZ() & 0x3FFFFFF) << 12 | (block.getY() & 0xFFF);
    }

    /**
     * One chest's hologram, with a copy per language since the text is sent as is to whoever sees it.
     */
    private static class ChestHolo {

        private final Location location;
        private final Map<String, IHologram> byLanguage = new HashMap<>();
        // Who has the hologram right now, and in which language.
        private final Map<Player, String> shown = new HashMap<>();

        private ChestHolo(Location location) {
            this.location = location;
        }

        private void sync(List<Player> viewers) {
            Set<Player> present = new HashSet<>(viewers);
            shown.keySet().removeIf(player -> {
                if (present.contains(player) && player.isOnline()) return false;
                drop(player);
                return true;
            });

            for (Player player : viewers) {
                String iso = Language.getPlayerLanguage(player).getIso();
                String current = shown.get(player);
                double distance = player.getLocation().distanceSquared(location);

                if (current != null && (distance > HIDE_DISTANCE_SQUARED || !current.equals(iso))) {
                    hide(player);
                    current = null;
                }
                if (current == null && distance <= SHOW_DISTANCE_SQUARED) show(player, iso);
            }
        }

        private void show(Player player, String iso) {
            IHologram hologram = byLanguage.computeIfAbsent(iso, k -> BedWars.hologramManager.createHologram(
                    Collections.<Player>emptyList(), location.clone(),
                    Language.getLang(k).l(Messages.CHEST_HOLOGRAM).toArray(new String[0])));
            hologram.addPlayer(player);
            hologram.getLines().forEach(line -> line.reveal(player));
            shown.put(player, iso);
        }

        private void hide(Player player) {
            String iso = shown.remove(player);
            IHologram hologram = iso == null ? null : byLanguage.get(iso);
            if (hologram != null) hologram.removePlayer(player);
        }

        /**
         * Stop tracking a player without sending anything, so the next sync sends the hologram afresh.
         */
        private void forget(Player player) {
            String iso = shown.remove(player);
            IHologram hologram = iso == null ? null : byLanguage.get(iso);
            if (hologram != null) hologram.getPlayers().remove(player);
        }

        /**
         * A player who left the map: an online one still has the entities and needs them destroyed.
         */
        private void drop(Player player) {
            IHologram hologram = byLanguage.get(shown.get(player));
            if (hologram == null) return;
            if (player.isOnline()) hologram.removePlayer(player);
            else hologram.getPlayers().remove(player);
        }

        private void remove() {
            byLanguage.values().forEach(IHologram::remove);
            byLanguage.clear();
            shown.clear();
        }
    }
}
