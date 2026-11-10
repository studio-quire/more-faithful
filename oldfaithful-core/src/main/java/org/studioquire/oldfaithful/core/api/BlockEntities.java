package org.studioquire.oldfaithful.core.api;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** What a mod does with its block entities, past vanilla's own methods. Declared with {@link Content#blockEntity}. */
public final class BlockEntities {
	private static final Random RANDOM = new Random();
	private static boolean swapping;

	private BlockEntities() {
	}

	/**
	 * After changing what a {@link SyncedBlockEntity} writes: sends it to the clients that have its chunk, at the end of the tick,
	 * behind the block change that may have come with it. In single player it redraws the block. Cheap to call more than once a tick.
	 */
	public static void sync(BlockEntity blockEntity) {
		if (blockEntity.world != null && !blockEntity.world.isRemote) {
			blockEntity.world.blockUpdateEvent(blockEntity.x, blockEntity.y, blockEntity.z);
		}
	}

	/**
	 * Turns a block into another around the same block entity, with its meta: a furnace's lit and unlit blocks, which exist because light belongs to the block. Safe from that block entity's own
	 * {@code tick}. Neither block's {@code onBreak} or {@code onPlaced} runs, so nothing spills and the block entity is never gone, even to a neighbour looking during the block updates after. Copying
	 * vanilla's {@code updateLitState} instead leaves a stray block entity ticking in the world. Where the world runs. A remote client sees an ordinary block change, as vanilla's furnace gives it: a
	 * new block entity there, with a {@link SyncedBlockEntity}'s data sent again, and the old one left in its list. Harmless as long as {@code tick} returns at once on a client, as it should.
	 */
	public static void swap(World world, int x, int y, int z, Block to) {
		if (world.isRemote) return;
		if (!(to instanceof BlockWithEntity)) throw new IllegalArgumentException("A block entity can't stay on a block without one: " + to);
		swapping = true;
		boolean changed;
		try {
			changed = world.setBlockWithoutNotifyingNeighbors(x, y, z, to.id, world.getBlockMeta(x, y, z));
		} finally {
			swapping = false;
		}
		if (!changed) return;
		world.blockUpdateEvent(x, y, z);
		world.notifyNeighbors(x, y, z, to.id);
	}

	/** The core's: whether a block's change is a {@link #swap}. */
	public static boolean swapping() {
		return swapping;
	}

	/** What a block entity here held, thrown out as a chest throws its own, if it's an {@code Inventory}. Call it from a block's {@code onBreak}, before {@code super}. */
	public static void spill(World world, int x, int y, int z) {
		BlockEntity blockEntity = world.getBlockEntity(x, y, z);
		if (blockEntity instanceof Inventory) spill(world, x, y, z, (Inventory) blockEntity);
	}

	/**
	 * Empties an inventory onto the ground at this block, stack data and all: copying vanilla's {@code new ItemStack(id, n, damage)} would lose it, and a backpack with it. Where the world runs; on a
	 * client it does nothing.
	 */
	public static void spill(World world, int x, int y, int z, Inventory inventory) {
		if (world.isRemote) return;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack == null) continue;
			inventory.setStack(slot, null);
			while (stack.count > 0) throwOut(world, x, y, z, stack.split(Math.min(stack.count, 10 + RANDOM.nextInt(21))));
		}
	}

	private static void throwOut(World world, int x, int y, int z, ItemStack stack) {
		ItemEntity item = new ItemEntity(world, x + 0.1F + RANDOM.nextFloat() * 0.8F, y + 0.1F + RANDOM.nextFloat() * 0.8F, z + 0.1F + RANDOM.nextFloat() * 0.8F, stack);
		item.velocityX = RANDOM.nextGaussian() * 0.05F;
		item.velocityY = RANDOM.nextGaussian() * 0.05F + 0.2F;
		item.velocityZ = RANDOM.nextGaussian() * 0.05F;
		world.spawnEntity(item);
	}
}
