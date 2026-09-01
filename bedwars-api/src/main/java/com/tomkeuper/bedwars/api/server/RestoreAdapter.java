package com.tomkeuper.bedwars.api.server;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public abstract class RestoreAdapter {

    private final Plugin plugin;

    /**
     * Constructor for RestoreAdapter.
     *
     * @param owner The owner plugin of the adapter.
     */
    public RestoreAdapter(Plugin owner) {
        this.plugin = owner;
    }

    /**
     * Get the owner plugin of the adapter.
     *
     * @return The owner plugin.
     */
    public Plugin getOwner() {
        return plugin;
    }

    /**
     * Load the world.
     * Arenas will be initialized automatically based on WorldLoadEvent.
     *
     * @param arena The arena to enable.
     */
    public abstract void onEnable(IArena arena);

    /**
     * Restore the world.
     * Call new Arena when it's done.
     *
     * @param arena The arena to restart.
     */
    public abstract void onRestart(IArena arena);

    /**
     * Unload the world.
     * This is usually used for /bw unloadArena name.
     *
     * @param arena The arena to disable.
     */
    public abstract void onDisable(IArena arena);

    /**
     * Load the world for setting it up.
     *
     * @param setupSession The setup session containing the world to load.
     */
    public abstract void onSetupSessionStart(ISetupSession setupSession);

    /**
     * Unload the world.
     *
     * @param setupSession The setup session containing the world to unload.
     */
    public abstract void onSetupSessionClose(ISetupSession setupSession);

    /**
     * Remove lobby blocks.
     *
     * @param arena The arena to remove lobby blocks from.
     */
    public void onLobbyRemoval(@NotNull IArena arena) {
        Location corner1 = arena.getConfig().getArenaLoc(ConfigPath.ARENA_WAITING_POS1);
        Location corner2 = arena.getConfig().getArenaLoc(ConfigPath.ARENA_WAITING_POS2);
        if (null == corner1 || null == corner2 || null == corner1.getWorld()) return;

        new LobbyRemovalTask(this, corner1, corner2).runTaskTimer(getOwner(), 1L, 1L);
    }

    /**
     * Clears the waiting lobby without stalling the server.
     * <p>
     * The naive version of this walked the whole cuboid in a single tick calling
     * {@code block.setType(AIR)}, which runs block physics for every block: neighbour updates,
     * light updates and a block change packet each. On a large lobby that is tens of thousands
     * of physics events in one tick and the server visibly freezes when a game starts.
     * <p>
     * This one keeps the same result but:
     * <ul>
     *   <li>skips blocks that are already air, which is most of a lobby region;</li>
     *   <li>writes with physics disabled, so no neighbour or light cascade per block and no
     *       item drops to clean up afterwards;</li>
     *   <li>walks the region chunk by chunk instead of striding across chunk borders on every
     *       block, so each chunk's palette stays hot;</li>
     *   <li>spreads the work over ticks with a fixed budget, so a huge lobby costs a little on
     *       several ticks instead of everything on one.</li>
     * </ul>
     * Small lobbies still finish in the first tick, so nothing looks different in game.
     */
    private static final class LobbyRemovalTask extends BukkitRunnable {

        /** Blocks looked at per tick. Reading an air block is cheap, so this can be generous. */
        private static final int BLOCK_BUDGET_PER_TICK = 16384;

        private final RestoreAdapter adapter;
        private final World world;
        private final int minX, minY, minZ, maxX, maxY, maxZ;

        // cursor over the region, chunk aligned
        private int chunkX, chunkZ;
        private int x, y, z;
        private boolean done;

        private LobbyRemovalTask(RestoreAdapter adapter, Location corner1, Location corner2) {
            this.adapter = adapter;
            this.world = corner1.getWorld();

            this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
            this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
            this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
            // upper bounds stay exclusive, same region the previous implementation cleared
            this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
            this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
            this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

            this.chunkX = minX >> 4;
            this.chunkZ = minZ >> 4;
            this.x = minX;
            this.y = minY;
            this.z = minZ;
            this.done = minX >= maxX || minY >= maxY || minZ >= maxZ;
        }

        @Override
        public void run() {
            if (done) {
                finish();
                return;
            }

            int budget = BLOCK_BUDGET_PER_TICK;
            while (budget > 0) {
                // bounds of the chunk we are currently inside, clipped to the region
                int chunkMaxX = Math.min(((chunkX << 4) + 16), maxX);
                int chunkMaxZ = Math.min(((chunkZ << 4) + 16), maxZ);

                Block block = world.getBlockAt(x, y, z);
                if (block.getType() != Material.AIR) {
                    // physics off: no neighbour cascade, no light cascade, no dropped items
                    block.setType(Material.AIR, false);
                }
                budget--;

                // advance the cursor inside the current chunk column, then to the next chunk
                if (++y >= maxY) {
                    y = minY;
                    if (++z >= chunkMaxZ) {
                        z = Math.max(minZ, chunkZ << 4);
                        if (++x >= chunkMaxX) {
                            if (!advanceChunk()) {
                                done = true;
                                finish();
                                return;
                            }
                        }
                    }
                }
            }
        }

        /**
         * Move to the next chunk that overlaps the region.
         *
         * @return false when the whole region has been walked
         */
        private boolean advanceChunk() {
            chunkX++;
            if ((chunkX << 4) >= maxX) {
                chunkX = minX >> 4;
                chunkZ++;
                if ((chunkZ << 4) >= maxZ) return false;
            }
            x = Math.max(minX, chunkX << 4);
            z = Math.max(minZ, chunkZ << 4);
            y = minY;
            return true;
        }

        private void finish() {
            cancel();
            // physics were off so nothing dropped, but a player may have thrown something in
            adapter.clearItems(world, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    /**
     * Check if the given world exists.
     *
     * @param name The name of the world to check.
     * @return `true` if the world exists, `false` otherwise.
     */
    public abstract boolean isWorld(String name);

    /**
     * Delete a world.
     *
     * @param name The name of the world to delete.
     */
    public abstract void deleteWorld(String name);

    /**
     * Clone an arena world.
     *
     * @param sourceArena The name of the source world to clone.
     * @param destinationArena The name of the destination world to create.
     */
    public abstract void cloneArena(String sourceArena, String destinationArena);

    /**
     * Get the list of worlds.
     *
     * @return The list of world names.
     */
    public abstract List<String> getWorldsList();

    /**
     * Convert worlds if necessary before loading them.
     * Let them load on BedWars2023 main Thread, so they will be converted before getting loaded.
     */
    public abstract void convertWorlds();

    /**
     * Get the display name of the restore adapter.
     *
     * @return The display name.
     */
    public abstract String getDisplayName();

    public void foreachBlockInRegion(
            @Nullable Location corner1, @Nullable Location corner2,
            @NotNull Consumer<Block> consumer
    ) {
        if (null == corner1 || null == corner2) {
            return;
        }

        Vector min = new Vector(
                Math.min(corner1.getBlockX(), corner2.getBlockX()),
                Math.min(corner1.getBlockY(), corner2.getBlockY()),
                Math.min(corner1.getBlockZ(), corner2.getBlockZ())
        );

        Vector max = new Vector(
                Math.max(corner1.getBlockX(), corner2.getBlockX()),
                Math.max(corner1.getBlockY(), corner2.getBlockY()),
                Math.max(corner1.getBlockZ(), corner2.getBlockZ())
        );

        for (int x = min.getBlockX(); x < max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y < max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z < max.getBlockZ(); z++) {
                    consumer.accept(corner1.getWorld().getBlockAt(x, y, z));
                }
            }
        }
    }

    /**
     * Clear all entities for a given world
     *
     * @param world The world instance.
     */
    public void clearItems(@NotNull World world) {
        // getEntitiesByClass lets the server filter, instead of us walking every entity
        for (Item item : world.getEntitiesByClass(Item.class)) {
            item.remove();
        }
    }

    /**
     * Remove dropped items inside a region only, so clearing a lobby does not walk every
     * entity in the world while a game is running.
     */
    public void clearItems(@NotNull World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (Item item : world.getEntitiesByClass(Item.class)) {
            Location loc = item.getLocation();
            if (loc.getBlockX() < minX || loc.getBlockX() >= maxX) continue;
            if (loc.getBlockY() < minY || loc.getBlockY() >= maxY) continue;
            if (loc.getBlockZ() < minZ || loc.getBlockZ() >= maxZ) continue;
            item.remove();
        }
    }
}