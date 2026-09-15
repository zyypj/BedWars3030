package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.api.sidebar.ISidebar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SidebarBoard implements ISidebar {

    private static final int MAX_LINES = 15;
    private static final String SIDEBAR_OBJECTIVE = "bw_sidebar";
    private static final String BELOW_NAME_OBJECTIVE = "bw_health";
    private static final String TAB_HEALTH_OBJECTIVE = "bw_tabhealth";

    private final Player player;
    private final PlaceholderRegistry placeholders;
    private final boolean legacy;
    private final Scoreboard scoreboard;
    private final Objective sidebar;
    private final String[] entries = new String[MAX_LINES];
    private final Team[] lineTeams = new Team[MAX_LINES];
    private final String[] renderedLines = new String[MAX_LINES];
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private final Map<UUID, String> playerTeamNames = new HashMap<>();
    private final Map<UUID, String> playerFormats = new HashMap<>();

    private String name = "";
    private String titleTemplate = "";
    private List<String> lineTemplates = new ArrayList<>();
    private String renderedTitle = "";
    private int visibleLines = 0;
    private Objective belowName;
    private Objective tabHealth;

    public SidebarBoard(@NotNull Player player, @NotNull PlaceholderRegistry placeholders, boolean legacy) {
        this.player = player;
        this.placeholders = placeholders;
        this.legacy = legacy;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.sidebar = scoreboard.registerNewObjective(SIDEBAR_OBJECTIVE, "dummy");
        this.sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.sidebar.setDisplayName("");

        for (int i = 0; i < MAX_LINES; i++) {
            entries[i] = ChatColor.values()[i].toString() + ChatColor.RESET;
            Team lineTeam = scoreboard.registerNewTeam("bw_line_" + i);
            lineTeam.addEntry(entries[i]);
            lineTeams[i] = lineTeam;
        }

        player.setScoreboard(scoreboard);
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull List<String> getLines() {
        List<String> lines = new ArrayList<>(visibleLines);
        for (int i = 0; i < visibleLines; i++) {
            if (renderedLines[i] != null) lines.add(renderedLines[i]);
        }
        return lines;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    public void setContent(@NotNull String title, @NotNull List<String> lines) {
        this.titleTemplate = title;
        this.lineTemplates = new ArrayList<>(lines);
        clearLines();
        refreshTitle();
        refreshLines();
    }

    @Override
    public void refreshTitle() {
        String rendered = truncate(placeholders.parse(player, titleTemplate), legacy ? 32 : 128);
        if (rendered.equals(renderedTitle)) return;
        renderedTitle = rendered;
        sidebar.setDisplayName(rendered);
    }

    @Override
    public void refreshLines() {
        int index = 0;
        for (String template : lineTemplates) {
            if (index >= MAX_LINES) break;
            String rendered = placeholders.parse(player, template);
            if (rendered.isEmpty()) continue;
            applyLine(index, rendered);
            index++;
        }
        for (int i = index; i < visibleLines; i++) {
            scoreboard.resetScores(entries[i]);
            renderedLines[i] = null;
        }
        visibleLines = index;
    }

    @Override
    public void remove() {
        clearLines();
        for (Team team : playerTeams.values()) {
            unregisterTeam(team);
        }
        playerTeams.clear();
        playerTeamNames.clear();
        playerFormats.clear();
        if (player.isOnline() && Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void setHealthDisplay(boolean enabled, @NotNull String belowNameTitle) {
        if (enabled) {
            if (belowName == null) {
                belowName = scoreboard.registerNewObjective(BELOW_NAME_OBJECTIVE, "health");
                belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
                belowName.setDisplayName(truncate(belowNameTitle, legacy ? 32 : 128));
            }
            if (tabHealth == null) {
                tabHealth = scoreboard.registerNewObjective(TAB_HEALTH_OBJECTIVE, "health");
                tabHealth.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
            return;
        }
        if (belowName != null) {
            unregisterObjective(belowName);
            belowName = null;
        }
        if (tabHealth != null) {
            unregisterObjective(tabHealth);
            tabHealth = null;
        }
    }

    public void updatePlayerFormat(@NotNull Player target, @NotNull String teamName, @NotNull String prefix, @NotNull String suffix) {
        UUID targetId = target.getUniqueId();
        String limitedPrefix = truncate(prefix, legacy ? 16 : 64);
        String limitedSuffix = truncate(suffix, legacy ? 16 : 64);

        Team team = playerTeams.get(targetId);
        if (team == null || !teamName.equals(playerTeamNames.get(targetId))) {
            if (team != null) unregisterTeam(team);
            team = scoreboard.getTeam(teamName);
            if (team == null) team = scoreboard.registerNewTeam(teamName);
            team.addEntry(target.getName());
            playerTeams.put(targetId, team);
            playerTeamNames.put(targetId, teamName);
            playerFormats.remove(targetId);
        }

        String format = limitedPrefix + ChatColor.COLOR_CHAR + limitedSuffix;
        if (format.equals(playerFormats.get(targetId))) return;
        playerFormats.put(targetId, format);
        team.setPrefix(limitedPrefix);
        team.setSuffix(limitedSuffix);
    }

    public void removePlayerFormat(@NotNull Player target) {
        UUID targetId = target.getUniqueId();
        Team team = playerTeams.remove(targetId);
        playerTeamNames.remove(targetId);
        playerFormats.remove(targetId);
        if (team != null) unregisterTeam(team);
    }

    private void applyLine(int index, @NotNull String text) {
        if (!text.equals(renderedLines[index])) {
            renderedLines[index] = text;
            applyText(lineTeams[index], text);
        }
        sidebar.getScore(entries[index]).setScore(MAX_LINES - index);
    }

    private void clearLines() {
        for (int i = 0; i < MAX_LINES; i++) {
            if (renderedLines[i] != null) {
                scoreboard.resetScores(entries[i]);
                renderedLines[i] = null;
            }
        }
        visibleLines = 0;
    }

    private void applyText(@NotNull Team team, @NotNull String text) {
        int limit = legacy ? 16 : 64;
        if (text.length() <= limit) {
            team.setPrefix(text);
            team.setSuffix("");
            return;
        }

        int split = limit;
        if (text.charAt(split - 1) == ChatColor.COLOR_CHAR) split--;
        String prefix = text.substring(0, split);
        String suffix = ChatColor.getLastColors(prefix) + text.substring(split);
        team.setPrefix(prefix);
        team.setSuffix(truncate(suffix, limit));
    }

    @NotNull
    private static String truncate(@NotNull String text, int limit) {
        if (text.length() <= limit) return text;
        String cut = text.substring(0, limit);
        if (cut.charAt(cut.length() - 1) == ChatColor.COLOR_CHAR) cut = cut.substring(0, cut.length() - 1);
        return cut;
    }

    private static void unregisterTeam(@NotNull Team team) {
        try {
            team.unregister();
        } catch (IllegalStateException alreadyGone) {
            return;
        }
    }

    private static void unregisterObjective(@NotNull Objective objective) {
        try {
            objective.unregister();
        } catch (IllegalStateException alreadyGone) {
            return;
        }
    }
}
