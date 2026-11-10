package org.studioquire.faithful.waterbottles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public abstract class Bottle extends Item {
	protected Bottle(int id, int texture) {
		super(id);
		setTextureId(texture);
	}

	/**
	 * What {@code use} returns when one of the stack turns into {@code result}: the hand's new stack, with {@code result} in the hand if that was the last
	 * and otherwise in the inventory or at the player's feet. A server's packet handler only tells the client about the hand and swallows every other slot's
	 * change, so the server sends those here, first; a client leaves it all to that.
	 */
	protected static ItemStack exchange(ItemStack stack, World world, PlayerEntity user, ItemStack result) {
		if (stack.count <= 1) return result;
		if (world.isRemote) return stack;
		stack.count--;
		if (!user.inventory.addStack(result)) user.dropItem(result);
		user.currentScreenHandler.sendContentUpdates();
		return stack;
	}
}
