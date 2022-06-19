package dev.drawethree.ultraprisoncore.mines.migration.gui;

import dev.drawethree.ultraprisoncore.mines.migration.model.MinesMigration;
import dev.drawethree.ultraprisoncore.utils.gui.ConfirmationGui;
import org.bukkit.entity.Player;

public final class MinesMigrationGui extends ConfirmationGui {

	private final MinesMigration migration;

	public MinesMigrationGui(Player player, MinesMigration migration) {
		super(player, "Migrate from " + migration.getFromPlugin() + "?");
		this.migration = migration;
	}

	@Override
	public void confirm(boolean confirm) {
		this.close();
		if (confirm) {
			migration.migrate(getPlayer());
		}
	}
}
