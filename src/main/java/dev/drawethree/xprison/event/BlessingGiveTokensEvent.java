package dev.drawethree.xprison.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class BlessingGiveTokensEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter @Setter
    private Collection<Player> recipients;

    @Getter @Setter
    private double chance;

    @Getter @Setter
    private long tokenAmount;

    @Setter @Getter
    private boolean cancelled = false;

    public BlessingGiveTokensEvent(@NotNull Player who, long tokenAmount, double chance, Collection<Player> recipients) {
        super(who);
        this.tokenAmount = tokenAmount;
        this.chance = chance;
        this.recipients = recipients;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}