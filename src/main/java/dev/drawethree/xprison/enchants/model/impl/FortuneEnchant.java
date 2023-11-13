package dev.drawethree.xprison.enchants.model.impl;

import dev.drawethree.xprison.enchants.XPrisonEnchants;
import dev.drawethree.xprison.enchants.model.XPrisonEnchantment;
import dev.drawethree.xprison.utils.compat.CompMaterial;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class FortuneEnchant extends XPrisonEnchantment {

    private static List<CompMaterial> blackListedBlocks;
    private static List<CompMaterial> whiteListedBlocks;

    public FortuneEnchant(XPrisonEnchants instance) {
        super(instance, 3);
        blackListedBlocks = plugin.getEnchantsConfig().getYamlConfig().getStringList("enchants." + id + ".Blacklist").stream().map(CompMaterial::fromString).filter(Objects::nonNull).collect(Collectors.toList());
        whiteListedBlocks = plugin.getEnchantsConfig().getYamlConfig().getStringList("enchants." + id + ".Whitelist").stream().map(CompMaterial::fromString).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void onEquip(Player p, ItemStack pickAxe, int level) {
        ItemMeta meta = pickAxe.getItemMeta();
        meta.removeEnchant(Enchantment.LOOT_BONUS_BLOCKS);
        pickAxe.setItemMeta(meta);
    }

    @Override
    public void onUnequip(Player p, ItemStack pickAxe, int level) {

    }

    @Override
    public void onBlockBreak(BlockBreakEvent e, int enchantLevel) {
        if (enchantLevel > 0) {
            Block block = e.getBlock();
            if (isFortuneAffectedBlock(block)) {
                e.getBlock().getDrops().forEach(itemStack -> itemStack.setAmount(calculateExtraDrops(enchantLevel)));
            }
        }
    }

    private boolean isFortuneAffectedBlock(Block block) {
        // Implement logic to check if the block type is affected by Fortune
        // Return true if this block is affected by the Fortune enchantment
        // Example: return block.getType() == Material.DIAMOND_ORE;
        return whiteListedBlocks.contains(CompMaterial.fromBlock(block)); // Placeholder, customize based on affected blocks
    }
    private int calculateExtraDrops(int fortuneLevel) {
        // Implement the logic for extra drops based on Fortune level
        // Example: return ThreadLocalRandom.current().nextInt(1, 4); // Fortune III emulation
        return ThreadLocalRandom.current().nextInt((fortuneLevel - (fortuneLevel - 1)), (fortuneLevel + 1)); // Placeholder, adjust based on Fortune level

    }

    @Override
    public double getChanceToTrigger(int enchantLevel) {
        return 100.0;
    }

    @Override
    public void reload() {
        super.reload();
        blackListedBlocks = plugin.getEnchantsConfig().getYamlConfig().getStringList("enchants." + id + ".Blacklist").stream().map(CompMaterial::fromString).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public String getAuthor() {
        return "Drawethree";
    }

    public static boolean isBlockBlacklisted(Block block) {
        CompMaterial blockMaterial = CompMaterial.fromBlock(block);
        return blackListedBlocks.contains(blockMaterial);
    }
}
