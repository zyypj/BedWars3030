package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetLobby extends SubCommand {

    public SetLobby(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(1);
        showInList(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/"+ MainCommand.getInstance().getName()+" "+getSubCommandName()+ (BedWars.config.getLobbyWorldName().isEmpty() ? " §c(não definido)" : " §a(set)"),
                "§aDefine o lobby principal. §fIsso é obrigatório, mas\n§fse você for usar o servidor no modo §eBUNGEE§f,\n§fa localização do lobby §enão §fserá usada.\n§eDigite novamente para substituir o spawn antigo.",
                "/"+getParent().getName()+" "+getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (SetupSession.isInSetupSession(p.getUniqueId())){
            p.sendMessage("§6 ▪ §4Este comando não pode ser usado em arenas. Ele é para o lobby principal!");
            return true;
        }
        BedWars.config.saveConfigLoc("lobbyLoc", p.getLocation());
        p.sendMessage("§6 ▪ §7Localização do lobby definida!");
        BedWars.config.reload();
        BedWars.setLobbyWorld(p.getLocation().getWorld().getName());
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, com.tomkeuper.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;

        if (!BedWars.getLobbyWorld().isEmpty()) return false;

        return hasPermission(s);
    }
}
