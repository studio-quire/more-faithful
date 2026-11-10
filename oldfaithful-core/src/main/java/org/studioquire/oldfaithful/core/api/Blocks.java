package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Players acting on blocks, where the world runs: single player and a server alike, never on a client on a server (a client's
 * guess is put right by the server, and a veto here sends the block back). What a broken block drops is {@link Drops#block}.
 */
public final class Blocks {
	/** A player's action on one cell. */
	public static class Action extends Cancellable {
		public final World world;
		public final PlayerEntity player;
		public final int x, y, z;
		/** The block as it stands. */
		public final int block, meta;

		Action(World world, PlayerEntity player, int x, int y, int z) {
			this.world = world;
			this.player = player;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = world.getBlockId(x, y, z);
			this.meta = world.getBlockMeta(x, y, z);
		}
	}

	public static final class Use extends Action {
		/** What's in hand, or null. */
		public final ItemStack stack;
		public final int side;

		Use(World world, PlayerEntity player, ItemStack stack, int x, int y, int z, int side) {
			super(world, player, x, y, z);
			this.stack = stack;
			this.side = side;
		}
	}

	/** A cell a player's item changed. */
	public static final class Placed {
		public final World world;
		public final PlayerEntity player;
		public final ItemStack stack;
		public final int x, y, z, block, meta;
		/** What the cell was: grass before a hoe, air or tall grass under a placed block. */
		public final int before, beforeMeta;

		Placed(World world, PlayerEntity player, ItemStack stack, int x, int y, int z, int before, int beforeMeta) {
			this.before = before;
			this.beforeMeta = beforeMeta;
			this.world = world;
			this.player = player;
			this.stack = stack;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = world.getBlockId(x, y, z);
			this.meta = world.getBlockMeta(x, y, z);
		}
	}

	private static final Listeners<Consumer<Placed>> PLACED = new Listeners<>("Blocks.onPlaced");
	private static final Listeners<Consumer<Action>> BROKEN = new Listeners<>("Blocks.onBroken");
	private static final Listeners<Consumer<Use>> USED = new Listeners<>("Blocks.onUsed");

	private Blocks() {
	}

	/**
	 * Notice: a player's item put a block in the world: the cell it was used on or the one beside it changed to something other
	 * than air. So a door is its lower half, a bed its foot, and a hoe's farmland counts. To stop a placement, use {@link #onUsed}.
	 * Not for blocks placed by anything but a player's item.
	 */
	public static void onPlaced(Consumer<Placed> listener) {
		PLACED.add(listener);
	}

	/** Veto: a player is about to break a block. Cancelled, it stays, the tool isn't worn and nothing drops. */
	public static void onBroken(Consumer<Action> listener) {
		BROKEN.add(listener);
	}

	/**
	 * Veto: a player is using a block, with or without something in hand. Cancelled, neither the block nor the item acts on it,
	 * which stops placing against it too.
	 */
	public static void onUsed(Consumer<Use> listener) {
		USED.add(listener);
	}

	public static boolean hasPlacedListeners() {
		return !PLACED.isEmpty();
	}

	/** The core's. */
	public static void placed(World world, PlayerEntity player, ItemStack stack, int x, int y, int z, int before, int beforeMeta) {
		Placed event = new Placed(world, player, stack, x, y, z, before, beforeMeta);
		PLACED.each(l -> l.accept(event));
	}

	/** The core's: whether the break goes ahead. */
	public static boolean breaking(World world, PlayerEntity player, int x, int y, int z) {
		if (BROKEN.isEmpty() || world.isRemote) return true;
		Action event = new Action(world, player, x, y, z);
		BROKEN.each(l -> l.accept(event));
		return !event.cancelled();
	}

	/** The core's: whether the use goes ahead. */
	public static boolean using(World world, PlayerEntity player, ItemStack stack, int x, int y, int z, int side) {
		if (USED.isEmpty() || world.isRemote) return true;
		Use event = new Use(world, player, stack, x, y, z, side);
		USED.each(l -> l.accept(event));
		return !event.cancelled();
	}
}
