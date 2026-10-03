package com.tomkeuper.bedwars.arena;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.exceptions.InvalidMaterialException;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.region.Region;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.stats.IModeStats;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.configuration.Sounds;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.tomkeuper.bedwars.BedWars.*;
import static com.tomkeuper.bedwars.api.language.Language.getList;
import static com.tomkeuper.bedwars.api.language.Language.getMsg;

public class Misc {

    public static void moveToLobbyOrKick(Player p, @Nullable IArena arena, boolean notAbandon) {
        if (getServerType() != ServerType.BUNGEE) {
            if (!p.getWorld().getName().equalsIgnoreCase(config.getLobbyWorldName())) {
                Location loc = config.getConfigLoc("lobbyLoc");
                if (loc != null){ // Can happen when location is not set in config
                    try{
                        p.teleport(loc);
                    } catch (Exception ignored){
                        Bukkit.getLogger().severe("Não foi possível teleportar o jogador para o lobby! Tente definir o lobby novamente com /bw setLobby");
                    }
                } else {
                    forceKick(p, arena, notAbandon);
                    return;
                }
                if (arena != null) {
                    if (arena.isSpectator(p)) {
                        arena.removeSpectator(p, false, true);
                    } else {
                        arena.removePlayer(p, false, true);

                        // Manage internal parties
                        if (getPartyManager().isInternal()) {
                            if (getPartyManager().hasParty(p)) {
                                if (getPartyManager().isOwner(p)) {
                                    for (Player partyMember: getPartyManager().getMembers(p)) {
                                        if (arena.isPlayer(partyMember)) arena.removePlayer(partyMember, false, true);
                                        else if (arena.isSpectator(partyMember)) arena.removeSpectator(partyMember, false, true);
                                        else {
                                            BedWars.debug("Não é possível remover " + partyMember.getName() + " from " + arena.getDisplayName() + " porque o membro não é jogador nem espectador.");
                                        }
                                    }
                                }
                            }
                        }

                        if (!notAbandon && arena.getStatus() == GameState.playing) {
                            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_MARK_LEAVE_AS_ABANDON)) {
                                arena.abandonGame(p);
                            }
                        }
                    }
                }
            } else {
                forceKick(p, arena, notAbandon);
            }
            return;
        }
        forceKick(p, arena, notAbandon);
    }


    @SuppressWarnings("UnstableApiUsage")
    private static void forceKick(Player p, @Nullable IArena arena, boolean notAbandon) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_LOBBY_SERVER));
        p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        if (arena != null && !notAbandon && arena.getStatus() == GameState.playing) {
            if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_MARK_LEAVE_AS_ABANDON)) {
                arena.abandonGame(p);
            }
        }

        if (getServerType() == ServerType.BUNGEE) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // if lobby server is unreachable
                if (p.isOnline()) {
                    p.kickPlayer(getMsg(p, Messages.ARENA_RESTART_PLAYER_KICK));
                    if (arena != null && !notAbandon && arena.getStatus() == GameState.playing) {
                        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_MARK_LEAVE_AS_ABANDON)) {
                            arena.abandonGame(p);
                        }
                    }
                }
            }, 30L);
        }
    }

    /**
     * Win fireworks
     */
    @SuppressWarnings("unused")
    public static void launchFirework(Player p) {
        Color[] colors = {Color.WHITE, Color.AQUA, Color.BLUE, Color.FUCHSIA, Color.GRAY, Color.GREEN, Color.LIME, Color.RED,
                Color.YELLOW, Color.BLACK, Color.MAROON, Color.NAVY, Color.OLIVE, Color.ORANGE, Color.PURPLE};
        Random r = new Random();
        Firework fw = p.getWorld().spawn(p.getEyeLocation(), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder()
                .withFade(colors[r.nextInt(colors.length - 1)])
                .withTrail().withColor(colors[r.nextInt(colors.length - 1)]).with(FireworkEffect.Type.BALL_LARGE).build());
        fw.setFireworkMeta(meta);
        fw.setVelocity(p.getEyeLocation().getDirection());
    }


    @SuppressWarnings("unused")
    public static void launchFirework(Location l) {
        Color[] colors = {Color.WHITE, Color.AQUA, Color.BLUE, Color.FUCHSIA, Color.GRAY, Color.GREEN, Color.LIME, Color.RED,
                Color.YELLOW, Color.BLACK, Color.MAROON, Color.NAVY, Color.OLIVE, Color.ORANGE, Color.PURPLE};
        Random r = new Random();
        Firework fw = l.getWorld().spawn(l, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder()
                .withFade(colors[r.nextInt(colors.length - 1)])
                .withTrail().withColor(colors[r.nextInt(colors.length - 1)]).with(FireworkEffect.Type.BALL_LARGE).build());
        fw.setFireworkMeta(meta);
    }

    public static String replaceFirst(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    /**
     * Create an item stack
     *
     * @param material item material
     * @param data     item data
     * @param name     item name
     * @param lore     item lore
     * @param owner    in case of skull, can be null, don't worry
     */
    public static ItemStack createItem(Material material, byte data, boolean enchanted, String name, List<String> lore, Player owner, @SuppressWarnings("SameParameterValue") String metaKey, String metaData) {
        ItemStack i = new ItemStack(material, 1, data);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        im.setLore(lore);
        if (enchanted) {
            im.addEnchant(Enchantment.LUCK, 1, true);
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        i.setItemMeta(im);
        if (!(metaData.isEmpty() || metaKey.isEmpty())) {
            i = nms.addCustomData(i, metaKey + "_" + metaData);
        }
        if (owner != null) {
            if (nms.isPlayerHead(material.toString(), data)) {
                i = nms.getPlayerHead(owner, i);
            }
        }
        return i;
    }

    /**
     * Create an itemStack
     */
    @SuppressWarnings("unused")
    public static ItemStack createItemStack(String material, int data, String name, List<String> lore, boolean enchanted, String customData) throws InvalidMaterialException {
        Material m;
        try {
            m = Material.valueOf(material);
        } catch (Exception e) {
            throw new InvalidMaterialException(material);
        }
        ItemStack i = new ItemStack(m, 1, (short) data);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        im.setLore(lore);
        if (enchanted) {
            im.addEnchant(Enchantment.LUCK, 1, true);
            im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        i.setItemMeta(im);
        if (!customData.isEmpty()) {
            i = nms.addCustomData(i, customData);
        }
        return i;
    }

    @SuppressWarnings("unused")
    public static BlockFace getDirection(Location loc) {
        int rotation = (int) loc.getYaw();
        if (rotation < 0) {
            rotation += 360;
        }
        if (0 <= rotation && rotation < 22) {
            return BlockFace.SOUTH;
        } else if (22 <= rotation && rotation < 67) {
            return BlockFace.SOUTH;
        } else if (67 <= rotation && rotation < 112) {
            return BlockFace.WEST;
        } else if (112 <= rotation && rotation < 157) {
            return BlockFace.NORTH;
        } else if (157 <= rotation && rotation < 202) {
            return BlockFace.NORTH;
        } else if (202 <= rotation && rotation < 247) {
            return BlockFace.NORTH;
        } else if (247 <= rotation && rotation < 292) {
            return BlockFace.EAST;
        } else if (292 <= rotation && rotation < 337) {
            return BlockFace.SOUTH;
        } else if (337 <= rotation && rotation < 360) {
            return BlockFace.SOUTH;
        } else {
            return BlockFace.SOUTH;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isProjectile(Material i) {
        return Material.EGG == i || nms.materialFireball() == i || nms.materialSnowball() == i || Material.ARROW == i;
    }

    /**
     * create TextComponent message
     */
    public static TextComponent msgHoverClick(String msg, String hover, String click, ClickEvent.Action clickAction) {
        TextComponent tc = new TextComponent(msg);
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
        tc.setClickEvent(new ClickEvent(clickAction, click));
        return tc;
    }

    /**
     * add default stats gui item
     */
    public static void addDefaultStatsItem(YamlConfiguration yml, int slot, Material itemstack, int data, String path) {
        addDefaultStatsItem(yml, slot, itemstack, data, path, 1);
    }

    public static void addDefaultStatsItem(YamlConfiguration yml, int slot, Material itemstack, int data, String path, int amount) {
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_MATERIAL.replace("%path%", path), itemstack.toString());
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_DATA.replace("%path%", path), data);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_SLOT.replace("%path%", path), slot);
        yml.addDefault(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_AMOUNT.replace("%path%", path), amount);
    }

    /**
     * open stats GUI to player
     */
    /**
     * Marks an open stats menu. The title names whose stats it shows, so it cannot be told apart by title once a
     * player can open someone else's.
     */
    public static final class StatsMenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public static void openStatsGUI(Player p) {
        openStatsGUI(p, p, BedWars.getStatsManager().get(p.getUniqueId()), null);
    }

    /**
     * Show someone's stats to a player, in the viewer's language.
     *
     * @param target    the player the stats belong to, or null when they are not on this server
     * @param stats     the stats to show
     * @param levelData {@link com.tomkeuper.bedwars.api.database.IDatabase#getLevelData}, read for the level
     *                  placeholders only when target is null
     */
    public static void openStatsGUI(Player viewer, @Nullable Player target, IPlayerStats stats, @Nullable Object[] levelData) {
        Player p = viewer;

        Bukkit.getScheduler().runTask(plugin, () -> {

            if(!p.isOnline())
                return;

            /* create inventory */
            StatsMenuHolder holder = new StatsMenuHolder();
            Inventory inv = Bukkit.createInventory(holder, config.getInt(ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE), replaceStatsPlaceholders(p, target, stats, levelData, getMsg(p, Messages.PLAYER_STATS_GUI_INV_NAME), true));
            holder.inventory = inv;

            /* add custom items to gui */
            for (String s : config.getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_STATS_PATH).getKeys(false)) {
                /* skip inv size, it isn't a content */
                if (ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE.contains(s)) continue;
                String material = config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_MATERIAL.replace("%path%", s));
                if (material == null) continue;
                /* create new itemStack for content */
                ItemStack i = nms.createItemStack(material.toUpperCase(), 1, (short) config.getInt(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_DATA.replace("%path%", s)));
                i.setAmount(Math.max(1, config.getInt(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_AMOUNT.replace("%path%", s))));
                ItemMeta im = i.getItemMeta();
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                im.setDisplayName(replaceStatsPlaceholders(p, target, stats, levelData, getMsg(p, Messages.PLAYER_STATS_GUI_PATH + "-" + s + "-name"), true));
                List<String> lore = new ArrayList<>();
                for (String string : getList(p, Messages.PLAYER_STATS_GUI_PATH + "-" + s + "-lore")) {
                    lore.add(replaceStatsPlaceholders(p, target, stats, levelData, string, true));
                }
                im.setLore(lore);
                i.setItemMeta(im);
                inv.setItem(config.getInt(ConfigPath.GENERAL_CONFIGURATION_STATS_ITEMS_SLOT.replace("%path%", s)), i);
            }

            p.openInventory(inv);
            Sounds.playSound("stats-gui-open", p);
        });
    }

    public static String replaceStatsPlaceholders(Player player, @NotNull String s, boolean papiReplacements) {
        return replaceStatsPlaceholders(player, player, BedWars.getStatsManager().get(player.getUniqueId()), null, s, papiReplacements);
    }

    /**
     * @param viewer the player reading it, whose language formats the dates
     * @param target the player the stats belong to, or null when they are not on this server: the name then comes
     *               from the stats, the level from levelData, and the prefix and PlaceholderAPI are left out
     */
    public static String replaceStatsPlaceholders(Player viewer, @Nullable Player target, IPlayerStats stats,
                                                  @Nullable Object[] levelData, @NotNull String s, boolean papiReplacements) {
        Player player = viewer;
        String displayName = target != null ? target.getDisplayName() : stats.getName();
        String name = target != null ? target.getName() : stats.getName();
        if (displayName == null) displayName = "";
        if (name == null) name = "";

        if (s.contains("%bw_kills%"))
            s = s.replace("%bw_kills%", String.valueOf(stats.getKills()));
        if (s.contains("%bw_deaths%"))
            s = s.replace("%bw_deaths%", String.valueOf(stats.getDeaths()));
        if (s.contains("%bw_losses%"))
            s = s.replace("%bw_losses%", String.valueOf(stats.getLosses()));
        if (s.contains("%bw_wins%"))
            s = s.replace("%bw_wins%", String.valueOf(stats.getWins()));
        if (s.contains("%bw_final_kills%"))
            s = s.replace("%bw_final_kills%", String.valueOf(stats.getFinalKills()));
        if (s.contains("%bw_final_deaths%"))
            s = s.replace("%bw_final_deaths%", String.valueOf(stats.getFinalDeaths()));
        if (s.contains("%bw_beds%"))
            s = s.replace("%bw_beds%", String.valueOf(stats.getBedsDestroyed()));
        if (s.contains("%bw_games_played%"))
            s = s.replace("%bw_games_played%", String.valueOf(stats.getGamesPlayed()));
        if (s.contains("%bw_play_first%"))
            s = s.replace("%bw_play_first%", new SimpleDateFormat(getMsg(player, Messages.FORMATTING_STATS_DATE_FORMAT)).format(stats.getFirstPlay() != null ? Timestamp.from(stats.getFirstPlay()) : Timestamp.from(Instant.now())));
        if (s.contains("%bw_play_last%"))
            s = s.replace("%bw_play_last%", new SimpleDateFormat(getMsg(player, Messages.FORMATTING_STATS_DATE_FORMAT)).format(stats.getLastPlay() != null ? Timestamp.from(stats.getLastPlay()) : Timestamp.from(Instant.now())));
        if (s.contains("%bw_player%"))
            s = s.replace("%bw_player%", displayName);
        if (s.contains("%bw_playername%"))
            s = s.replace("%bw_playername%", name);
        if (s.contains("%bw_prefix%"))
            s = s.replace("%bw_prefix%", target != null ? BedWars.getChatSupport().getPrefix(target) : "");
        if (s.contains("%bw_assists%"))
            s = s.replace("%bw_assists%", String.valueOf(stats.getAssists()));
        if (s.contains("%bw_final_assists%"))
            s = s.replace("%bw_final_assists%", String.valueOf(stats.getFinalAssists()));
        if (s.contains("%bw_beds_lost%"))
            s = s.replace("%bw_beds_lost%", String.valueOf(stats.getBedsLost()));
        if (s.contains("%bw_winstreak%"))
            s = s.replace("%bw_winstreak%", String.valueOf(stats.getWinstreak()));
        if (s.contains("%bw_best_winstreak%"))
            s = s.replace("%bw_best_winstreak%", String.valueOf(stats.getBestWinstreak()));

        if (target != null) {
            if (s.contains("%bw_level%"))
                s = s.replace("%bw_level%", BedWars.getLevelSupport().getLevel(target));
            if (s.contains("%bw_level_unformatted%"))
                s = s.replace("%bw_level_unformatted%", String.valueOf(BedWars.getLevelSupport().getPlayerLevel(target)));
            if (s.contains("%bw_current_xp%"))
                s = s.replace("%bw_current_xp%", BedWars.getLevelSupport().getCurrentXpFormatted(target));
            if (s.contains("%bw_required_xp%"))
                s = s.replace("%bw_required_xp%", BedWars.getLevelSupport().getRequiredXpFormatted(target));
            if (s.contains("%bw_progress%"))
                s = s.replace("%bw_progress%", BedWars.getLevelSupport().getProgressBar(target));
        } else {
            // level data: 0 level, 1 xp, 2 level name, 3 next level cost
            boolean hasLevel = levelData != null && levelData.length >= 4;
            s = s.replace("%bw_level%", hasLevel && levelData[2] != null ? ChatColor.translateAlternateColorCodes('&', String.valueOf(levelData[2])) : "");
            s = s.replace("%bw_level_unformatted%", hasLevel ? String.valueOf(levelData[0]) : "0");
            s = s.replace("%bw_current_xp%", hasLevel ? String.valueOf(levelData[1]) : "0");
            s = s.replace("%bw_required_xp%", hasLevel ? String.valueOf(levelData[3]) : "0");
            s = s.replace("%bw_progress%", "");
        }

        s = replaceModeStatsPlaceholders(stats, s);

        if (target == null) return s;
        return papiReplacements ? SupportPAPI.getSupportPAPI().replace(target, s) : s;
    }

    private static String replaceModeStatsPlaceholders(IPlayerStats stats, String s) {
        for (ArenaMode mode : ArenaMode.values()) {
            String prefix = "%bw_" + mode.getGroup().toLowerCase() + "_";
            if (!s.contains(prefix)) continue;

            IModeStats modeStats = stats.getModeStats(mode.getGroup().toLowerCase());
            s = s.replace(prefix + "games_played%", String.valueOf(modeStats.getGamesPlayed()));
            s = s.replace(prefix + "wins%", String.valueOf(modeStats.getWins()));
            s = s.replace(prefix + "losses%", String.valueOf(modeStats.getLosses()));
            s = s.replace(prefix + "kills%", String.valueOf(modeStats.getKills()));
            s = s.replace(prefix + "deaths%", String.valueOf(modeStats.getDeaths()));
            s = s.replace(prefix + "assists%", String.valueOf(modeStats.getAssists()));
            s = s.replace(prefix + "final_kills%", String.valueOf(modeStats.getFinalKills()));
            s = s.replace(prefix + "final_deaths%", String.valueOf(modeStats.getFinalDeaths()));
            s = s.replace(prefix + "final_assists%", String.valueOf(modeStats.getFinalAssists()));
            s = s.replace(prefix + "beds%", String.valueOf(modeStats.getBedsDestroyed()));
            s = s.replace(prefix + "beds_lost%", String.valueOf(modeStats.getBedsLost()));
            s = s.replace(prefix + "winstreak%", String.valueOf(modeStats.getWinstreak()));
            s = s.replace(prefix + "best_winstreak%", String.valueOf(modeStats.getBestWinstreak()));
        }
        return s;
    }

    public static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
        } catch (Exception e) {
            try {
                Integer.parseInt(s);
            } catch (Exception ex) {
                try {
                    Long.parseLong(s);
                } catch (Exception exx) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if a location is outside the World Border
     *
     * @since API 8
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isOutsideOfBorder(Location l) {
        WorldBorder border = l.getWorld().getWorldBorder();
        double radius = (border.getSize() / 2) + border.getWarningDistance();
        Location center = border.getCenter();
        return center.distance(l) >= radius;
    }

    /**
     * Check if location is on a protected region
     */
    public static boolean isBuildProtected(Location l, IArena a) {
        for (Region region : a.getRegionsList()){
            if (region.isInRegion(l)){
                return true;
            }
        }
        return isOutsideOfBorder(l);
    }

    /**
     * Get lower location between 2 locations.
     *
     * @return a new Location instance.
     */
    public static Location minLoc(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) throw new IllegalStateException("As localizações não estão no mesmo mundo!");
        double x = Math.min(loc1.getX(), loc2.getX());
        double y = Math.min(loc1.getY(), loc2.getY());
        double z = Math.min(loc1.getZ(), loc2.getZ());
        return new Location(loc1.getWorld(), x, y, z);
    }

    /**
     * Get higher location between 2 locations.
     *
     * @return a new Location instance.
     */
    public static Location maxLoc(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) throw new IllegalStateException("As localizações não estão no mesmo mundo!");
        double x = Math.max(loc1.getX(), loc2.getX());
        double y = Math.max(loc1.getY(), loc2.getY());
        double z = Math.max(loc1.getZ(), loc2.getZ());
        return new Location(loc1.getWorld(), x, y, z);
    }
}
