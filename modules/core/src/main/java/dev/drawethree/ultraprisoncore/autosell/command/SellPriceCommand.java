package dev.drawethree.ultraprisoncore.autosell.command;

import dev.drawethree.ultraprisoncore.autosell.UltraPrisonAutoSell;
import dev.drawethree.ultraprisoncore.autosell.gui.AllSellRegionsGui;
import dev.drawethree.ultraprisoncore.autosell.model.SellRegion;
import dev.drawethree.ultraprisoncore.autosell.utils.AutoSellContants;
import dev.drawethree.ultraprisoncore.utils.compat.CompMaterial;
import dev.drawethree.ultraprisoncore.utils.misc.RegionUtils;
import dev.drawethree.ultraprisoncore.utils.player.PlayerUtils;
import me.lucko.helper.Commands;
import me.lucko.helper.command.CommandInterruptException;
import me.lucko.helper.command.context.CommandContext;
import org.bukkit.entity.Player;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

public class SellPriceCommand {

    private static final String COMMAND_NAME = "sellprice";
    private final UltraPrisonAutoSell plugin;

    public SellPriceCommand(UltraPrisonAutoSell plugin) {

        this.plugin = plugin;
    }

    public void register() {
        Commands.create()
                .assertPlayer()
                .assertPermission(AutoSellContants.ADMIN_PERMISSION)
                .handler(c -> {

                    if (!this.validateContext(c)) {
                        this.sendInvalidUsage(c.sender());
                        return;
                    }

                    if (isEditorCommand(c)) {
                        this.openEditorGui(c.sender());
                        return;
                    }

                    CompMaterial type = this.parseMaterialFromCommandContext(c);
                    double price = this.parsePriceFromCommandContext(c);
                    SellRegion region = this.parseSellRegionFromCommandContext(c);

                    if (!validateMaterial(type)) {
                        PlayerUtils.sendMessage(c.sender(), "&cInvalid item in hand / specified item!");
                        return;
                    }

                    if (!validatePrice(price)) {
                        PlayerUtils.sendMessage(c.sender(), "&cSell price needs to be higher than 0!");
                        return;
                    }

                    IWrappedRegion wrappedRegion = RegionUtils.getFirstRegionAtLocation(c.sender().getLocation());

                    if (!validateRegion(wrappedRegion) && region == null) {
                        PlayerUtils.sendMessage(c.sender(), "&cYou must be standing in a region / specify a valid region!");
                        return;
                    }

                    SellRegion sellRegion = region == null ? this.getSellRegionFromWrappedRegion(wrappedRegion) : region;

                    if (sellRegion == null) {
                        sellRegion = new SellRegion(wrappedRegion, c.sender().getWorld());
                    }

                    sellRegion.addSellPrice(type, price);

                    this.plugin.getManager().updateSellRegion(sellRegion);
                    this.plugin.getAutoSellConfig().saveSellRegion(sellRegion);

                    PlayerUtils.sendMessage(c.sender(), String.format("&aSuccessfuly set sell price of &e%s &ato &e$%.2f &ain region &e%s", type.name(), price, wrappedRegion.getId()));

                }).registerAndBind(this.plugin.getCore(), COMMAND_NAME);
    }

    private SellRegion parseSellRegionFromCommandContext(CommandContext<Player> c) {
        if (c.args().size() == 2) {
            return getSellRegionByName(c.rawArg(1));
        } else if (c.args().size() == 3) {
            return getSellRegionByName(c.rawArg(2));
        }
        return null;
    }

    private void openEditorGui(Player sender) {
        AllSellRegionsGui.createAndOpenTo(sender);
    }

    private boolean isEditorCommand(CommandContext<Player> c) {
        return "editor".equalsIgnoreCase(c.rawArg(0));
    }

    private SellRegion getSellRegionByName(String name) {
        return this.plugin.getManager().getSellRegionByName(name);
    }

    private SellRegion getSellRegionFromWrappedRegion(IWrappedRegion region) {
        return this.plugin.getManager().getSellRegionFromWrappedRegion(region);
    }

    private boolean validateRegion(IWrappedRegion region) {
        return region != null;
    }

    private boolean validatePrice(double price) {
        return price > 0.0;
    }

    private boolean validateMaterial(CompMaterial type) {
        return type != null;
    }

    private void sendInvalidUsage(Player player) {
        PlayerUtils.sendMessage(player, "&cInvalid usage!");
        PlayerUtils.sendMessage(player, "&c/sellprice editor - Opens Editor GUI for sell prices");
        PlayerUtils.sendMessage(player, "&c/sellprice <material> <price> [region] - Sets the sell price of specified material. Region is optional");
        PlayerUtils.sendMessage(player, "&c/sellprice <price> [region] - Sets the sell price of item material you have in your hand. Region is optional");
    }

    private boolean validateContext(CommandContext<Player> context) {
        return context.args().size() == 1 || context.args().size() == 2 || context.args().size() == 3;
    }

    private CompMaterial parseMaterialFromCommandContext(CommandContext<Player> c) {
        CompMaterial material = null;
        if (c.args().size() == 1) {
            if (c.sender().getItemInHand() == null) {
                PlayerUtils.sendMessage(c.sender(), "&cPlease hold some item!");
            } else {
                material = CompMaterial.fromItem(c.sender().getItemInHand());
            }
        } else if (c.args().size() == 2) {
            material = CompMaterial.fromString(c.rawArg(0));
        }
        return material;
    }

    private double parsePriceFromCommandContext(CommandContext<Player> c) throws CommandInterruptException {
        double price = 0.0;
        if (c.args().size() == 1) {
            price = c.arg(0).parseOrFail(Double.class);
        } else if (c.args().size() == 2) {
            price = c.arg(1).parseOrFail(Double.class);
        }
        return price;
    }
}
