package com.tomkeuper.bedwars.database;

import com.tomkeuper.bedwars.api.stats.IModeStats;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.stats.ModeStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ModeStatsIO {

    private static final String GLOBAL_MODE = "global";

    private ModeStatsIO() {
    }

    public static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mode_stats (uuid VARCHAR(36) NOT NULL, mode VARCHAR(32) NOT NULL, " +
                "games_played INTEGER DEFAULT 0, wins INTEGER DEFAULT 0, looses INTEGER DEFAULT 0, " +
                "kills INTEGER DEFAULT 0, deaths INTEGER DEFAULT 0, assists INTEGER DEFAULT 0, " +
                "final_kills INTEGER DEFAULT 0, final_deaths INTEGER DEFAULT 0, final_assists INTEGER DEFAULT 0, " +
                "beds_destroyed INTEGER DEFAULT 0, beds_lost INTEGER DEFAULT 0, " +
                "winstreak INTEGER DEFAULT 0, best_winstreak INTEGER DEFAULT 0, PRIMARY KEY (uuid, mode));";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public static void load(Connection connection, IPlayerStats stats) throws SQLException {
        String sql = "SELECT * FROM mode_stats WHERE uuid = ?;";
        Map<String, IModeStats> modes = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, stats.getUuid().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String mode = result.getString("mode");
                    if (mode == null) continue;

                    if (GLOBAL_MODE.equalsIgnoreCase(mode)) {
                        stats.setAssists(result.getInt("assists"));
                        stats.setFinalAssists(result.getInt("final_assists"));
                        stats.setBedsLost(result.getInt("beds_lost"));
                        stats.setBestWinstreak(result.getInt("best_winstreak"));
                        stats.setWinstreak(result.getInt("winstreak"));
                        continue;
                    }

                    IModeStats modeStats = new ModeStats(mode.toLowerCase());
                    modeStats.setGamesPlayed(result.getInt("games_played"));
                    modeStats.setWins(result.getInt("wins"));
                    modeStats.setLosses(result.getInt("looses"));
                    modeStats.setKills(result.getInt("kills"));
                    modeStats.setDeaths(result.getInt("deaths"));
                    modeStats.setAssists(result.getInt("assists"));
                    modeStats.setFinalKills(result.getInt("final_kills"));
                    modeStats.setFinalDeaths(result.getInt("final_deaths"));
                    modeStats.setFinalAssists(result.getInt("final_assists"));
                    modeStats.setBedsDestroyed(result.getInt("beds_destroyed"));
                    modeStats.setBedsLost(result.getInt("beds_lost"));
                    modeStats.setBestWinstreak(result.getInt("best_winstreak"));
                    modeStats.setWinstreak(result.getInt("winstreak"));
                    modes.put(modeStats.getMode(), modeStats);
                }
            }
        }

        stats.setModeStats(modes);
    }

    public static void save(Connection connection, IPlayerStats stats) throws SQLException {
        for (IModeStats modeStats : stats.getModeStats().values()) {
            write(connection, stats.getUuid(), modeStats.getMode(), modeStats.getGamesPlayed(), modeStats.getWins(),
                    modeStats.getLosses(), modeStats.getKills(), modeStats.getDeaths(), modeStats.getAssists(),
                    modeStats.getFinalKills(), modeStats.getFinalDeaths(), modeStats.getFinalAssists(),
                    modeStats.getBedsDestroyed(), modeStats.getBedsLost(), modeStats.getWinstreak(),
                    modeStats.getBestWinstreak());
        }

        write(connection, stats.getUuid(), GLOBAL_MODE, stats.getGamesPlayed(), stats.getWins(), stats.getLosses(),
                stats.getKills(), stats.getDeaths(), stats.getAssists(), stats.getFinalKills(), stats.getFinalDeaths(),
                stats.getFinalAssists(), stats.getBedsDestroyed(), stats.getBedsLost(), stats.getWinstreak(),
                stats.getBestWinstreak());
    }

    private static void write(Connection connection, UUID uuid, String mode, int gamesPlayed, int wins, int losses,
                              int kills, int deaths, int assists, int finalKills, int finalDeaths, int finalAssists,
                              int bedsDestroyed, int bedsLost, int winstreak, int bestWinstreak) throws SQLException {
        String update = "UPDATE mode_stats SET games_played=?, wins=?, looses=?, kills=?, deaths=?, assists=?, " +
                "final_kills=?, final_deaths=?, final_assists=?, beds_destroyed=?, beds_lost=?, winstreak=?, " +
                "best_winstreak=? WHERE uuid=? AND mode=?;";

        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setInt(1, gamesPlayed);
            statement.setInt(2, wins);
            statement.setInt(3, losses);
            statement.setInt(4, kills);
            statement.setInt(5, deaths);
            statement.setInt(6, assists);
            statement.setInt(7, finalKills);
            statement.setInt(8, finalDeaths);
            statement.setInt(9, finalAssists);
            statement.setInt(10, bedsDestroyed);
            statement.setInt(11, bedsLost);
            statement.setInt(12, winstreak);
            statement.setInt(13, bestWinstreak);
            statement.setString(14, uuid.toString());
            statement.setString(15, mode);
            if (statement.executeUpdate() > 0) return;
        }

        String insert = "INSERT INTO mode_stats (uuid, mode, games_played, wins, looses, kills, deaths, assists, " +
                "final_kills, final_deaths, final_assists, beds_destroyed, beds_lost, winstreak, best_winstreak) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, mode);
            statement.setInt(3, gamesPlayed);
            statement.setInt(4, wins);
            statement.setInt(5, losses);
            statement.setInt(6, kills);
            statement.setInt(7, deaths);
            statement.setInt(8, assists);
            statement.setInt(9, finalKills);
            statement.setInt(10, finalDeaths);
            statement.setInt(11, finalAssists);
            statement.setInt(12, bedsDestroyed);
            statement.setInt(13, bedsLost);
            statement.setInt(14, winstreak);
            statement.setInt(15, bestWinstreak);
            statement.executeUpdate();
        }
    }
}
