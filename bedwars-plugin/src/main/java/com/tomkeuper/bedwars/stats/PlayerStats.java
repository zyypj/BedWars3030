package com.tomkeuper.bedwars.stats;

import com.tomkeuper.bedwars.api.stats.IModeStats;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStats implements IPlayerStats {

    private final UUID uuid;

    private String name;
    private Instant firstPlay;
    private Instant lastPlay;
    private int wins;
    private int kills;
    private int finalKills;
    private int totalKills;
    private int losses;
    private int deaths;
    private int finalDeaths;
    private int bedsDestroyed;
    private int gamesPlayed;
    private int assists;
    private int finalAssists;
    private int bedsLost;
    private int winstreak;
    private int bestWinstreak;
    private final Map<String, IModeStats> modeStats = new ConcurrentHashMap<>();

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Instant getFirstPlay() {
        return firstPlay;
    }

    public void setFirstPlay(Instant firstPlay) {
        this.firstPlay = firstPlay;
    }

    public Instant getLastPlay() {
        return lastPlay;
    }

    public void setLastPlay(Instant lastPlay) {
        this.lastPlay = lastPlay;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
        this.totalKills = kills + finalKills;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public void setFinalKills(int finalKills) {
        this.finalKills = finalKills;
        this.totalKills = kills + finalKills;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getFinalDeaths() {
        return finalDeaths;
    }

    public void setFinalDeaths(int finalDeaths) {
        this.finalDeaths = finalDeaths;
    }

    public int getBedsDestroyed() {
        return bedsDestroyed;
    }

    public void setBedsDestroyed(int bedsDestroyed) {
        this.bedsDestroyed = bedsDestroyed;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamePlayed) {
        this.gamesPlayed = gamePlayed;
    }

    public int getTotalKills() {
        return totalKills;
    }
    @Override
    public int getAssists() {
        return assists;
    }

    @Override
    public void setAssists(int assists) {
        this.assists = assists;
    }

    @Override
    public int getFinalAssists() {
        return finalAssists;
    }

    @Override
    public void setFinalAssists(int finalAssists) {
        this.finalAssists = finalAssists;
    }

    @Override
    public int getBedsLost() {
        return bedsLost;
    }

    @Override
    public void setBedsLost(int bedsLost) {
        this.bedsLost = bedsLost;
    }

    @Override
    public int getWinstreak() {
        return winstreak;
    }

    @Override
    public void setWinstreak(int winstreak) {
        this.winstreak = winstreak;
        if (winstreak > bestWinstreak) this.bestWinstreak = winstreak;
    }

    @Override
    public int getBestWinstreak() {
        return bestWinstreak;
    }

    @Override
    public void setBestWinstreak(int bestWinstreak) {
        this.bestWinstreak = bestWinstreak;
    }

    @Override
    public @NotNull IModeStats getModeStats(@NotNull String mode) {
        return modeStats.computeIfAbsent(mode.toLowerCase(), ModeStats::new);
    }

    @Override
    public @NotNull Map<String, IModeStats> getModeStats() {
        return modeStats;
    }

    @Override
    public void setModeStats(@NotNull Map<String, IModeStats> modeStats) {
        this.modeStats.clear();
        this.modeStats.putAll(modeStats);
    }
}
