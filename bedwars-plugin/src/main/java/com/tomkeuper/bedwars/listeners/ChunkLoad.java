package com.tomkeuper.bedwars.listeners;

import com.tomkeuper.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoad implements Listener {

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent e){
        if (e == null) return;
        if (e.getChunk() == null) return;
        if (e.getChunk().getEntities() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, ()-> {
            for (Entity entity : e.getChunk().getEntities()){
                if (entity instanceof ArmorStand){
                    if (entity.hasMetadata("bw2023-setup")){
                        Bukkit.getScheduler().runTask(BedWars.plugin, entity::remove);
                        continue;
                    }
                    if (!((ArmorStand)entity).isVisible()){
                        if (((ArmorStand)entity).isMarker()){
                            if (entity.isCustomNameVisible()){
                                String holoName = ChatColor.stripColor(entity.getCustomName());
                                if (holoName.contains(" DEFINID") || holoName.contains(" definid")
                                        || holoName.contains(" SET") || holoName.contains(" set")){
                                    Bukkit.getScheduler().runTask(BedWars.plugin, entity::remove);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
