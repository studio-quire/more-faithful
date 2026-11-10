package org.studioquire.oldfaithful.core.impl.content;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.ItemStorage;
import org.studioquire.oldfaithful.core.api.Stacks;

/** An {@code Inventory} as an {@link ItemStorage}, all its slots from every side. */
public final class VanillaStorage implements ItemStorage {
	/** Implemented by the game's furnace: its fuel table is private. */
	public interface FuelTable {
		int faithful$fuelTime(ItemStack stack);
	}

	private final Inventory inventory;

	public VanillaStorage(Inventory inventory) {
		this.inventory = inventory;
	}

	/** What {@link ItemStorage#TYPE} falls back on. */
	public static ItemStorage find(World world, int x, int y, int z, int side) {
		BlockEntity be = world.getBlockEntity(x, y, z);
		if (be instanceof ChestBlockEntity) return new VanillaStorage(chest(world, x, y, z, (Inventory) be));
		return be instanceof Inventory ? new VanillaStorage((Inventory) be) : null;
	}

	/** One chest, or both halves in the order vanilla's own screen shows them. */
	private static Inventory chest(World world, int x, int y, int z, Inventory chest) {
		int id = world.getBlockId(x, y, z);
		if (world.getBlockId(x - 1, y, z) == id) chest = new DoubleInventory("Large chest", (Inventory) world.getBlockEntity(x - 1, y, z), chest);
		if (world.getBlockId(x + 1, y, z) == id) chest = new DoubleInventory("Large chest", chest, (Inventory) world.getBlockEntity(x + 1, y, z));
		if (world.getBlockId(x, y, z - 1) == id) chest = new DoubleInventory("Large chest", (Inventory) world.getBlockEntity(x, y, z - 1), chest);
		if (world.getBlockId(x, y, z + 1) == id) chest = new DoubleInventory("Large chest", chest, (Inventory) world.getBlockEntity(x, y, z + 1));
		return chest;
	}

	@Override
	public int size() {
		return inventory.size();
	}

	@Override
	public ItemStack peek(int slot) {
		return Stacks.copy(inventory.getStack(slot));
	}

	private int limit(ItemStack stack) {
		return Math.min(inventory.getMaxCountPerStack(), stack.getMaxCount());
	}

	@Override
	public ItemStack insert(ItemStack stack, boolean simulate) {
		int left = stack.count;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack there = inventory.getStack(slot);
			if (left == 0 || there == null || !Stacks.canMerge(there, stack)) continue;
			int moved = Math.min(left, limit(stack) - there.count);
			if (moved <= 0) continue;
			left -= moved;
			if (!simulate) there.count += moved;
		}
		for (int slot = 0; slot < inventory.size(); slot++) {
			if (left == 0 || inventory.getStack(slot) != null) continue;
			int moved = Math.min(left, limit(stack));
			left -= moved;
			if (!simulate) {
				ItemStack placed = stack.copy();
				placed.count = moved;
				inventory.setStack(slot, placed);
			}
		}
		if (left != stack.count && !simulate) inventory.markDirty();
		if (left == 0) return null;
		ItemStack rest = stack.copy();
		rest.count = left;
		return rest;
	}

	@Override
	public ItemStack extract(int slot, int amount, boolean simulate) {
		ItemStack there = inventory.getStack(slot);
		if (there == null || amount <= 0) return null;
		if (simulate) {
			ItemStack copy = there.copy();
			copy.count = Math.min(amount, there.count);
			return copy;
		}
		ItemStack taken = inventory.removeStack(slot, amount);
		inventory.markDirty();
		return taken;
	}
}
