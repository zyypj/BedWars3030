package com.tomkeuper.bedwars.arena;

import com.tomkeuper.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

import static com.tomkeuper.bedwars.BedWars.plugin;

/**
 * This is where player stuff are stored so he can have them back after a game
 */
class PlayerGoods {

    private UUID uuid;
    private int level, foodLevel;
    private double health, healthscale;
    private float exp;
    private HashMap<ItemStack, Integer> items = new HashMap<>();
    private List<PotionEffect> potions = new ArrayList<>();
    private ItemStack[] armor;
    private HashMap<ItemStack, Integer> enderchest = new HashMap<>();
    private GameMode gamemode;
    private boolean allowFlight, flying;
    private String displayName, tabName;

    PlayerGoods(Player p, boolean prepare){
        this(p, prepare, false);
    }

    PlayerGoods(Player p, boolean prepare, boolean rejoin) {
        BedWars.debug("Criando PlayerGoods para o jogador " + p.getUniqueId() + " rejoin: " + rejoin + ".");
        // Do not overwrite an existing snapshot
        if (hasGoods(p)) {
            plugin.getLogger().severe("Ignorando a criação de PlayerGoods para " + p.getName() + " porque já existe um snapshot.");
            return;
        }

        // Rejoin/in-game safety: avoid snapshotting (and avoid touching inventories) during rejoin or in-game phases
        try {
            boolean isRejoin = ReJoin.exists(p);
            com.tomkeuper.bedwars.api.arena.IArena currentArena = Arena.getArenaByPlayer(p);
            boolean inGamePhase = false;
            if (currentArena != null) {
                inGamePhase = currentArena.getStatus() == com.tomkeuper.bedwars.api.arena.GameState.playing
                        || currentArena.getStatus() == com.tomkeuper.bedwars.api.arena.GameState.starting;
            }
            if (isRejoin || inGamePhase) {
                BedWars.debug("Ignorando a criação de PlayerGoods para " + p.getName() + " por detecção de rejoin/em jogo.");
                return;
            }
        } catch (Throwable t) {
            // best-effort detection; on failure continue with normal snapshot
        }
        this.uuid = p.getUniqueId();
        this.level = p.getLevel();
        this.exp = p.getExp();
        this.health = p.getHealth();
        this.healthscale = p.getHealthScale();
        this.foodLevel = p.getFoodLevel();
        playerGoods.put(p.getUniqueId(), this);
        int x = 0;
        for (ItemStack i : p.getInventory()) {
            if (i != null) {
                if (i.getType() != Material.AIR) {
                    items.put(i, x);
                }
            }
            x++;
        }
        for (PotionEffect ef : p.getActivePotionEffects()) {
            potions.add(ef);
            if (prepare) p.removePotionEffect(ef.getType());
        }
        armor = p.getInventory().getArmorContents();

        if (!rejoin) {
            int x2 = 0;
            for (ItemStack i : p.getEnderChest()) {
                if (i != null) {
                    if (i.getType() != Material.AIR) {
                        enderchest.put(i, x2);
                    }
                }
                x2++;
            }
        }

        this.gamemode = p.getGameMode();
        this.allowFlight = p.getAllowFlight();
        this.flying = p.isFlying();
        this.tabName = p.getPlayerListName();
        this.displayName = p.getDisplayName();

        /* prepare for arena */
        if (prepare) {
            p.setExp(0);
            p.setLevel(0);
            p.setHealthScale(20);
            p.setHealth(20);
            p.setFoodLevel(20);
            for (PotionEffect potion: p.getActivePotionEffects()) {
                p.removePotionEffect(potion.getType());
            }
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            if (!rejoin) {
                p.getEnderChest().clear();
            }
            p.setGameMode(GameMode.SURVIVAL);
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    /**
     * Safely create a PlayerGoods snapshot when needed.
     * - Returns existing snapshot if present.
     * - Skips creation during rejoin or in-game phases.
     * - Otherwise creates and returns a new snapshot.
     */
    static PlayerGoods createIfNeeded(Player p, boolean prepare) {
        // If there's already a snapshot, return it as-is
        PlayerGoods existing = getPlayerGoods(p);
        if (existing != null) {
            BedWars.debug("PlayerGoods já existe para " + p.getName() + ", returning existing snapshot.");
            return existing;
        }

        // Rejoin/in-game detection
        boolean skip;
        try {
            boolean isRejoin = ReJoin.exists(p);
            com.tomkeuper.bedwars.api.arena.IArena currentArena = Arena.getArenaByPlayer(p);
            boolean inGamePhase = false;
            if (currentArena != null) {
                inGamePhase = currentArena.getStatus() == com.tomkeuper.bedwars.api.arena.GameState.playing
                        || currentArena.getStatus() == com.tomkeuper.bedwars.api.arena.GameState.starting;
            }
            skip = isRejoin || inGamePhase;
        } catch (Throwable t) {
            skip = false; // be conservative and allow snapshot if detection failed
        }

        if (skip) {
            BedWars.debug("Ignorando a criação do snapshot de PlayerGoods para " + p.getName() + " (rejoin/em jogo).");
            return null;
        }

        // Safe to create a new snapshot
        return new PlayerGoods(p, prepare);
    }

    /**
     * a list where you can get PlayerGoods by player
     */
    private static HashMap<UUID, PlayerGoods> playerGoods = new HashMap<>();

    /**
     * check if a player has a vault
     */
    static boolean hasGoods(Player p) {
        return playerGoods.containsKey(p.getUniqueId());
    }

    /**
     * get a player vault
     */
    static PlayerGoods getPlayerGoods(Player p) {
        return playerGoods.get(p.getUniqueId());
    }

    /**
     * restore player
     */
    void restore() {
        BedWars.debug("Restaurando PlayerGoods do jogador " + uuid.toString());
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        playerGoods.remove(player.getUniqueId());


        for (PotionEffect pf : player.getActivePotionEffects()) {
            player.removePotionEffect(pf.getType());
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setLevel(level);
        player.setExp(exp);
        player.setHealthScale(healthscale);
        try {
            player.setHealth(health);
        } catch (Exception e){
            BedWars.plugin.getLogger().severe("Algo deu errado ao restaurar a vida do jogador: "+health+". Aplicando o padrão: 20");
            player.setHealth(20);
        }
        player.setFoodLevel(foodLevel);

        if (!items.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
                player.getInventory().setItem(entry.getValue(), entry.getKey());
            }
            player.updateInventory();
            items.clear();
        }
        if (!potions.isEmpty()) {
            for (PotionEffect pe : potions) {
                player.addPotionEffect(pe);
            }
            potions.clear();
        }
        player.getEnderChest().clear();
        if (!enderchest.isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry : enderchest.entrySet()) {
                player.getEnderChest().setItem(entry.getValue(), entry.getKey());
            }
            enderchest.clear();
        }
        player.getInventory().setArmorContents(armor);
        player.setGameMode(gamemode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);

        if (!displayName.equals(player.getDisplayName())) {
            player.setDisplayName(displayName);
        }
        if (!tabName.equals(player.getPlayerListName())) {
            player.setPlayerListName(tabName);
        }

        uuid = null;
        items = null;
        potions = null;
        armor = null;
        enderchest = null;
    }

}
