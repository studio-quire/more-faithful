package org.studioquire.faithful.snipping.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.studioquire.faithful.snipping.Snips;

/** Beta's shears have no use on a block; on a plant that grows they snip it. Any mod's shears are {@code ShearsItem}s too. */
@Mixin(ShearsItem.class)
abstract class ShearsItemMixin extends Item {
	private ShearsItemMixin(int id) {
		super(id);
	}

	@Override
	public boolean useOnBlock(ItemStack stack, PlayerEntity user, World world, int x, int y, int z, int side) {
		return Snips.useShears(stack, user, world, x, y, z) || super.useOnBlock(stack, user, world, x, y, z, side);
	}
}
