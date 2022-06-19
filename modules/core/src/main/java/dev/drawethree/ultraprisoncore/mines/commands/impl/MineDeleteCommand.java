package dev.drawethree.ultraprisoncore.mines.commands.impl;

import dev.drawethree.ultraprisoncore.mines.UltraPrisonMines;
import dev.drawethree.ultraprisoncore.mines.commands.MineCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MineDeleteCommand extends MineCommand {

	public MineDeleteCommand(UltraPrisonMines plugin) {
		super(plugin, "delete", "remove");
	}

	@Override
	public boolean execute(CommandSender sender, List<String> args) {

		if (args.size() != 1) {
			return false;
		}

		this.plugin.getManager().deleteMine(sender, args.get(0));
		return true;
	}

	@Override
	public String getUsage() {
		return "&cUsage: /mines delete <name> - Delete a mine";
	}

	@Override
	public boolean canExecute(CommandSender sender) {
		return sender.hasPermission(UltraPrisonMines.MINES_ADMIN_PERM);
	}
}
