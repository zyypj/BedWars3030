package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.arena.Arena;
import java.util.List;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DisableArena extends SubCommand {

    public DisableArena(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(6);
        showInList(true);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " "+getSubCommandName()+" §6<worldName>", "§fDesativa uma arena.\nIsso vai remover os jogadores \n§fda arena antes de desativar.",
                "/" + getParent().getName() + " "+getSubCommandName()+" ", ClickEvent.Action.SUGGEST_COMMAND));
        setPermission(Permissions.PERMISSION_ARENA_DISABLE);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!MainCommand.isLobbySet()) {
            s.sendMessage("§c▪ §7Você precisa definir a localização do lobby primeiro!");
            return true;
        }
        if (args.length != 1) {
            s.sendMessage("§c▪ §7Uso: §o/" + getParent().getName() + " "+getSubCommandName()+" <mapName>");
            return true;
        }
        if (!BedWars.getAPI().getRestoreAdapter().isWorld(args[0])) {
            s.sendMessage("§c▪ §7" + args[0] + " é um mundo e não uma arena!");
            return true;
        }
        // with auto scale an arena can be hosting several games at once, all of them have to go
        List<IArena> games = Arena.getArenasByName(args[0]);
        if (games.isEmpty()) {
            s.sendMessage("§c▪ §7Isso já foi desativado ou não existe!");
            return true;
        }
        for (IArena game : games) {
            if (game.getStatus() == GameState.playing) {
                s.sendMessage("§6 ▪ §7Há uma partida em andamento nesta arena, desative-a após a partida!");
                return true;
            }
        }
        s.sendMessage("§6 ▪ §7Desativando arena..." + (games.size() > 1 ? " §8(" + games.size() + " partidas)" : ""));
        for (IArena game : games) {
            game.disable();
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> tab = new ArrayList<>();
        for (IArena a : Arena.getArenas()){
            tab.add(a.getArenaName());
        }
        return tab;
    }

    @Override
    public boolean canSee(CommandSender s, com.tomkeuper.bedwars.api.BedWars api) {
        if (s instanceof Player) {
            Player p = (Player) s;
            if (Arena.isInArena(p)) return false;
            if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        }
        return hasPermission(s);
    }
}
