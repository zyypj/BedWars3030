package com.tomkeuper.bedwars.listeners;

import com.tomkeuper.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Warnings implements Listener {
    private final BedWars plugin;

    public Warnings(BedWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if(!player.isOp()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendMessage(ChatColor.RED + "[BedWars2023] Multiverse-Core detectado! Remova-o ou garanta que ele não mexa nos mapas do BedWars!"), 5); // run after 5 ticks to make sure its after any update spam on join
        }

        if(Bukkit.getServer().getSpawnRadius() > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendMessage(ChatColor.RED + "[BedWars2023] O spawn-protection do seu server.properties está ativado. "+ChatColor.YELLOW+"Isso pode atrapalhar as arenas do BedWars!"+ChatColor.GRAY+" É altamente recomendável defini-lo como 0."), 5);
        }
    }
}
