package org.studioquire.faithful.featherlure;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.faithful.featherlure.mixin.FishingBobberAccess;

/**
 * Beta's fishing rod with a feather on its line. Casting, reeling and wear are beta's own ({@code super.use}: 1 for a fish, 3 for something hooked, 2 off the ground,
 * nothing for an empty line, 64 at most); its bobber bites twice as often. A fish caught takes the feather one time in {@link FeatherLure#LOSE_ONE_IN}, and the hand
 * then holds a plain fishing rod with the same wear.
 */
public class LuredFishingRod extends FishingRodItem {
	public LuredFishingRod(int id, int texture) {
		super(id);
		setTextureId(texture);
	}

	@Override
	public ItemStack use(ItemStack stack, World world, PlayerEntity user) {
		FishingBobberEntity hook = user.fishHook;
		// Beta's bobber gives a fish when nothing's hooked and one has bitten: asked before the reel, which ends the bobber.
		boolean fish = hook != null && hook.hookedEntity == null && ((FishingBobberAccess) hook).featherlure$hookCountdown() > 0;
		ItemStack held = super.use(stack, world, user);
		// A client on a server never has a bobber of its own to reel (beta's client is never told whose one is), so this is where the world runs; the hand reaches
		// the client as the server's slot update, which follows any hand that ends up other than the client said it held.
		if (!fish || world.isRemote || held.count <= 0 || world.random.nextInt(FeatherLure.LOSE_ONE_IN) != 0) return held;
		FeatherLure.lose(user, hook.x, hook.y, hook.z);
		return new ItemStack(Item.FISHING_ROD, 1, held.getDamage());
	}
}
