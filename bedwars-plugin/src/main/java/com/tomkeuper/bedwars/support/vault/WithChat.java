package com.tomkeuper.bedwars.support.vault;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.chat.IChat;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WithChat implements IChat {

    private static net.milkbowl.vault.chat.Chat chat;

    @Override
    public String getPrefix(Player p) {
        return ChatColor.translateAlternateColorCodes('&', chat.getPlayerPrefix(p));
    }

    @Override
    public String getSuffix(Player p) {
        return ChatColor.translateAlternateColorCodes('&', chat.getPlayerSuffix(p));
    }

    @Override
    public void sendMessage(Player player, String msg) {
        BedWars.plugin.adventure().player(player).sendMessage(ChatFormatting.parseLegacyMini(msg));
    }

    @Override
    public Component parseMiniMessage(String msg) {
        return ChatFormatting.parseLegacyMini(msg);
    }

    @Override
    public Component parsePlaceholders(Player player, String msg) {
        String format = SupportPAPI.getSupportPAPI().replace(player, msg);
        return ChatFormatting.parseLegacyMini(format);
    }

    public static void setChat(net.milkbowl.vault.chat.Chat chat) {
        WithChat.chat = chat;
    }
}
