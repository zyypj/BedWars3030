package com.tomkeuper.bedwars.arena;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AssistTracker {

    private static final long ASSIST_WINDOW = 13_000L;
    private static final Map<UUID, Map<UUID, Long>> recentDamagers = new ConcurrentHashMap<>();

    private AssistTracker() {
    }

    public static void record(@NotNull Player victim, @NotNull Player damager) {
        if (victim.getUniqueId().equals(damager.getUniqueId())) return;
        recentDamagers.computeIfAbsent(victim.getUniqueId(), uuid -> new ConcurrentHashMap<>())
                .put(damager.getUniqueId(), System.currentTimeMillis());
    }

    @NotNull
    public static List<Player> getAssists(@NotNull Player victim, @Nullable Player killer) {
        Map<UUID, Long> damagers = recentDamagers.get(victim.getUniqueId());
        List<Player> assists = new ArrayList<>();
        if (damagers == null) return assists;

        long oldestAccepted = System.currentTimeMillis() - ASSIST_WINDOW;
        for (Map.Entry<UUID, Long> entry : damagers.entrySet()) {
            if (entry.getValue() < oldestAccepted) continue;
            if (killer != null && killer.getUniqueId().equals(entry.getKey())) continue;

            Player damager = Bukkit.getPlayer(entry.getKey());
            if (damager != null && damager.isOnline()) assists.add(damager);
        }
        return assists;
    }

    public static void clear(@NotNull Player victim) {
        recentDamagers.remove(victim.getUniqueId());
    }
}
