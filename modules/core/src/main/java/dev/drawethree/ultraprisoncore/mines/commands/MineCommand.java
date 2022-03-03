package dev.drawethree.ultraprisoncore.mines.commands;

import com.google.common.collect.ImmutableList;
import dev.drawethree.ultraprisoncore.mines.UltraPrisonMines;
import lombok.Getter;
import org.bukkit.command.CommandSender;

public abstract class MineCommand {

	protected UltraPrisonMines plugin;
	@Getter
	private String name;
	@Getter
	private String[] aliases;

	public MineCommand(UltraPrisonMines plugin, String name, String... aliases) {
		this.plugin = plugin;
		this.name = name;
		this.aliases = aliases;
	}

	public abstract boolean execute(CommandSender sender, ImmutableList<String> args);

	public abstract String getUsage();

	public abstract boolean canExecute(CommandSender sender);
}