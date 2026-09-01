package com.tomkeuper.bedwars.listeners;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static com.tomkeuper.bedwars.api.language.Language.getMsg;

public class CmdProcess implements Listener {

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {

        Player p = e.getPlayer();

        if (e.getMessage().equals("/party sethome")){
            BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME)));
            e.setCancelled(true);
        }

        if (e.getMessage().equals("/party home")){
            BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME)));
            e.setCancelled(true);
        }

        if (p.hasPermission(Permissions.PERMISSION_COMMAND_BYPASS)) return;
        String[] cmd = e.getMessage().replaceFirst("/", "").split(" ");
        if (cmd.length == 0) return;
        if (Arena.isInArena(p)) {
            if (!BedWars.config.getList(ConfigPath.GENERAL_CONFIGURATION_ALLOWED_COMMANDS).contains(cmd[0])) {
                BedWars.plugin.adventure().player(p).sendMessage(ChatFormatting.parseLegacyMini(getMsg(p, Messages.COMMAND_NOT_ALLOWED_IN_GAME)));
                e.setCancelled(true);
            }
        }
    }
}
