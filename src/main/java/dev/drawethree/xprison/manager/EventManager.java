package dev.drawethree.xprison.manager;

import dev.drawethree.xprison.event.BlessingGiveTokensEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Collection;

public class EventManager {

    public static BlessingGiveTokensEvent callBlessingGiveTokensEvent(Player player, long tokenAmount, double chance, Collection<Player> players) {
        var event = new BlessingGiveTokensEvent(player, tokenAmount, chance, players);
        callEvent(event);
        return event;
    }

    private static void callEvent(Event event){
        Bukkit.getServer().getPluginManager().callEvent(event);
    }
}