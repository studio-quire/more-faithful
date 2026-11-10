package org.studioquire.faithful.waterbottles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class WaterBottle extends Bottle {
	WaterBottle(int id, int texture) {
		super(id, texture);
		maxCount = 1;
	}

	/** At once and for nothing, as stew is but for the healing. */
	@Override
	public ItemStack use(ItemStack stack, World world, PlayerEntity user) {
		return exchange(stack, world, user, new ItemStack(WaterBottles.glassBottle.get()));
	}
}
