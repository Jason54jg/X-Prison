package dev.drawethree.ultraprisoncore.enchants.managers;

import de.tr7zw.changeme.nbtapi.NBTItem;
import dev.drawethree.ultraprisoncore.api.enums.LostCause;
import dev.drawethree.ultraprisoncore.api.enums.ReceiveCause;
import dev.drawethree.ultraprisoncore.enchants.UltraPrisonEnchants;
import dev.drawethree.ultraprisoncore.enchants.api.events.UltraPrisonPlayerEnchantEvent;
import dev.drawethree.ultraprisoncore.enchants.enchants.UltraPrisonEnchantment;
import dev.drawethree.ultraprisoncore.enchants.gui.DisenchantGUI;
import dev.drawethree.ultraprisoncore.enchants.gui.EnchantGUI;
import dev.drawethree.ultraprisoncore.pickaxelevels.UltraPrisonPickaxeLevels;
import dev.drawethree.ultraprisoncore.pickaxelevels.model.PickaxeLevel;
import dev.drawethree.ultraprisoncore.utils.compat.CompMaterial;
import dev.drawethree.ultraprisoncore.utils.item.ItemStackBuilder;
import dev.drawethree.ultraprisoncore.utils.player.PlayerUtils;
import dev.drawethree.ultraprisoncore.utils.text.TextUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import me.lucko.helper.time.Time;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.codemc.worldguardwrapper.flag.WrappedState;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnchantsManager {

	private static final String NBT_TAG_IDENTIFIER = "ultra-prison-ench-";
	private static final Pattern PICKAXE_LORE_ENCHANT_PATTER = Pattern.compile("%Enchant-\\d+%");

	private final UltraPrisonEnchants plugin;
	private final List<UUID> lockedPlayers;

	public EnchantsManager(UltraPrisonEnchants plugin) {
		this.plugin = plugin;
		this.lockedPlayers = new ArrayList<>();
	}

	public Map<UltraPrisonEnchantment, Integer> getItemEnchants(ItemStack itemStack) {

		Map<UltraPrisonEnchantment,Integer> returnMap = new HashMap<>();

		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return returnMap;
		}

		NBTItem nbtItem = new NBTItem(itemStack);

		for (UltraPrisonEnchantment enchantment : UltraPrisonEnchantment.all()) {
			if (!nbtItem.hasKey(NBT_TAG_IDENTIFIER + enchantment.getId())) {
				returnMap.put(enchantment, 0);
			} else {
				int level = nbtItem.getInteger(NBT_TAG_IDENTIFIER + enchantment.getId());
				returnMap.put(enchantment, Math.min(level, enchantment.getMaxLevel()));
			}
		}

		return returnMap;
	}

	public ItemStack updatePickaxe(Player player, ItemStack item) {

		if (item == null || !this.plugin.getCore().isPickaxeSupported(item.getType())) {
			return item;
		}

		return this.applyLoreToPickaxe(player, item);
	}

	private ItemStack applyLoreToPickaxe(Player player, ItemStack item) {

		ItemMeta meta = item.getItemMeta();
		List<String> lore = new ArrayList<>();

		boolean pickaxeLevels = this.plugin.getCore().isModuleEnabled(UltraPrisonPickaxeLevels.MODULE_NAME);

		PickaxeLevel currentLevel = null;
		PickaxeLevel nextLevel = null;
		String pickaxeProgressBar = "";

		if (pickaxeLevels) {
			currentLevel = this.plugin.getCore().getPickaxeLevels().getPickaxeLevel(item);
			nextLevel = this.plugin.getCore().getPickaxeLevels().getNextPickaxeLevel(currentLevel);
			pickaxeProgressBar = this.plugin.getCore().getPickaxeLevels().getProgressBar(item);
		}

		long blocksBroken = getBlocksBroken(item);
		Map<UltraPrisonEnchantment, Integer> enchants = this.getItemEnchants(item);

		List<String> pickaxeLore = this.plugin.getEnchantsConfig().getPickaxeLore();

		for (String s : pickaxeLore) {
			s = s.replace("%Blocks%", String.valueOf(blocksBroken));

			if (pickaxeLevels) {
				s = s.replace("%Blocks_Required%", nextLevel == null ? "∞" : String.valueOf(nextLevel.getBlocksRequired()));
				s = s.replace("%PickaxeLevel%", currentLevel == null ? "0" : String.valueOf(currentLevel.getLevel()));
				s = s.replace("%PickaxeProgress%", pickaxeProgressBar);
			}

			Matcher matcher = PICKAXE_LORE_ENCHANT_PATTER.matcher(s);

			if (matcher.find()) {
				int enchId = Integer.parseInt(matcher.group().replaceAll("\\D", ""));
				UltraPrisonEnchantment enchantment = UltraPrisonEnchantment.getEnchantById(enchId);

				if (enchantment != null) {
					int enchLvl = enchants.get(enchantment);
					if (enchLvl > 0) {
						s = s.replace(matcher.group(), enchantment.getName() + " " + enchLvl);
					} else {
						continue;
					}
				} else {
					continue;
				}
			}

			if (this.plugin.getCore().isPlaceholderAPIEnabled()) {
				s = PlaceholderAPI.setPlaceholders(player, s);
			}

			lore.add(TextUtils.applyColor(s));
		}

		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
		return item;
	}

	public long getBlocksBroken(ItemStack item) {

		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}

		NBTItem nbtItem = new NBTItem(item);

		if (!nbtItem.hasKey("blocks-broken")) {
			return 0;
		}

		return nbtItem.getLong("blocks-broken");
	}

	public synchronized void addBlocksBrokenToItem(Player p, int amount) {

		if (amount == 0) {
			return;
		}

		NBTItem nbtItem = new NBTItem(p.getItemInHand());

		if (!nbtItem.hasKey("blocks-broken")) {
			nbtItem.setLong("blocks-broken", 0L);
		}

		nbtItem.setLong("blocks-broken", nbtItem.getLong("blocks-broken") + amount);

		ItemStack item = nbtItem.getItem();
		applyLoreToPickaxe(p, item);
		p.setItemInHand(item);
	}

	public synchronized void addBlocksBrokenToItem(Player player, ItemStack item, int amount) {

		if (amount == 0) {
			return;
		}

		NBTItem nbtItem = new NBTItem(item);

		if (!nbtItem.hasKey("blocks-broken")) {
			nbtItem.setLong("blocks-broken", 0L);
		}

		nbtItem.setLong("blocks-broken", nbtItem.getLong("blocks-broken") + amount);
		player.setItemInHand(nbtItem.getItem());
		applyLoreToPickaxe(player, player.getItemInHand());
	}

	public synchronized int getEnchantLevel(ItemStack itemStack, UltraPrisonEnchantment enchantment) {

		if (enchantment == null || itemStack == null || itemStack.getType() == Material.AIR) {
			return 0;
		}

		NBTItem nbtItem = new NBTItem(itemStack);

		if (!nbtItem.hasKey(NBT_TAG_IDENTIFIER + enchantment.getId())) {
			return 0;
		}

		int level = nbtItem.getInteger(NBT_TAG_IDENTIFIER + enchantment.getId());

		return Math.min(level, enchantment.getMaxLevel());
	}

	public void handleBlockBreak(BlockBreakEvent e, ItemStack pickAxe) {

		Optional<WrappedState> optional = this.plugin.getCore().getWorldGuardWrapper().queryFlag(e.getPlayer(), e.getBlock().getLocation(), this.plugin.getEnchantsWGFlag());

		if (!optional.isPresent() || optional.get() == WrappedState.DENY) {
			this.plugin.getCore().debug("EnchantsManager::handleBlockBreak >> No region with flag upc-enchants found. Enchants will not be triggered.", this.plugin);
			return;
		}

		this.addBlocksBrokenToItem(e.getPlayer(), 1);

		Map<UltraPrisonEnchantment, Integer> playerEnchants = this.getItemEnchants(pickAxe);

		for (UltraPrisonEnchantment enchantment : playerEnchants.keySet()) {
			int level = playerEnchants.get(enchantment);

			if (!enchantment.isEnabled() || level == 0) {
				continue;
			}

			enchantment.onBlockBreak(e, playerEnchants.get(enchantment));
		}

	}

	public void handlePickaxeEquip(Player p, ItemStack newItem) {
		Map<UltraPrisonEnchantment, Integer> playerEnchants = this.getItemEnchants(newItem);
		for (UltraPrisonEnchantment enchantment : playerEnchants.keySet()) {
			if (!enchantment.isEnabled()) {
				continue;
			}
			enchantment.onEquip(p, newItem, playerEnchants.get(enchantment));
		}
	}

	public void handlePickaxeUnequip(Player p, ItemStack newItem) {
		Map<UltraPrisonEnchantment, Integer> playerEnchants = this.getItemEnchants(newItem);
		for (UltraPrisonEnchantment enchantment : playerEnchants.keySet()) {
			if (!enchantment.isEnabled()) {
				continue;
			}
			enchantment.onUnequip(p, newItem, playerEnchants.get(enchantment));
		}
	}

	public ItemStack setEnchantLevel(Player player, ItemStack item, UltraPrisonEnchantment enchantment, int level) {

		if (enchantment == null || item == null) {
			return item;
		}

		NBTItem nbtItem = new NBTItem(item, true);

		if (level > 0) {
			nbtItem.setInteger(EnchantsManager.NBT_TAG_IDENTIFIER + enchantment.getId(), level);
		} else {
			nbtItem.removeKey(EnchantsManager.NBT_TAG_IDENTIFIER + enchantment.getId());
		}

		nbtItem.mergeCustomNBT(item);
		return this.applyLoreToPickaxe(player, item);
	}

	public ItemStack removeEnchant(Player player, ItemStack item, UltraPrisonEnchantment enchantment) {
		return setEnchantLevel(player, item, enchantment, 0);
	}

	public void buyEnchnant(UltraPrisonEnchantment enchantment, EnchantGUI gui, int currentLevel, int addition) {

		if (currentLevel >= enchantment.getMaxLevel()) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_max_level"));
			return;
		}

		if (currentLevel + addition > enchantment.getMaxLevel()) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_max_level_exceed"));
			return;
		}

		long totalCost = 0;

		long startTime = Time.nowMillis();

		for (int j = 0; j < addition; j++) {
			totalCost += enchantment.getCostOfLevel(currentLevel + j + 1);
		}

		this.plugin.getCore().debug(String.format("Calculation of levels %,d - %,d of %s enchant took %dms", currentLevel + 1, currentLevel + addition + 1, enchantment.getRawName(), Time.nowMillis() - startTime), this.plugin);

		if (!plugin.getCore().getTokens().getApi().hasEnough(gui.getPlayer(), totalCost)) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("not_enough_tokens"));
			return;
		}

		UltraPrisonPlayerEnchantEvent event = new UltraPrisonPlayerEnchantEvent(gui.getPlayer(), totalCost, currentLevel + addition);

		Events.callSync(event);

		if (event.isCancelled()) {
			return;
		}

		plugin.getCore().getTokens().getApi().removeTokens(gui.getPlayer(), totalCost, LostCause.ENCHANT);

		this.setEnchantLevel(gui.getPlayer(), gui.getPickAxe(), enchantment, currentLevel + addition);

		enchantment.onUnequip(gui.getPlayer(), gui.getPickAxe(), currentLevel);
		enchantment.onEquip(gui.getPlayer(), gui.getPickAxe(), currentLevel + addition);

		gui.getPlayer().getInventory().setItem(gui.getPickaxePlayerInventorySlot(), gui.getPickAxe());

		if (addition == 1) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_bought").replace("%tokens%", String.format("%,d", totalCost)));
		} else {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_bought_multiple")
					.replace("%amount%", String.valueOf(addition))
					.replace("%enchant%", enchantment.getName())
					.replace("%tokens%", String.format("%,d", totalCost)));
		}
	}

	public void disenchant(UltraPrisonEnchantment enchantment, DisenchantGUI gui, int currentLevel, int substraction) {

		if (currentLevel <= 0) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_no_level"));
			return;
		}

		if (currentLevel - substraction < 0) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_min_level_exceed"));
			return;
		}

		long totalRefunded = 0;

		for (int j = 0; j < substraction; j++) {
			totalRefunded += enchantment.getRefundForLevel(currentLevel - j);
		}

		plugin.getCore().getTokens().getTokensManager().giveTokens(gui.getPlayer(), totalRefunded, null, ReceiveCause.REFUND);

		this.setEnchantLevel(gui.getPlayer(), gui.getPickAxe(), enchantment, currentLevel - substraction);

		enchantment.onUnequip(gui.getPlayer(), gui.getPickAxe(), currentLevel);
		enchantment.onEquip(gui.getPlayer(), gui.getPickAxe(), currentLevel - substraction);

		gui.getPlayer().getInventory().setItem(gui.getPickaxePlayerInventorySlot(), gui.getPickAxe());

		PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_refunded").replace("%amount%", String.format("%,d", substraction)).replace("%enchant%", enchantment.getName()));
		PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_tokens_back").replace("%tokens%", String.format("%,d", totalRefunded)));
	}

	public void disenchantMax(UltraPrisonEnchantment enchantment, DisenchantGUI gui, int currentLevel) {

		if (currentLevel <= 0) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_no_level"));
			return;
		}

		if (this.lockedPlayers.contains(gui.getPlayer().getUniqueId())) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("transaction_in_progress"));
			return;
		}

		this.lockedPlayers.add(gui.getPlayer().getUniqueId());


		Schedulers.async().run(() -> {
			int current = currentLevel;
			int levelsToRefund = current;

			long totalRefunded = 0;

			while (gui.getPlayer().isOnline() && current > 0) {
				totalRefunded += enchantment.getRefundForLevel(current);
				current--;
			}

			if (!gui.getPlayer().isOnline()) {
				this.lockedPlayers.remove(gui.getPlayer().getUniqueId());
				return;
			}

			int finalCurrent = current;

			this.lockedPlayers.remove(gui.getPlayer().getUniqueId());

			Schedulers.sync().run(() -> {
				enchantment.onUnequip(gui.getPlayer(), gui.getPickAxe(), currentLevel);
				this.setEnchantLevel(gui.getPlayer(), gui.getPickAxe(), enchantment, finalCurrent);
				gui.getPlayer().getInventory().setItem(gui.getPickaxePlayerInventorySlot(), gui.getPickAxe());
				enchantment.onEquip(gui.getPlayer(), gui.getPickAxe(), finalCurrent);
				gui.redraw();
			});

			plugin.getCore().getTokens().getTokensManager().giveTokens(gui.getPlayer(), totalRefunded, null, ReceiveCause.REFUND);

			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_refunded").replace("%amount%", String.format("%,d", levelsToRefund)).replace("%enchant%", enchantment.getName()));
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_tokens_back").replace("%tokens%", String.format("%,d", totalRefunded)));
		});
	}

	public void buyMaxEnchant(UltraPrisonEnchantment enchantment, EnchantGUI gui, int currentLevel) {

		if (currentLevel >= enchantment.getMaxLevel()) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_max_level"));
			return;
		}

		if (this.lockedPlayers.contains(gui.getPlayer().getUniqueId())) {
			PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("transaction_in_progress"));
			return;
		}

		this.lockedPlayers.add(gui.getPlayer().getUniqueId());

		Schedulers.async().run(() -> {
			int levelsToBuy = 0;
			long totalCost = 0;

			while (gui.getPlayer().isOnline() && (currentLevel + levelsToBuy + 1) <= enchantment.getMaxLevel() && this.plugin.getCore().getTokens().getApi().hasEnough(gui.getPlayer(), totalCost + enchantment.getCostOfLevel(currentLevel + levelsToBuy + 1))) {
				levelsToBuy += 1;
				totalCost += enchantment.getCostOfLevel(currentLevel + levelsToBuy + 1);
			}

			if (!gui.getPlayer().isOnline()) {
				this.lockedPlayers.remove(gui.getPlayer().getUniqueId());
				return;
			}

			if (levelsToBuy == 0) {
				this.lockedPlayers.remove(gui.getPlayer().getUniqueId());
				PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("not_enough_tokens"));
				return;
			}

			UltraPrisonPlayerEnchantEvent event = new UltraPrisonPlayerEnchantEvent(gui.getPlayer(), totalCost, currentLevel + levelsToBuy);

			Events.callSync(event);

			if (event.isCancelled()) {
				this.lockedPlayers.remove(gui.getPlayer().getUniqueId());
				return;
			}

			plugin.getCore().getTokens().getApi().removeTokens(gui.getPlayer(), totalCost, LostCause.ENCHANT);

			int finalLevelsToBuy = levelsToBuy;

			this.lockedPlayers.remove(gui.getPlayer().getUniqueId());
			Schedulers.sync().run(() -> {
				enchantment.onUnequip(gui.getPlayer(), gui.getPickAxe(), currentLevel);
				this.setEnchantLevel(gui.getPlayer(), gui.getPickAxe(), enchantment, currentLevel + finalLevelsToBuy);
				enchantment.onEquip(gui.getPlayer(), gui.getPickAxe(), currentLevel + finalLevelsToBuy);
				gui.getPlayer().getInventory().setItem(gui.getPickaxePlayerInventorySlot(), gui.getPickAxe());
				gui.redraw();
			});

			if (levelsToBuy == 1) {
				PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_bought").replace("%tokens%", String.format("%,d", totalCost)));
			} else {
				PlayerUtils.sendMessage(gui.getPlayer(), plugin.getEnchantsConfig().getMessage("enchant_bought_multiple")
						.replace("%amount%", String.valueOf(levelsToBuy))
						.replace("%enchant%", enchantment.getName())
						.replace("%tokens%", String.format("%,d", totalCost)));
			}
		});
	}

	public long getPickaxeValue(ItemStack pickAxe) {

		long sum = 0;

		Map<UltraPrisonEnchantment, Integer> playerEnchants = this.getItemEnchants(pickAxe);

		for (UltraPrisonEnchantment enchantment : playerEnchants.keySet()) {
			for (int i = 1; i <= playerEnchants.get(enchantment); i++) {
				sum += enchantment.getCostOfLevel(i);
			}
		}
		return sum;
	}

	// /givepickaxe <player> <enchant:18=1;...> <name>
	public void givePickaxe(Player target, Map<UltraPrisonEnchantment, Integer> enchants, String pickaxeName, CommandSender sender) {
		ItemStackBuilder pickaxeBuilder = ItemStackBuilder.of(Material.DIAMOND_PICKAXE);

		if (pickaxeName != null) {
			pickaxeBuilder.name(pickaxeName);
		}

		ItemStack pickaxe = pickaxeBuilder.build();

		for (Map.Entry<UltraPrisonEnchantment, Integer> entry : enchants.entrySet()) {
			this.setEnchantLevel(target, pickaxe, entry.getKey(), entry.getValue());
		}

		pickaxe = this.applyLoreToPickaxe(target, pickaxe);

		if (target == null && sender instanceof Player) {
			target = (Player) sender;
		}

		if (target != null) {
			if (target.getInventory().firstEmpty() == -1) {
				PlayerUtils.sendMessage(sender, this.plugin.getEnchantsConfig().getMessage("pickaxe_inventory_full").replace("%player%", target.getName()));
				return;
			}

			target.getInventory().addItem(pickaxe);
			PlayerUtils.sendMessage(sender, this.plugin.getEnchantsConfig().getMessage("pickaxe_given").replace("%player%", target.getName()));
			PlayerUtils.sendMessage(target, this.plugin.getEnchantsConfig().getMessage("pickaxe_received").replace("%sender%", sender.getName()));
		}
	}

	public ItemStack createFirstJoinPickaxe(Player player) {

		String pickaxeName = this.plugin.getEnchantsConfig().getFirstJoinPickaxeName();
		pickaxeName = pickaxeName.replace("%player%", player.getName());

		if (this.plugin.getCore().isPlaceholderAPIEnabled()) {
			pickaxeName = PlaceholderAPI.setPlaceholders(player, pickaxeName);
		}

		CompMaterial material = this.plugin.getEnchantsConfig().getFirstJoinPickaxeMaterial();
		ItemStack item = ItemStackBuilder.of(material.toItem()).name(pickaxeName).build();

		List<String> firstJoinPickaxeEnchants = this.plugin.getEnchantsConfig().getFirstJoinPickaxeEnchants();

		for (String s : firstJoinPickaxeEnchants) {
			try {
				String[] data = s.split(" ");
				UltraPrisonEnchantment enchantment = UltraPrisonEnchantment.getEnchantByName(data[0]);
				int level = Integer.parseInt(data[1]);
				this.setEnchantLevel(player, item, enchantment, level);
			} catch (Exception e) {

			}
		}

		return this.applyLoreToPickaxe(player, item);
	}

	public boolean hasEnchants(ItemStack item) {
		//TODO: improve handling
		return item != null && this.getItemEnchants(item).values().stream().mapToInt(Integer::intValue).sum() > 0;
	}

	public void enable() {

	}

	public void disable() {

	}

	public void giveFirstJoinPickaxe(Player target) {
		target.getInventory().addItem(this.createFirstJoinPickaxe(target));
	}
}
