package com.tomkeuper.bedwars.api.stats;

import org.jetbrains.annotations.NotNull;

public interface IModeStats {

    @NotNull
    String getMode();

    int getGamesPlayed();

    void setGamesPlayed(int gamesPlayed);

    int getWins();

    void setWins(int wins);

    int getLosses();

    void setLosses(int losses);

    int getKills();

    void setKills(int kills);

    int getDeaths();

    void setDeaths(int deaths);

    int getAssists();

    void setAssists(int assists);

    int getFinalKills();

    void setFinalKills(int finalKills);

    int getFinalDeaths();

    void setFinalDeaths(int finalDeaths);

    int getFinalAssists();

    void setFinalAssists(int finalAssists);

    int getBedsDestroyed();

    void setBedsDestroyed(int bedsDestroyed);

    int getBedsLost();

    void setBedsLost(int bedsLost);

    int getWinstreak();

    void setWinstreak(int winstreak);

    int getBestWinstreak();

    void setBestWinstreak(int bestWinstreak);
}
