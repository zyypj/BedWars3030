package com.tomkeuper.bedwars.api.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Get Chat Methods
 */
public interface IChat {

    /**
     * Get Player prefix
     * @param p player from which to take the prefix
     */
    String getPrefix(Player p);

    /**
     * Get Player suffix
     * @param p player from which to take the suffix
     */
    String getSuffix(Player p);

    /**
     * Sends a message to a player with adventure minimessage formating
     * @param player the player to receive the message
     * @param msg the message that will be sent to the player
     */
    void sendMessage(Player player, String msg);

    /**
     * Parse legacy and minimessage string to Component
     * @param msg the message to be parsed
     * @return the parsed Component
     */
    Component parseMiniMessage(String msg);

    /**
     * Parse placeholders, legacy and minimessage string to Component
     * @param player the player to get placeholders for
     * @param msg the message to be parsed
     * @return the parsed Component
     */
    Component parsePlaceholders(Player player, String msg);

}
