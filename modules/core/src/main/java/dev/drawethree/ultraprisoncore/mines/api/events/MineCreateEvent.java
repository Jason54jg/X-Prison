package dev.drawethree.ultraprisoncore.mines.api.events;

import dev.drawethree.ultraprisoncore.mines.model.mine.Mine;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MineCreateEvent extends Event implements Cancellable {

	private static final HandlerList HANDLERS_LIST = new HandlerList();

	@Getter
	@Setter
	private boolean cancelled;

	@Getter
	private CommandSender creator;

	@Getter
	private Mine mine;

	/**
	 * Fired when mine is created
	 *
	 * @param creator CommandSender who created the mine
	 * @param mine    Mine
	 */
	public MineCreateEvent(Player creator, Mine mine) {
		this.creator = creator;
		this.mine = mine;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS_LIST;
	}
}