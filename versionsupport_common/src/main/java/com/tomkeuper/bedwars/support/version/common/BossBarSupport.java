package com.tomkeuper.bedwars.support.version.common;

import com.tomkeuper.bedwars.api.sidebar.IBossBar;
import org.jetbrains.annotations.NotNull;

public final class BossBarSupport {

    private BossBarSupport() {
    }

    public static boolean isSupported(int version) {
        return version > 0;
    }

    public static IBossBar create(int version, @NotNull String title, @NotNull String color) {
        if (!isSupported(version)) return new EmptyBossBar();
        return new BukkitBossBar(title, color);
    }
}
