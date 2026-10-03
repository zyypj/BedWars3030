/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The signed texture behind a skin.
 * <p>
 * A packet NPC needs the raw texture value and its signature, which only Mojang can hand out. Looking it up
 * takes two web calls, so it is done once per skin and then kept in {@code skins.yml}: nobody should have to
 * paste a base64 blob to put an NPC in the lobby.
 */
public class SkinTexture {

    private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]+)\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private static final int TIMEOUT = 5000;

    private final String value;
    private final String signature;

    public SkinTexture(@NotNull String value, @Nullable String signature) {
        this.value = value;
        this.signature = signature;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @Nullable String getSignature() {
        return signature;
    }

    /**
     * Look a player's skin up at Mojang.
     * <p>
     * Blocking on purpose: every caller is already off the main thread, and making that obvious here is better
     * than hiding a network round trip behind a callback.
     *
     * @return the signed texture, or null when the name is unknown or Mojang is unreachable
     */
    public static @Nullable SkinTexture fetch(@NotNull String playerName) {
        String uuid = read("https://api.mojang.com/users/profiles/minecraft/" + playerName, UUID_PATTERN);
        if (uuid == null) return null;

        String profile = get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        if (profile == null) return null;

        Matcher value = VALUE_PATTERN.matcher(profile);
        if (!value.find()) return null;

        Matcher signature = SIGNATURE_PATTERN.matcher(profile);
        return new SkinTexture(value.group(1), signature.find() ? signature.group(1) : null);
    }

    private static @Nullable String read(@NotNull String url, @NotNull Pattern pattern) {
        String body = get(url);
        if (body == null) return null;

        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static @Nullable String get(@NotNull String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("User-Agent", "BedWars2023");

            // A name nobody owns answers 204, which is not an error worth logging.
            if (connection.getResponseCode() != 200) return null;

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return body.toString();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
