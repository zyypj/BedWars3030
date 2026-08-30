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

package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.configuration.ArenaConfig;
import com.tomkeuper.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaGroup extends SubCommand {

    public ArenaGroup(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(8);
        showInList(true);
        setPermission(Permissions.PERMISSION_ARENA_GROUP);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName()+" §8- §eclique para detalhes", "§fGerencia os grupos de arena.",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (!MainCommand.isLobbySet() && p != null) {
            p.sendMessage("§c▪ §7Você precisa definir a localização do lobby primeiro!");
            return true;
        }
        if (args.length < 2 && (args.length < 1 || !args[0].equalsIgnoreCase("list"))) {
            sendArenaGroupCmdList(p);
        } else if (args[0].equalsIgnoreCase("create")) {
            if (args[0].contains("+")) {
                p.sendMessage("§c▪ §7" + args[0] + " não pode conter este símbolo: " + ChatColor.RED + "+");
                return true;
            }
            java.util.List<String> groups;
            if (BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS) == null) {
                groups = new ArrayList<>();
            } else {
                groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
            }
            if (groups.contains(args[1])) {
                p.sendMessage("§c▪ §7Este grupo já existe!");
                return true;
            }
            groups.add(args[1]);
            BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
            p.sendMessage("§6 ▪ §7Grupo criado!");
        } else if (args[0].equalsIgnoreCase("remove")) {
            List<String> groups;
            if (BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS) == null) {
                groups = new ArrayList<>();
            } else {
                groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
            }
            if (!groups.contains(args[1])) {
                p.sendMessage("§c▪ §7Este grupo não existe!");
                return true;
            }
            groups.remove(args[1]);
            BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
            p.sendMessage("§6 ▪ §7Grupo removido!");
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> groups;
            if (BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS) == null) {
                groups = new ArrayList<>();
            } else {
                groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
            }
            p.sendMessage("§7Grupos de arena disponíveis:");
            p.sendMessage("§6 ▪ §fDefault");
            for (String gs : groups) {
                p.sendMessage("§6 ▪ §f" + gs);
            }
        } else if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sendArenaGroupCmdList(p);
                return true;
            }
            if (BedWars.config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS) != null) {
                if (BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS).contains(args[2])) {
                    File arena = new File(BedWars.plugin.getDataFolder(), "/Arenas/" + args[1] + ".yml");
                    if (!arena.exists()) {
                        p.sendMessage("§c▪ §7Arena " + args[1] + " não existe!");
                        return true;
                    }
                    ArenaConfig cm = new ArenaConfig(BedWars.plugin, args[1], BedWars.plugin.getDataFolder().getPath() + "/Arenas");
                    cm.set("group", args[2]);
                    if (Arena.getArenaByName(args[1]) != null) {
                        Arena.getArenaByName(args[1]).setGroup(args[2]);
                    }
                    p.sendMessage("§6 ▪ §7" + args[1] + " foi adicionada ao grupo: " + args[2]);
                } else {
                    p.sendMessage("§6 ▪ §7Não existe nenhum grupo chamado: " + args[2]);
                    Bukkit.dispatchCommand(p, "/bw list");
                }
            } else {
                p.sendMessage("§6 ▪ §7Não existe nenhum grupo chamado: " + args[2]);
                Bukkit.dispatchCommand(p, "/bw list");
            }
        } else {
            sendArenaGroupCmdList(p);
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("create", "remove", "list", "set");
    }

    private void sendArenaGroupCmdList(Player p) {
        p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " create §o<groupName>",
                "Cria um grupo de arena. Mais detalhes na nossa wiki.", "/" + getParent().getName() + " " + getSubCommandName() + " create",
                ClickEvent.Action.SUGGEST_COMMAND));
        p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " list",
                "Ver os grupos disponíveis.", "/" + getParent().getName() + " " + getSubCommandName() + " list",
                ClickEvent.Action.RUN_COMMAND));
        p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " remove §o<groupName>",
                "Remove um grupo de arena. Mais detalhes na nossa wiki.", "/" + getParent().getName() + " " + getSubCommandName() + " remove",
                ClickEvent.Action.SUGGEST_COMMAND));
        p.spigot().sendMessage(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName() + " §r§7set §o<nomeDaArena> <nomeDoGrupo>",
                "Define o grupo da arena. Mais detalhes na nossa wiki.", "/" + getParent().getName() + " " + getSubCommandName() + " set",
                ClickEvent.Action.SUGGEST_COMMAND));
    }

    @Override
    public boolean canSee(CommandSender s, com.tomkeuper.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }
}
