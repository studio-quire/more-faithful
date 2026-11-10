package org.studioquire.oldfaithful.core.api;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * What blocks and mobs leave behind, where the world runs. Both are filters: listeners run in order, each changing the list the
 * last left. Vanilla's chance rolls have been made by then, so {@code stacks} is what would really drop. What a chest
 * spills isn't a drop.
 */
public final class Drops {
	/** Any cause: a player, an explosion, a piston, water, a block losing its support. Generation too, when a spring's water reaches a plant, so go by {@link #world} here: it may be one the server doesn't have yet. */
	public static final class BlockDrops {
		public final World world;
		public final int x, y, z;
		public final Block block;
		public final int meta;
		/** The player breaking it, or null for anything else. */
		public final PlayerEntity player;
		public final List<ItemStack> stacks = new ArrayList<>();

		BlockDrops(World world, int x, int y, int z, Block block, int meta, PlayerEntity player) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = block;
			this.meta = meta;
			this.player = player;
		}
	}

	/** Not players, who have {@link Players#onDrops}. */
	public static final class MobDrops {
		public final LivingEntity entity;
		/** What killed it, or null. */
		public final Entity adversary;
		public final List<ItemStack> stacks = new ArrayList<>();

		MobDrops(LivingEntity entity, Entity adversary) {
			this.entity = entity;
			this.adversary = adversary;
		}
	}

	public interface Spawner {
		void drop(ItemStack stack);
	}

	private static final Listeners<Consumer<BlockDrops>> BLOCK = new Listeners<>("Drops.block");
	private static final Listeners<Consumer<MobDrops>> MOB = new Listeners<>("Drops.mob");

	/** Block drops being gathered, innermost last: one block's drops can call another's (a moving block's). */
	private static final Deque<BlockDrops> GATHERING = new ArrayDeque<>();
	private static MobDrops gatheringMob;
	private static PlayerEntity breaker;
	private static boolean spawning;

	private Drops() {
	}

	/** Filter. {@code player} is set when a player broke it, so this is the drops filter for {@link Blocks#onBroken} too. */
	public static void block(Consumer<BlockDrops> listener) {
		BLOCK.add(listener);
	}

	/** Filter. */
	public static void mob(Consumer<MobDrops> listener) {
		MOB.add(listener);
	}

	public static boolean hasBlockListeners() {
		return !BLOCK.isEmpty();
	}

	public static boolean hasMobListeners() {
		return !MOB.isEmpty();
	}

	/** The core's: {@code work} runs with this player as the breaker. */
	public static void brokenBy(PlayerEntity player, Runnable work) {
		breaker = player;
		try {
			work.run();
		} finally {
			breaker = null;
		}
	}

	/** The core's: a block's whole drop, gathered while {@code work} runs, filtered, then spawned. */
	public static void gatherBlock(World world, int x, int y, int z, Block block, int meta, Spawner spawner, Runnable work) {
		BlockDrops drops = new BlockDrops(world, x, y, z, block, meta, breaker);
		GATHERING.addLast(drops);
		try {
			work.run();
		} finally {
			GATHERING.removeLast();
		}
		BLOCK.each(l -> l.accept(drops));
		spawn(drops.stacks, spawner);
	}

	/** The core's: true if this stack was taken for a drop being gathered. Outside one, a stack is a drop of its own. */
	public static boolean takeBlockStack(World world, int x, int y, int z, Block block, ItemStack stack, Spawner spawner) {
		if (spawning || BLOCK.isEmpty()) return false;
		if (GATHERING.isEmpty()) gatherBlock(world, x, y, z, block, world.getBlockMeta(x, y, z), spawner, () -> GATHERING.peekLast().stacks.add(stack));
		else GATHERING.peekLast().stacks.add(stack);
		return true;
	}

	/** The core's: whether a block's drop is being gathered, so an item it spawns by hand, as wheat does its seeds, can join it. */
	public static boolean gathering() {
		return !GATHERING.isEmpty() && !spawning;
	}

	/** The core's. */
	public static void addBlockStack(ItemStack stack) {
		GATHERING.peekLast().stacks.add(stack);
	}

	/** The core's: a mob's drop. */
	public static void gatherMob(LivingEntity entity, Entity adversary, Spawner spawner, Runnable work) {
		MobDrops drops = new MobDrops(entity, adversary);
		gatheringMob = drops;
		try {
			work.run();
		} finally {
			gatheringMob = null;
		}
		MOB.each(l -> l.accept(drops));
		spawn(drops.stacks, spawner);
	}

	/** The core's: true if this stack was taken for the mob's drop being gathered. */
	public static boolean takeMobStack(Entity entity, ItemStack stack) {
		if (spawning || gatheringMob == null || gatheringMob.entity != entity) return false;
		gatheringMob.stacks.add(stack);
		return true;
	}

	private static void spawn(List<ItemStack> stacks, Spawner spawner) {
		boolean outer = !spawning;
		spawning = true;
		try {
			for (ItemStack s : new ArrayList<>(stacks)) {
				if (s != null && s.count > 0) spawner.drop(s);
			}
		} finally {
			if (outer) spawning = false;
		}
	}
}
