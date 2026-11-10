package org.studioquire.oldfaithful.core.api;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Crops that aren't wheat: {@code DyeItem} and {@code FarmlandBlock} know only it. A plain crop is a {@link Crop}, which has all this already. */
public final class Growth {
	/** A block of a mod's that bonemeal grows, with no listener to write. Asked after {@link #onBonemeal}'s listeners, unless one has handled it or cancelled it. */
	public interface Bonemealable {
		/** Where the world runs. True if it grew, and one bonemeal is used; false leaves the bonemeal in the hand. */
		boolean grow(World world, int x, int y, int z, Random random);
	}

	/** Bonemeal used on a block. */
	public static final class Bonemeal extends Cancellable {
		public final World world;
		public final PlayerEntity player;
		public final ItemStack stack;
		public final int x, y, z, block;
		private boolean handled;

		Bonemeal(World world, PlayerEntity player, ItemStack stack, int x, int y, int z) {
			this.world = world;
			this.player = player;
			this.stack = stack;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = world.getBlockId(x, y, z);
		}

		/** This listener has grown something: one bonemeal is used, however many say so, and vanilla does nothing more. Sticky. */
		public void handled() {
			handled = true;
		}

		public boolean isHandled() {
			return handled;
		}
	}

	private static final Listeners<Consumer<Bonemeal>> BONEMEAL = new Listeners<>("Growth.onBonemeal");
	private static final BitSet FARMLAND = new BitSet();

	private Growth() {
	}

	/**
	 * Where the world runs, before vanilla looks at the block; never on a client on a server. Every listener hears.
	 * {@link Bonemeal#handled} and {@link Cancellable#cancel} are both sticky. Cancelled means nothing happens and no bonemeal is
	 * used, and it wins over handled.
	 */
	public static void onBonemeal(Consumer<Bonemeal> listener) {
		BONEMEAL.add(listener);
	}

	/** With this block on it, dry farmland stays farmland, as it does under wheat. A plain list: blocks are declared, not decided. */
	public static void farmlandHolds(Block block) {
		FARMLAND.set(block.id);
	}

	public static boolean holdsFarmland(int block) {
		return FARMLAND.get(block);
	}

	public static boolean hasFarmlandCrops() {
		return !FARMLAND.isEmpty();
	}

	/** The core's: null when vanilla should go on as it would. */
	public static Bonemeal bonemeal(World world, PlayerEntity player, ItemStack stack, int x, int y, int z) {
		if (world.isRemote) return null;
		Block block = Block.BLOCKS[world.getBlockId(x, y, z)];
		if (BONEMEAL.isEmpty() && !(block instanceof Bonemealable)) return null;
		Bonemeal event = new Bonemeal(world, player, stack, x, y, z);
		BONEMEAL.each(l -> l.accept(event));
		if (block instanceof Bonemealable && !event.cancelled() && !event.isHandled() && ((Bonemealable) block).grow(world, x, y, z, world.random)) event.handled();
		return event;
	}
}
