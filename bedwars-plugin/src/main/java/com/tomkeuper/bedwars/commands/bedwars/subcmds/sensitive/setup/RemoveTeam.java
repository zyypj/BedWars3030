package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.configuration.Sounds;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class RemoveTeam extends SubCommand {

    public RemoveTeam(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            //s.sendMessage("§c ▪ §7Você não está em uma sessão de setup!");
            return false;
        }
        if (args.length < 1) {
            p.sendMessage(ss.getPrefix() + ChatColor.RED + "Uso: /" + com.tomkeuper.bedwars.BedWars.mainCmd + " removeTeam <teamName>");
            if (ss.getConfig().getYml().get("Team") != null) {
                p.sendMessage(ss.getPrefix() + "Times disponíveis: ");
                for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(false)) {
                    p.spigot().sendMessage(Misc.msgHoverClick(ChatColor.GOLD + " " + '▪' + " " + TeamColor.getChatColor(team) + team, ChatColor.GRAY + "Remove " + TeamColor.getChatColor(team) + team + " " + ChatColor.GRAY + "(clique para remover)", "/" + com.tomkeuper.bedwars.BedWars.mainCmd + " removeTeam " + team, ClickEvent.Action.RUN_COMMAND));
                }
            }
        } else {
            if (ss.getConfig().getYml().get("Team." + args[0] + ".Color") == null) {
                p.sendMessage(ss.getPrefix() + "Este time não existe: " + args[0]);
                com.tomkeuper.bedwars.BedWars.nms.sendTitle(p, " ", ChatColor.RED + "Time não encontrado: " + args[0], 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_INSUFF_MONEY, p);
            } else {
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Iron") != null) {
                    for (Location loc : ss.getConfig().getArenaLocations("Team." + args[0] + ".Iron")) {
                        ss.removeGeneratorHologramLineContainingType(loc, "Iron");
                    }
                }
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Gold") != null) {
                    for (Location loc : ss.getConfig().getArenaLocations("Team." + args[0] + ".Gold")) {
                        ss.removeGeneratorHologramLineContainingType(loc, "Gold");
                    }
                }
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Emerald") != null) {
                    for (Location loc : ss.getConfig().getArenaLocations("Team." + args[0] + ".Emerald")) {
                        ss.removeGeneratorHologramLineContainingType(loc, "Emerald");
                    }
                }
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Shop") != null) {
                    ss.removeShopHologram(args[0]);
                }
                if (ss.getConfig().getYml().get("Team." + args[0] + ".Upgrade") != null) {
                    ss.removeUpgradeHologram(args[0]);
                }
                if (ss.getConfig().getYml().get("Team." + args[0] + "." + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC) != null) {
                    ss.removeKillDropsHologram(args[0]);
                }
                p.sendMessage(ss.getPrefix() + "Time removido: " + ss.getTeamColor(args[0]) + args[0]);
                com.tomkeuper.bedwars.BedWars.nms.sendTitle(p, " ", ChatColor.GREEN + "Time removido: " + ss.getTeamColor(args[0]) + args[0], 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_BOUGHT, p);
                ss.getConfig().set("Team." + args[0], null);
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
