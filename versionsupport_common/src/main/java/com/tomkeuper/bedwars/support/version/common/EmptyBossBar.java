package com.tomkeuper.bedwars.support.version.common;

import com.tomkeuper.bedwars.api.sidebar.IBossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EmptyBossBar implements IBossBar {

    @Override
    public void addPlayer(@NotNull Player player) {
    }

    @Override
    public void removePlayer(@NotNull Player player) {
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public void setTitle(@NotNull String title) {
    }

    @Override
    public void setProgress(double progress) {
    }

    @Override
    public void remove() {
    }
}
