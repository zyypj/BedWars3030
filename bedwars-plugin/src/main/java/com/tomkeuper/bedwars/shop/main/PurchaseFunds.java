package com.tomkeuper.bedwars.shop.main;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Resources a player can pay with besides the ones they carry: what they stored in their team's chests and in
 * their ender chest, and, for team upgrades, what their teammates carry.
 */
public final class PurchaseFunds {

    // Team chests are map blocks, so they are looked up once per team and game.
    private static final Map<ITeam, List<Location>> teamChests = new WeakHashMap<>();

    private PurchaseFunds() {
    }

    /**
     * The player's storage in the order it is spent: the team's chests first, since enemies can loot those,
     * then the ender chest.
     */
    public static List<Inventory> storage(Player player) {
        if (!BedWars.config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_SHARED_FUNDS_CHESTS)) return Collections.emptyList();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing) return Collections.emptyList();
        ITeam team = arena.getTeam(player);
        if (team == null) return Collections.emptyList();

        List<Inventory> inventories = new ArrayList<>();
        for (Location location : teamChests(arena, team)) {
            BlockState state = location.getBlock().getState();
            if (state instanceof Chest) inventories.add(((Chest) state).getBlockInventory());
        }
        inventories.add(player.getEnderChest());
        return inventories;
    }

    /**
     * Teammates whose inventories can pay for a team upgrade, not counting the buyer.
     */
    public static List<Player> teammates(Player player) {
        if (!BedWars.config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_SHARED_FUNDS_TEAMMATES)) return Collections.emptyList();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing) return Collections.emptyList();
        ITeam team = arena.getTeam(player);
        if (team == null) return Collections.emptyList();

        List<Player> teammates = new ArrayList<>();
        for (Player member : team.getMembers()) {
            if (member.equals(player) || !member.isOnline() || arena.isSpectator(member)) continue;
            teammates.add(member);
        }
        return teammates;
    }

    public static int count(Inventory inventory, Material currency) {
        int amount = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == currency) amount += item.getAmount();
        }
        return amount;
    }

    /**
     * Take up to {@code amount} of the currency out of an inventory.
     *
     * @return how much was taken.
     */
    public static int take(Inventory inventory, Material currency, int amount) {
        int taken = 0;
        for (int slot = 0; slot < inventory.getSize() && taken < amount; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() != currency) continue;

            int used = Math.min(item.getAmount(), amount - taken);
            taken += used;
            if (used == item.getAmount()) {
                inventory.setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - used);
                inventory.setItem(slot, item);
            }
        }
        return taken;
    }

    /**
     * Chests on the team's island, which is also what decides who may open them.
     */
    private static List<Location> teamChests(IArena arena, ITeam team) {
        List<Location> cached = teamChests.get(team);
        World world = arena.getWorld();
        // A restarted arena may keep its teams but load a fresh world.
        if (cached != null && (cached.isEmpty() || cached.get(0).getWorld() == world)) return cached;

        List<Location> found = new ArrayList<>();
        Location spawn = team.getSpawn();
        if (world != null && spawn != null) {
            int radius = arena.getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS);
            for (int cx = (spawn.getBlockX() - radius) >> 4; cx <= (spawn.getBlockX() + radius) >> 4; cx++) {
                for (int cz = (spawn.getBlockZ() - radius) >> 4; cz <= (spawn.getBlockZ() + radius) >> 4; cz++) {
                    for (BlockState state : world.getChunkAt(cx, cz).getTileEntities()) {
                        if (state.getType() != Material.CHEST) continue;
                        Block block = state.getBlock();
                        if (block.getLocation().distance(spawn) <= radius) found.add(block.getLocation());
                    }
                }
            }
        }
        teamChests.put(team, found);
        return found;
    }
}
