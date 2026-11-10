package org.studioquire.faithful.alchemy;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.faithful.waterbottles.Bottle;
import org.studioquire.faithful.waterbottles.WaterBottles;

/**
 * One size of one family. Drunk at once where the world runs, leaving the bottle; not drunk when there's no room for any of it (a full bar of health, the toughest ward on and whole, the longest time
 * left), as cake isn't eaten at full health.
 */
public final class Potion extends Bottle {
	/** A full bar of health, in half-hearts. */
	public static final int FULL = 20;

	public interface Wanted {
		boolean test(PlayerEntity player, int size);
	}

	public interface Drink {
		void apply(PlayerEntity player, int size);
	}

	/** 0, 1 or 2: weak, plain, strong. */
	public final int size;
	private final Wanted wanted;
	private final Drink drink;

	Potion(int id, int texture, int size, Wanted wanted, Drink drink) {
		super(id, texture);
		this.size = size;
		this.wanted = wanted;
		this.drink = drink;
		maxCount = 1;
	}

	/**
	 * No guess on a client: what it knows can be a packet behind, and a wrong guess of a drink would stay, as a server only corrects a hand that changed there. The answer comes as a slot update.
	 * Health heals by the plain {@code heal}, so anything over time another mod has going carries on beside it.
	 */
	@Override
	public ItemStack use(ItemStack stack, World world, PlayerEntity user) {
		if (world.isRemote || user.health <= 0 || !wanted.test(user, size)) return stack;
		drink.apply(user, size);
		return exchange(stack, world, user, new ItemStack(WaterBottles.glassBottle.get()));
	}
}
