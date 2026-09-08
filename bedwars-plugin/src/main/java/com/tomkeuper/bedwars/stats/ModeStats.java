package com.tomkeuper.bedwars.stats;

import com.tomkeuper.bedwars.api.stats.IModeStats;
import org.jetbrains.annotations.NotNull;

public class ModeStats implements IModeStats {

    private final String mode;

    private int gamesPlayed;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int assists;
    private int finalKills;
    private int finalDeaths;
    private int finalAssists;
    private int bedsDestroyed;
    private int bedsLost;
    private int winstreak;
    private int bestWinstreak;

    public ModeStats(@NotNull String mode) {
        this.mode = mode;
    }

    @Override
    public @NotNull String getMode() {
        return mode;
    }

    @Override
    public int getGamesPlayed() {
        return gamesPlayed;
    }

    @Override
    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    @Override
    public int getWins() {
        return wins;
    }

    @Override
    public void setWins(int wins) {
        this.wins = wins;
    }

    @Override
    public int getLosses() {
        return losses;
    }

    @Override
    public void setLosses(int losses) {
        this.losses = losses;
    }

    @Override
    public int getKills() {
        return kills;
    }

    @Override
    public void setKills(int kills) {
        this.kills = kills;
    }

    @Override
    public int getDeaths() {
        return deaths;
    }

    @Override
    public void setDeaths(int deaths) {
        this.deaths = deaths;
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
    public int getFinalKills() {
        return finalKills;
    }

    @Override
    public void setFinalKills(int finalKills) {
        this.finalKills = finalKills;
    }

    @Override
    public int getFinalDeaths() {
        return finalDeaths;
    }

    @Override
    public void setFinalDeaths(int finalDeaths) {
        this.finalDeaths = finalDeaths;
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
    public int getBedsDestroyed() {
        return bedsDestroyed;
    }

    @Override
    public void setBedsDestroyed(int bedsDestroyed) {
        this.bedsDestroyed = bedsDestroyed;
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
}
