package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.sidebar.IScoreboardService;
import com.tomkeuper.bedwars.api.sidebar.ISidebar;
import com.tomkeuper.bedwars.api.tasks.PlayingTask;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.levels.internal.PlayerLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;

public class BoardManager implements IScoreboardService {

    private static final String TITLE_PLACEHOLDER = "%bw_scoreboard_title%";
    private static final String BELOW_NAME_TITLE = "&cHealth";

    @Getter
    private static BoardManager instance;

    private final PlaceholderRegistry placeholders = new PlaceholderRegistry();
    private final Map<String, SidebarContent> registeredSidebars = new ConcurrentHashMap<>();
    private final Map<UUID, SidebarBoard> boards = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tabPlayersPrefix = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tabPlayersSuffix = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> headPlayersPrefix = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> headPlayersSuffix = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tabPlayersTitle = new ConcurrentHashMap<>();
    private final Map<UUID, String> sortIds = new ConcurrentHashMap<>();
    private final Map<UUID, String> tabListNames = new ConcurrentHashMap<>();
    private final boolean legacy;
    private int sortIdCounter = 0;

    private BoardManager() {
        this.legacy = BedWars.nms.getVersion() <= 5;
    }

    public static boolean init() {
        if (instance != null) return true;
        if (Bukkit.getScoreboardManager() == null) return false;

        instance = new BoardManager();
        instance.registerPlaceholders();
        instance.registerLobbyScoreboards();
        Bukkit.getPluginManager().registerEvents(new BoardListener(), BedWars.plugin);
        instance.startRefreshTasks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            instance.giveTabFeatures(player, Arena.getArenaByPlayer(player), false);
        }
        return true;
    }

    public PlaceholderRegistry getPlaceholders() {
        return placeholders;
    }

    public void registerLobbyScoreboards() {
        if (!BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR)) return;
        BedWars.debug("Registrando as scoreboards do lobby...");

        for (Language language : Language.getLanguages()) {
            List<String> lines = language.l(Messages.SCOREBOARD_LOBBY);
            if (lines.isEmpty()) continue;
            lines.replaceAll(s -> s.isEmpty() ? " " : s);
            registeredSidebars.put("bw_lobby_" + language.getIso(), new SidebarContent(lines.subList(1, lines.size())));
        }
    }

    public List<String> registerArenaScoreboards(IArena arena) {
        BedWars.debug("Registrando a scoreboard da arena: " + arena.getDisplayName());
        List<String> names = new ArrayList<>();

        for (Language language : Language.getLanguages()) {
            names.add(register("bw_" + arena.getGroup() + "_waiting_" + language.getIso(),
                    getScoreboardLines(arena, language, "waiting", Messages.SCOREBOARD_DEFAULT_WAITING)));
            names.add(register("bw_" + arena.getGroup() + "_starting_" + language.getIso(),
                    getScoreboardLines(arena, language, "starting", Messages.SCOREBOARD_DEFAULT_STARTING)));
            names.add(register("bw_" + arena.getGroup() + "_playing_" + language.getIso(),
                    getScoreboardLines(arena, language, "playing", Messages.SCOREBOARD_DEFAULT_PLAYING)));
        }
        return names;
    }

    public void unregisterScoreboards(@Nullable List<String> names) {
        if (names == null) return;
        names.forEach(registeredSidebars::remove);
    }

    private String register(String name, List<String> lines) {
        if (!lines.isEmpty()) {
            registeredSidebars.put(name, new SidebarContent(lines.subList(1, lines.size())));
        }
        return name;
    }

    private List<String> getScoreboardLines(IArena arena, Language language, String phase, String path) {
        List<String> lines = Language.getScoreboard(language, "scoreboard." + arena.getGroup() + "." + phase, path);
        lines.replaceAll(s -> s.isEmpty() ? " " : s);
        return lines;
    }

    private SimpleDateFormat getDateFormat(Player player) {
        return new SimpleDateFormat(getMsg(player, Messages.FORMATTING_SCOREBOARD_DATE));
    }

    private SimpleDateFormat getNextEventDateFormat(Player player) {
        SimpleDateFormat nextEventDateFormat = new SimpleDateFormat(getMsg(player, Messages.FORMATTING_SCOREBOARD_NEXEVENT_TIMER));
        nextEventDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return nextEventDateFormat;
    }

    private void startRefreshTasks() {
        long titleTicks = toTicks(getInterval(ConfigPath.SB_CONFIG_SIDEBAR_TITLE_REFRESH_INTERVAL, "título da scoreboard"));
        long placeholderTicks = toTicks(getInterval(ConfigPath.SB_CONFIG_SIDEBAR_PLACEHOLDERS_REFRESH_INTERVAL, "placeholders"));
        long formattingTicks = toTicks(Math.min(
                getInterval(ConfigPath.SB_CONFIG_SIDEBAR_PREFIX_REFRESH_INTERVAL, "prefixo"),
                getInterval(ConfigPath.SB_CONFIG_SIDEBAR_SUFFIX_REFRESH_INTERVAL, "sufixo")));

        Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::refreshTitles, titleTicks, titleTicks);
        Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::refreshLines, placeholderTicks, placeholderTicks);
        Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this::refreshFormatting, formattingTicks, formattingTicks);
    }

    private int getInterval(String path, String name) {
        int interval = BedWars.config.getInt(path);
        if (interval < 50) {
            BedWars.plugin.getLogger().warning("O intervalo de atualização de " + name + " está definido como `" + interval + "` mas não pode ser menor que 50! Ajustando para 100 agora...");
            BedWars.config.set(path, 100);
            interval = 100;
        }
        return interval;
    }

    private static long toTicks(int milliseconds) {
        return Math.max(1L, milliseconds / 50L);
    }

    public void refreshTitles() {
        for (SidebarBoard board : boards.values()) {
            board.refreshTitle();
        }
    }

    public void refreshLines() {
        for (SidebarBoard board : boards.values()) {
            board.refreshLines();
        }
        for (IArena arena : Arena.getArenas()) {
            if (arena instanceof Arena) ((Arena) arena).refreshDragonBossBars();
        }
    }

    public void refreshFormatting() {
        for (Player target : Bukkit.getOnlinePlayers()) {
            refreshFormatting(target);
        }
    }

    public void refreshFormatting(@NotNull Player target) {
        IArena arena = Arena.getArenaByPlayer(target);

        String headPrefix = placeholders.parse(target, getPrefixHead(target));
        String headSuffix = placeholders.parse(target, getSuffixHead(target));
        String sortName = getSortName(target, arena);
        for (SidebarBoard board : boards.values()) {
            board.updatePlayerFormat(target, sortName, headPrefix, headSuffix);
        }

        String displayName = placeholders.parse(target, BedWars.config.getString(ConfigPath.SB_CONFIG_SIDEBAR_PLAYER_NAME));
        if (!BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_NAME_FORMATTING_ENABLED)) {
            setTabListName(target, displayName);
            return;
        }

        String tabPrefix = placeholders.parse(target, getPrefixTab(target));
        String tabSuffix = placeholders.parse(target, getSuffixTab(target));
        setTabListName(target, tabPrefix + displayName + tabSuffix);
    }

    private void setTabListName(Player target, String name) {
        if (name.equals(tabListNames.get(target.getUniqueId()))) return;
        tabListNames.put(target.getUniqueId(), name);
        BedWars.nms.setTabListName(target, name);
    }

    private String getSortName(Player player, @Nullable IArena arena) {
        int priority = 90;
        if (arena != null) {
            if (arena.isSpectator(player)) {
                priority = 80;
            } else {
                ITeam team = arena.getTeam(player);
                priority = team == null ? 70 : Math.min(69, arena.getTeams().indexOf(team));
            }
        }
        return String.format("%02d", priority) + getSortId(player);
    }

    private synchronized String getSortId(Player player) {
        return sortIds.computeIfAbsent(player.getUniqueId(), uuid -> {
            String id = Integer.toString(sortIdCounter++, 36);
            while (id.length() < 3) id = "0" + id;
            return id;
        });
    }

    private void registerPlaceholders() {
        BedWars.debug("Registrando os placeholders da scoreboard...");

        placeholders.registerPlayerPlaceholder("%bw_v_prefix%", player -> BedWars.getChatSupport().getPrefix(player));
        placeholders.registerPlayerPlaceholder("%bw_v_suffix%", player -> BedWars.getChatSupport().getSuffix(player));
        placeholders.registerPlayerPlaceholder("%bw_playername%", Player::getName);
        placeholders.registerPlayerPlaceholder("%bw_player%", Player::getDisplayName);
        placeholders.registerPlayerPlaceholder("%bw_player_health%", player -> String.valueOf((int) (player.getHealth() + BedWars.nms.getAbsorption(player))));
        placeholders.registerPlayerPlaceholder("%bw_money%", player -> String.valueOf(BedWars.getEconomy().getMoney(player)));
        placeholders.registerServerPlaceholder("%bw_server_ip%", () -> BedWars.config.getString(ConfigPath.GENERAL_CONFIG_PLACEHOLDERS_REPLACEMENTS_SERVER_IP));
        placeholders.registerServerPlaceholder("%bw_version%", () -> BedWars.plugin.getDescription().getVersion());
        placeholders.registerServerPlaceholder("%bw_server_id%", () -> BedWars.config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID));
        placeholders.registerPlayerPlaceholder("%bw_date%", player -> getDateFormat(player).format(new Date(System.currentTimeMillis())));
        placeholders.registerPlayerPlaceholder("%bw_progress%", player -> PlayerLevel.getLevelByPlayer(player.getUniqueId()).getProgress());
        placeholders.registerPlayerPlaceholder("%bw_level%", player -> PlayerLevel.getLevelByPlayer(player.getUniqueId()).getLevelName());
        placeholders.registerPlayerPlaceholder("%bw_level_unformatted%", player -> String.valueOf(PlayerLevel.getLevelByPlayer(player.getUniqueId()).getLevel()));
        placeholders.registerPlayerPlaceholder("%bw_current_xp%", player -> PlayerLevel.getLevelByPlayer(player.getUniqueId()).getFormattedCurrentXp());
        placeholders.registerPlayerPlaceholder("%bw_required_xp%", player -> PlayerLevel.getLevelByPlayer(player.getUniqueId()).getFormattedRequiredXp());
        placeholders.registerPlayerPlaceholder("%bw_map%", player -> Arena.getArenaByPlayer(player) == null ? "" : Arena.getArenaByPlayer(player).getDisplayName());
        placeholders.registerPlayerPlaceholder("%bw_map_name%", player -> Arena.getArenaByPlayer(player) == null ? "" : Arena.getArenaByPlayer(player).getArenaName());
        placeholders.registerPlayerPlaceholder("%bw_group%", player -> Arena.getArenaByPlayer(player) == null ? "" : Arena.getArenaByPlayer(player).getDisplayGroup(player));
        placeholders.registerPlayerPlaceholder("%bw_kills%", player -> {
            if (null != Arena.getArenaByPlayer(player)) return String.valueOf(Arena.getArenaByPlayer(player).getPlayerKills(player, false));
            return String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getKills());
        });
        placeholders.registerPlayerPlaceholder("%bw_total_kills%", player -> {
            if (null != Arena.getArenaByPlayer(player)) return String.valueOf(Arena.getArenaByPlayer(player).getPlayerTotalKills(player));
            return String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getTotalKills());
        });
        placeholders.registerPlayerPlaceholder("%bw_final_kills%", player -> {
            if (null != Arena.getArenaByPlayer(player)) return String.valueOf(Arena.getArenaByPlayer(player).getPlayerKills(player, true));
            return String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getFinalKills());
        });
        placeholders.registerPlayerPlaceholder("%bw_beds%", player -> {
            if (null != Arena.getArenaByPlayer(player)) return String.valueOf(Arena.getArenaByPlayer(player).getPlayerBedsDestroyed(player));
            return String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getBedsDestroyed());
        });
        placeholders.registerPlayerPlaceholder("%bw_deaths%", player -> {
            if (null != Arena.getArenaByPlayer(player)) return String.valueOf(Arena.getArenaByPlayer(player).getPlayerDeaths(player, false));
            return String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getDeaths());
        });
        placeholders.registerPlayerPlaceholder("%bw_final_deaths%", player -> String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getFinalDeaths()));
        placeholders.registerPlayerPlaceholder("%bw_wins%", player -> String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getWins()));
        placeholders.registerPlayerPlaceholder("%bw_losses%", player -> String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getLosses()));
        placeholders.registerPlayerPlaceholder("%bw_games_played%", player -> String.valueOf(BedWars.getStatsManager().get(player.getUniqueId()).getGamesPlayed()));
        placeholders.registerPlayerPlaceholder("%bw_next_event%", this::getNextEventName);
        placeholders.registerPlayerPlaceholder("%bw_on%", player -> String.valueOf(getOnlinePlayers(player)));
        placeholders.registerPlayerPlaceholder("%bw_max%", player -> Arena.getArenaByPlayer(player) == null ? "" : String.valueOf(Arena.getArenaByPlayer(player).getMaxPlayers()));
        placeholders.registerPlayerPlaceholder("%bw_time%", player -> {
            Arena arena = (Arena) Arena.getArenaByPlayer(player);
            if (null == arena) return "";
            if (arena.getStatus() == GameState.playing || arena.getStatus() == GameState.restarting) {
                return getNextEventTime(arena, player);
            } else if (arena.getStatus() == GameState.starting) {
                if (arena.getStartingTask() != null) {
                    return String.valueOf(arena.getStartingTask().getCountdown() + 1);
                }
            }
            return getNextEventDateFormat(player).format(new Date(System.currentTimeMillis()));
        });

        placeholders.registerPlayerPlaceholder("%bw_arena_status%", player -> {
            IArena arena = Arena.getArenaByPlayer(player);
            if (null == arena) return "";
            return arena.getStatus().toString();
        });
        placeholders.registerPlayerPlaceholder("%bw_team%", player -> {
            IArena arena = Arena.getArenaByPlayer(player);
            return null == arena ? "" : null == arena.getTeam(player) ? "" : arena.getTeam(player).getColor().chat() + arena.getTeam(player).getDisplayName(Language.getPlayerLanguage(player));
        });
        placeholders.registerPlayerPlaceholder("%bw_team_letter%", player -> {
            IArena arena = Arena.getArenaByPlayer(player);
            return null == arena ? "" : null == arena.getTeam(player) ? "" : (arena.getTeam(player).getDisplayName(Language.getPlayerLanguage(player)).substring(0, 1));
        });
        placeholders.registerPlayerPlaceholder("%bw_team_color%", player -> {
            IArena arena = Arena.getArenaByPlayer(player);
            return null == arena ? "" : null == arena.getTeam(player) ? "" : String.valueOf(arena.getTeam(player).getColor().chat());
        });

        placeholders.registerPlayerPlaceholder("%bw_prefix_tab%", this::getPrefixTab);
        placeholders.registerPlayerPlaceholder("%bw_suffix_tab%", this::getSuffixTab);
        placeholders.registerPlayerPlaceholder("%bw_prefix_head%", this::getPrefixHead);
        placeholders.registerPlayerPlaceholder("%bw_suffix_head%", this::getSuffixHead);

        placeholders.registerPlayerPlaceholder(TITLE_PLACEHOLDER, this::getSidebarTitleFrame);

        for (int i = 1; i <= 32; i++) {
            int teamNumber = i;
            placeholders.registerPlayerPlaceholder("%bw_team_" + i + "%", player -> getTeamPlaceholder(player, teamNumber));
        }
    }

    private String getSidebarTitleFrame(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        int index = tabPlayersTitle.getOrDefault(player.getUniqueId(), 0);
        List<String> lines = null;

        if (null == arena) {
            if (player.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
                lines = Language.getList(player, Messages.SCOREBOARD_LOBBY);
            }
        } else {
            if (arena.getStatus() == GameState.waiting) {
                lines = Language.getScoreboard(player, "scoreboard." + arena.getGroup() + ".waiting", Messages.SCOREBOARD_DEFAULT_WAITING);
            } else if (arena.getStatus() == GameState.starting) {
                lines = Language.getScoreboard(player, "scoreboard." + arena.getGroup() + ".starting", Messages.SCOREBOARD_DEFAULT_STARTING);
            } else if (arena.getStatus() == GameState.playing || arena.getStatus() == GameState.restarting) {
                lines = Language.getScoreboard(player, "scoreboard." + arena.getGroup() + ".playing", Messages.SCOREBOARD_DEFAULT_PLAYING);
            }
        }

        if (lines == null || lines.isEmpty()) return "";
        String[] titleArray = lines.get(0).split(",");

        if (index + 1 >= titleArray.length) {
            tabPlayersTitle.put(player.getUniqueId(), 0);
            index = 0;
        } else {
            tabPlayersTitle.put(player.getUniqueId(), index + 1);
        }
        String title = titleArray[index];
        return null == title ? "" : title;
    }

    @Override
    public void giveTabFeatures(@NotNull Player player, @Nullable IArena arena, boolean delay) {
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            String arenaDisplayname = (arena != null) ? arena.getDisplayName() : "null";
            BedWars.debug("giveTabFeatures() player: " + player.getDisplayName() + " arena: " + arenaDisplayname);

            if (!player.isOnline()) return;

            SidebarBoard board = getOrCreateBoard(player);
            if (board == null) return;

            if ((arena == null && !BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_LOBBY_SIDEBAR))
                    || (arena != null && !BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_USE_GAME_SIDEBAR))) {
                board.setName("");
                board.setContent(TITLE_PLACEHOLDER, Collections.emptyList());
                board.setHealthDisplay(false, "");
                setHeaderFooter(player, arena);
                refreshFormatting(player);
                return;
            }

            String scoreboardName;
            GameState arenaStatus = (arena != null) ? arena.getStatus() : null;
            Language playerLanguage = Language.getPlayerLanguage(player);

            if (arenaStatus == null) {
                scoreboardName = "bw_lobby_" + playerLanguage.getIso();
            } else {
                switch (arenaStatus) {
                    case waiting:
                        scoreboardName = "bw_" + arena.getGroup() + "_waiting_" + playerLanguage.getIso();
                        break;
                    case starting:
                        scoreboardName = "bw_" + arena.getGroup() + "_starting_" + playerLanguage.getIso();
                        break;
                    case playing:
                    case restarting:
                        scoreboardName = "bw_" + arena.getGroup() + "_playing_" + playerLanguage.getIso();
                        break;
                    default:
                        scoreboardName = "bw_lobby_" + playerLanguage.getIso();
                }
            }

            SidebarContent content = registeredSidebars.get(scoreboardName);
            board.setName(scoreboardName);
            board.setContent(TITLE_PLACEHOLDER, content == null ? Collections.emptyList() : content.getLines());
            board.setHealthDisplay(arena != null && arena.getStatus() == GameState.playing,
                    ChatColor.translateAlternateColorCodes('&', BELOW_NAME_TITLE));

            setHeaderFooter(player, arena);
            refreshFormatting(player);
        }, delay ? 5 : 0);
    }

    private String getTeamPlaceholder(Player player, int teamNumber) {
        Arena arena = (Arena) Arena.getArenaByPlayer(player);
        if (arena == null) return null;
        Language language = Language.getPlayerLanguage(player);
        String genericTeamFormat = language.m(Messages.FORMATTING_SCOREBOARD_TEAM_GENERIC);
        ITeam team;
        try {
            team = arena.getTeams().get(teamNumber - 1);
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
        String teamName = team.getDisplayName(language);
        if (arena.getTeams().size() >= teamNumber) {
            return genericTeamFormat
                    .replace("%bw_team_letter%", String.valueOf(teamName.length() != 0 ? teamName.charAt(0) : ""))
                    .replace("%bw_team_color%", team.getColor().chat().toString())
                    .replace("%bw_team_name%", teamName)
                    .replace("%bw_team_status%", getTeamStatus(team, player));
        } else {
            return null;
        }
    }

    private String getTeamStatus(ITeam currentTeam, Player player) {
        String result;
        if (currentTeam.isBedDestroyed()) {
            if (currentTeam.getSize() > 0) {
                result = getMsg(player, Messages.FORMATTING_SCOREBOARD_BED_DESTROYED)
                        .replace("%bw_players_remaining%", String.valueOf(currentTeam.getSize()));
            } else {
                result = getMsg(player, Messages.FORMATTING_SCOREBOARD_TEAM_ELIMINATED);
            }
        } else {
            result = getMsg(player, Messages.FORMATTING_SCOREBOARD_TEAM_ALIVE);
        }
        if (currentTeam.isMember(player)) {
            result += getMsg(player, Messages.FORMATTING_SCOREBOARD_YOUR_TEAM);
        }
        return result;
    }

    @Override
    public void remove(@NotNull Player player) {
        SidebarBoard board = boards.remove(player.getUniqueId());
        if (board != null) board.remove();
    }

    public void cleanupPlayer(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        tabPlayersPrefix.remove(playerId);
        tabPlayersSuffix.remove(playerId);
        headPlayersPrefix.remove(playerId);
        headPlayersSuffix.remove(playerId);
        tabPlayersTitle.remove(playerId);
        sortIds.remove(playerId);
        tabListNames.remove(playerId);

        SidebarBoard board = boards.remove(playerId);
        if (board != null) board.remove();

        for (SidebarBoard other : boards.values()) {
            other.removePlayerFormat(player);
        }
    }

    @Nullable
    public SidebarBoard getBoard(@NotNull Player player) {
        return boards.get(player.getUniqueId());
    }

    @Nullable
    public SidebarBoard getOrCreateBoard(@NotNull Player player) {
        if (Bukkit.getScoreboardManager() == null) return null;
        return boards.computeIfAbsent(player.getUniqueId(), uuid -> new SidebarBoard(player, placeholders, legacy));
    }

    public String getPrefixTab(Player player) {
        return getPrefix(player, "Tab");
    }

    public String getSuffixTab(Player player) {
        return getSuffix(player, "Tab");
    }

    public String getPrefixHead(Player player) {
        return getPrefix(player, "Head");
    }

    public String getSuffixHead(Player player) {
        return getSuffix(player, "Head");
    }

    public String getPrefix(Player player, String type) {
        IArena arena = Arena.getArenaByPlayer(player);
        boolean tab = type.equalsIgnoreCase("tab");
        Map<UUID, Integer> indexes = tab ? tabPlayersPrefix : headPlayersPrefix;
        int currentIndex = indexes.getOrDefault(player.getUniqueId(), 0);
        List<String> fixList;

        if (arena == null) {
            fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_LOBBY : Messages.FORMATTING_SCOREBOARD_HEAD_PREFIX_LOBBY);
        } else if (arena.isSpectator(player)) {
            fixList = Language.getList(player, Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_SPECTATOR);
        } else {
            switch (arena.getStatus()) {
                case playing:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_PLAYING : Messages.FORMATTING_SCOREBOARD_HEAD_PREFIX_PLAYING);
                    break;
                case waiting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_WAITING : Messages.FORMATTING_SCOREBOARD_HEAD_PREFIX_WAITING);
                    break;
                case starting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_STARTING : Messages.FORMATTING_SCOREBOARD_HEAD_PREFIX_STARTING);
                    break;
                case restarting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_PREFIX_RESTARTING : Messages.FORMATTING_SCOREBOARD_HEAD_PREFIX_RESTARTING);
                    break;
                default:
                    BedWars.debug("Estado de jogo não tratado para o prefixo do BedWars");
                    fixList = Collections.singletonList("");
                    break;
            }
        }

        return nextFrame(player, currentIndex, fixList, indexes);
    }

    public String getSuffix(Player player, String type) {
        IArena arena = Arena.getArenaByPlayer(player);
        boolean tab = type.equalsIgnoreCase("tab");
        Map<UUID, Integer> indexes = tab ? tabPlayersSuffix : headPlayersSuffix;
        int currentIndex = indexes.getOrDefault(player.getUniqueId(), 0);
        List<String> fixList;

        if (arena == null) {
            fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_LOBBY : Messages.FORMATTING_SCOREBOARD_HEAD_SUFFIX_LOBBY);
        } else if (arena.isSpectator(player)) {
            fixList = Language.getList(player, Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_SPECTATOR);
        } else {
            switch (arena.getStatus()) {
                case playing:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_PLAYING : Messages.FORMATTING_SCOREBOARD_HEAD_SUFFIX_PLAYING);
                    break;
                case waiting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_WAITING : Messages.FORMATTING_SCOREBOARD_HEAD_SUFFIX_WAITING);
                    break;
                case starting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_STARTING : Messages.FORMATTING_SCOREBOARD_HEAD_SUFFIX_STARTING);
                    break;
                case restarting:
                    fixList = Language.getList(player, tab ? Messages.FORMATTING_SCOREBOARD_TAB_SUFFIX_RESTARTING : Messages.FORMATTING_SCOREBOARD_HEAD_SUFFIX_RESTARTING);
                    break;
                default:
                    BedWars.debug("Estado de jogo não tratado para o sufixo do BedWars");
                    fixList = Collections.singletonList("");
                    break;
            }
        }

        return nextFrame(player, currentIndex, fixList, indexes);
    }

    @NotNull
    private String nextFrame(Player player, int currentIndex, List<String> fixList, Map<UUID, Integer> indexes) {
        if (currentIndex + 1 >= fixList.size()) {
            indexes.put(player.getUniqueId(), 0);
            currentIndex = 0;
        } else {
            indexes.put(player.getUniqueId(), currentIndex + 1);
        }

        String frame = (fixList.isEmpty() || currentIndex >= fixList.size()) ? null : fixList.get(currentIndex);
        return (frame == null) ? "" : frame;
    }

    @NotNull
    private String getNextEventName(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return "-";
        String st = "-";
        switch (arena.getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
                st = getMsg(player, Messages.NEXT_EVENT_EMERALD_UPGRADE_II);
                break;
            case EMERALD_GENERATOR_TIER_III:
                st = getMsg(player, Messages.NEXT_EVENT_EMERALD_UPGRADE_III);
                break;
            case DIAMOND_GENERATOR_TIER_II:
                st = getMsg(player, Messages.NEXT_EVENT_DIAMOND_UPGRADE_II);
                break;
            case DIAMOND_GENERATOR_TIER_III:
                st = getMsg(player, Messages.NEXT_EVENT_DIAMOND_UPGRADE_III);
                break;
            case GAME_END:
                st = getMsg(player, Messages.NEXT_EVENT_GAME_END);
                break;
            case BEDS_DESTROY:
                st = getMsg(player, Messages.NEXT_EVENT_BEDS_DESTROY);
                break;
            case ENDER_DRAGON:
                st = getMsg(player, Messages.NEXT_EVENT_DRAGON_SPAWN);
                break;
        }

        return st;
    }

    @NotNull
    private String getNextEventTime(Arena arena, Player player) {
        if (arena == null) return getNextEventDateFormat(player).format((0L));
        long time = 0L;
        PlayingTask playingTask = arena.getPlayingTask();
        switch (arena.getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III:
                time = (arena.upgradeEmeraldsCount) * 1000L;
                break;
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III:
                time = (arena.upgradeDiamondsCount) * 1000L;
                break;
            case GAME_END:
                if (null == playingTask) {
                    break;
                }
                time = (playingTask.getGameEndCountdown()) * 1000L;
                break;
            case BEDS_DESTROY:
                if (null == playingTask) {
                    break;
                }
                time = (arena.getPlayingTask().getBedsDestroyCountdown()) * 1000L;
                break;
            case ENDER_DRAGON:
                if (null == playingTask) {
                    break;
                }
                time = (arena.getPlayingTask().getDragonSpawnCountdown()) * 1000L;
                break;
        }
        return getNextEventDateFormat(player).format(new Date(time));
    }

    private int getOnlinePlayers(Player player) {
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return Bukkit.getOnlinePlayers().size();
        return arena.getPlayers().size();
    }

    private void setHeaderFooter(Player player, IArena arena) {
        if (isTabFormattingDisabled(arena)) {
            return;
        }
        Language lang = Language.getPlayerLanguage(player);

        if (null == arena) {
            sendHeaderFooter(player, text(lang, Messages.FORMATTING_SIDEBAR_TAB_HEADER_LOBBY), text(lang, Messages.FORMATTING_SIDEBAR_TAB_FOOTER_LOBBY));
            return;
        }
        if (arena.isSpectator(player)) {
            sendHeaderFooter(player, text(lang, Messages.FORMATTING_SIDEBAR_TAB_HEADER_SPECTATOR), text(lang, Messages.FORMATTING_SIDEBAR_TAB_FOOTER_SPECTATOR));
            return;
        }

        String headerPath = null;
        String footerPath = null;

        switch (arena.getStatus()) {
            case waiting:
                headerPath = Messages.FORMATTING_SIDEBAR_TAB_HEADER_WAITING;
                footerPath = Messages.FORMATTING_SIDEBAR_TAB_FOOTER_WAITING;
                break;
            case starting:
                headerPath = Messages.FORMATTING_SIDEBAR_TAB_HEADER_STARTING;
                footerPath = Messages.FORMATTING_SIDEBAR_TAB_FOOTER_STARTING;
                break;
            case playing:
                headerPath = Messages.FORMATTING_SIDEBAR_TAB_HEADER_PLAYING;
                footerPath = Messages.FORMATTING_SIDEBAR_TAB_FOOTER_PLAYING;
                break;
            case restarting:
                headerPath = Messages.FORMATTING_SIDEBAR_TAB_HEADER_RESTARTING;
                footerPath = Messages.FORMATTING_SIDEBAR_TAB_FOOTER_RESTARTING;
                break;
        }

        if (headerPath == null || footerPath == null) return;
        sendHeaderFooter(player, text(lang, headerPath), text(lang, footerPath));
    }

    private String text(Language language, String path) {
        List<String> lines = language.getYml().getStringList(path);
        if (lines != null && !lines.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', String.join("\n", lines));
        }
        return language.m(path);
    }

    private void sendHeaderFooter(Player player, String header, String footer) {
        BedWars.nms.setTabHeaderFooter(player, placeholders.parse(player, header), placeholders.parse(player, footer));
    }

    @Override
    public boolean isTabFormattingDisabled(IArena arena) {
        if (null == arena) {

            if (BedWars.getServerType() == ServerType.SHARED) {
                if (BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY) &&
                        !BedWars.config.getLobbyWorldName().trim().isEmpty()) {

                    World lobby = Bukkit.getWorld(BedWars.config.getLobbyWorldName());
                    return null != lobby;
                }
            }

            return !BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_LOBBY);
        }
        if (arena.getStatus() == GameState.playing && BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_PLAYING)) {
            return false;
        }

        if (arena.getStatus() == GameState.starting && BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_STARTING)) {
            return false;
        }

        if (arena.getStatus() == GameState.waiting && BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_WAITING)) {
            return false;
        }

        return arena.getStatus() != GameState.restarting || !BedWars.config.getBoolean(ConfigPath.SB_CONFIG_SIDEBAR_LIST_FORMAT_RESTARTING);
    }

    @Override
    public @Nullable ISidebar getScoreboard(@NotNull Player player) {
        return boards.get(player.getUniqueId());
    }

    private static class SidebarContent {

        private final List<String> lines;

        SidebarContent(List<String> lines) {
            this.lines = new ArrayList<>(lines);
        }

        List<String> getLines() {
            return lines;
        }
    }
}
