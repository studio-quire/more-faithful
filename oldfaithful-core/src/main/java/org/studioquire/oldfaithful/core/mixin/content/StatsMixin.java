package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.stat.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * Mod content has no statistics, on purpose: the stats file holds numbers with a checksum, and a removed id gets the whole file rejected.
 * So the mined table only needs to be long enough to index (null entries are safe), and the block/item line stays at 256 however
 * long {@code Block.BLOCKS} is now.
 */
@Mixin(Stats.class)
abstract class StatsMixin {
	@ModifyConstant(method = "initBlocksMined", constant = @Constant(intValue = 256, ordinal = 0))
	private static int faithful$minedTable(int size) {
		return ContentRegistry.BLOCK_LIMIT;
	}

	@Redirect(method = {"initializeItemStats", "initializeExtendedItemStats", "initItemsUsedStats"}, at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS:[Lnet/minecraft/block/Block;", args = "array=length"))
	private static int faithful$blockCount(net.minecraft.block.Block[] blocks) {
		return 256;
	}
}
