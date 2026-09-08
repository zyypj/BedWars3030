package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlaceholderRegistry {

    private final Map<String, Function<Player, String>> playerPlaceholders = new ConcurrentHashMap<>();
    private final Map<String, Supplier<String>> serverPlaceholders = new ConcurrentHashMap<>();

    public void registerPlayerPlaceholder(@NotNull String identifier, @NotNull Function<Player, String> resolver) {
        playerPlaceholders.put(identifier, resolver);
    }

    public void registerServerPlaceholder(@NotNull String identifier, @NotNull Supplier<String> resolver) {
        serverPlaceholders.put(identifier, resolver);
    }

    public void unregisterPlaceholder(@NotNull String identifier) {
        playerPlaceholders.remove(identifier);
        serverPlaceholders.remove(identifier);
    }

    public boolean isRegistered(@NotNull String identifier) {
        return playerPlaceholders.containsKey(identifier) || serverPlaceholders.containsKey(identifier);
    }

    public void clear() {
        playerPlaceholders.clear();
        serverPlaceholders.clear();
    }

    @NotNull
    public String parse(@NotNull Player player, @Nullable String text) {
        if (text == null || text.isEmpty()) return "";
        String parsed = replaceOwnPlaceholders(player, text);
        if (parsed.indexOf('%') < 0) return parsed;
        return SupportPAPI.getSupportPAPI().replace(player, parsed);
    }

    @NotNull
    public List<String> parse(@NotNull Player player, @NotNull List<String> lines) {
        List<String> parsed = new ArrayList<>(lines.size());
        for (String line : lines) {
            parsed.add(parse(player, line));
        }
        return parsed;
    }

    @NotNull
    private String replaceOwnPlaceholders(@NotNull Player player, @NotNull String text) {
        if (text.indexOf('%') < 0) return text;

        StringBuilder result = new StringBuilder(text.length());
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current != '%') {
                result.append(current);
                index++;
                continue;
            }

            int closing = text.indexOf('%', index + 1);
            if (closing < 0) {
                result.append(text, index, text.length());
                break;
            }

            String identifier = text.substring(index, closing + 1);
            String value = resolve(player, identifier);
            if (value == null) {
                result.append(current);
                index++;
            } else {
                result.append(value);
                index = closing + 1;
            }
        }
        return result.toString();
    }

    @Nullable
    private String resolve(@NotNull Player player, @NotNull String identifier) {
        Function<Player, String> playerResolver = playerPlaceholders.get(identifier);
        if (playerResolver != null) {
            String value = playerResolver.apply(player);
            return value == null ? "" : value;
        }
        Supplier<String> serverResolver = serverPlaceholders.get(identifier);
        if (serverResolver != null) {
            String value = serverResolver.get();
            return value == null ? "" : value;
        }
        return null;
    }
}
