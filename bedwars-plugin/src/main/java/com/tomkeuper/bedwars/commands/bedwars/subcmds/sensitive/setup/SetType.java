package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.server.SetupType;
import com.tomkeuper.bedwars.arena.ArenaMode;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetType extends SubCommand {

    public SetType(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    private static final List<String> available = ArenaMode.getGroups();

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            s.sendMessage("§c ▪ §7Você não está em uma sessão de setup!");
            return true;
        }
        if (args.length == 0) {
            sendUsage(p);
        } else {
            if (!available.contains(args[0])) {
                sendUsage(p);
                return true;
            }
            ArenaMode mode = ArenaMode.getByGroup(args[0]);
            if (mode == null) {
                sendUsage(p);
                return true;
            }
            List<String> groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
            String input = mode.getGroup();
            if (!groups.contains(input)) {
                groups.add(input);
                BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
            }
            ss.getConfig().set("maxInTeam", mode.getMaxInTeam());
            ss.getConfig().set("minPlayers", mode.getMinPlayers());
            ss.getConfig().set("group", input);
            p.sendMessage("§6 ▪ §7Grupo da arena alterado para: §d" + input);
            p.sendMessage("§6 ▪ §7Máximo por time: §f" + mode.getMaxInTeam() + " §7| Mínimo para começar: §f" + mode.getMinPlayers());
            if (ss.getSetupType() == SetupType.ASSISTED) {
                Bukkit.dispatchCommand(p, getParent().getName());
            }
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
        available.forEach(available -> {
            if (!groups.contains(available)) {
                groups.add(available);
            }
        });
        return BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
    }

    private void sendUsage(Player p) {
        p.sendMessage("§9 ▪ §7Uso: " + getParent().getName() + " " + getSubCommandName() + " <type>");
        p.sendMessage("§9Tipos disponíveis: ");
        for (String st : available) {
            p.spigot().sendMessage(Misc.msgHoverClick("§1 ▪ §e" + st + " §7(clique para definir)", "§dClique para tornar a arena " + st, "/" + getParent().getName() + " " + getSubCommandName() + " " + st, ClickEvent.Action.RUN_COMMAND));
        }
    }

    @Override
    public boolean canSee(CommandSender s, com.tomkeuper.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }
}
