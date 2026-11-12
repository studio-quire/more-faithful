package org.studioquire.faithful.snipping.selftest;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.pumpkins.Pumpkins;
import org.studioquire.faithful.pumpkins.Spreading;
import org.studioquire.faithful.snipping.Snips;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * Each of the six beside its twin, one snipped by real shears and one not, given random ticks as beta's world gives them (through the same test the mod's hook on that tick
 * makes): the snipped one stays as it is and the other grows. A second snip does nothing and costs nothing; bonemeal on a snipped sapling or wheat is refused and kept; a
 * snipped sugar cane stays as tall when its top is broken off or another is put on it; what a snipped plant drops is the plain item, and planted again it grows. After a
 * restart, each is still snipped and still doesn't grow.
 */
public final class SnippingTest extends WorldTest {
	private static final int ROUNDS = 3000, PUMPKIN_ROUNDS = 20000;
	private final Random random = new Random(2);
	private int grown;

	/** Offsets from the plot's middle, the snipped one at {@code -dx}, its twin at {@code dx}. */
	private static final int[] SAPLING = {6, 0, -6}, WHEAT = {2, 0, -6}, BONEMEAL_WHEAT = {0, 0, -6}, PUMPKIN = {4, 0, -1}, MUSHROOM = {5, 0, 3}, CANE = {1, 0, 7}, CACTUS = {6, 0, 7};
	/** The wheat the client test clicks. */
	public static final int[] CLICKED = {0, 0, 2};

	private int bx(int[] at, boolean snipped) {
		return x + (snipped ? -at[0] : at[0]);
	}

	/** A random tick, as the mod's hook on beta's lets it through. */
	private void randomTick(int cx, int cy, int cz) {
		int id = world.getBlockId(cx, cy, cz);
		if (id == 0) return;
		Block block = Block.BLOCKS[id];
		if (Snips.stops(block, world, cx, cy, cz)) Snips.stillTick(block, world, cx, cy, cz);
		else block.onTick(world, cx, cy, cz, random);
	}

	/** Random ticks to every cell of the column at a spot, from the floor up to 4 above it. */
	private void ticks(int cx, int cz, int rounds) {
		for (int r = 0; r < rounds; r++) {
			for (int cy = y; cy <= y + 4; cy++) randomTick(cx, cy, cz);
		}
	}

	private ItemStack shears() {
		return player.inventory.main[0];
	}

	/** Real shears on a cell: whether they did anything, and their wear before and after. */
	private boolean snip(int cx, int cy, int cz) {
		return shears().useOnBlock(player, world, cx, cy, cz, 1);
	}

	private int height(int cx, int cz, int id) {
		int n = 0;
		for (int cy = y; cy < y + 8 && world.getBlockId(cx, cy, cz) == id; cy++) n++;
		return n;
	}

	private int mushrooms(int cx, int cz) {
		int n = 0;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) if (world.getBlockId(cx + dx, y + dy, cz + dz) == Block.BROWN_MUSHROOM.id) n++;
		return n;
	}

	private void lay() {
		// Stone under the floor first: the plot's floor may lie over air or water, where sand would fall and water run off.
		for (int dx = -WorldTest.PLOT; dx <= WorldTest.PLOT; dx++) for (int dz = -WorldTest.PLOT; dz <= WorldTest.PLOT; dz++) world.setBlock(x + dx, y - 2, z + dz, Block.STONE.id);
		for (boolean s : new boolean[] {true, false}) {
			world.setBlock(bx(SAPLING, s), y - 1, z + SAPLING[2], Block.GRASS_BLOCK.id);
			world.setBlock(bx(SAPLING, s), y, z + SAPLING[2], Block.SAPLING.id, 0);
			world.setBlock(bx(WHEAT, s), y - 1, z + WHEAT[2], Block.FARMLAND.id);
			world.setBlock(bx(WHEAT, s), y, z + WHEAT[2], Block.WHEAT.id, 0);
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) world.setBlock(bx(PUMPKIN, s) + dx, y - 1, z + PUMPKIN[2] + dz, Block.GRASS_BLOCK.id);
			world.setBlock(bx(PUMPKIN, s), y, z + PUMPKIN[2], Block.PUMPKIN.id, 2);
			// A box with a roof, as dark as a cave, so the mushroom can spread inside it.
			int mx = bx(MUSHROOM, s), mz = z + MUSHROOM[2];
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
					for (int dy = 0; dy <= 1; dy++) world.setBlock(mx + dx, y + dy, mz + dz, wall ? Block.STONE.id : 0);
					world.setBlock(mx + dx, y + 2, mz + dz, Block.STONE.id);
				}
			}
			world.setBlock(mx, y, mz, Block.BROWN_MUSHROOM.id);
			// Sugar cane on grass by still water, two high for the snipped one.
			int cx = bx(CANE, s), cz = z + CANE[2];
			world.setBlock(cx, y - 1, cz, Block.GRASS_BLOCK.id);
			world.setBlock(cx + (s ? -1 : 1), y - 1, cz, Block.WATER.id);
			world.setBlock(cx, y, cz, Block.SUGAR_CANE.id);
			if (s) world.setBlock(cx, y + 1, cz, Block.SUGAR_CANE.id);
			world.setBlock(bx(CACTUS, s), y - 1, z + CACTUS[2], Block.SAND.id);
			world.setBlock(bx(CACTUS, s), y, z + CACTUS[2], Block.CACTUS.id);
		}
		world.setBlock(x + BONEMEAL_WHEAT[0], y - 1, z + BONEMEAL_WHEAT[2], Block.FARMLAND.id);
		world.setBlock(x + BONEMEAL_WHEAT[0], y, z + BONEMEAL_WHEAT[2], Block.WHEAT.id, 0);
	}

	private void snipAll() {
		int[][] cells = {{bx(SAPLING, true), y, z + SAPLING[2]}, {bx(WHEAT, true), y, z + WHEAT[2]}, {bx(PUMPKIN, true), y, z + PUMPKIN[2]},
				{bx(MUSHROOM, true), y, z + MUSHROOM[2]}, {bx(CANE, true), y, z + CANE[2]}, {bx(CACTUS, true), y, z + CACTUS[2]}};
		String[] names = {"a sapling", "wheat", "a pumpkin", "a mushroom", "sugar cane", "a cactus"};
		for (int i = 0; i < cells.length; i++) {
			int[] c = cells[i];
			int wear = shears().getDamage();
			boolean first = snip(c[0], c[1], c[2]);
			int after = shears().getDamage();
			boolean second = snip(c[0], c[1], c[2]);
			check("shears snip " + names[i] + " at a use of their wear (" + wear + " to " + after + "), and it reads as snipped",
					first && after == wear + 1 && Snips.snipped(world, c[0], c[1], c[2]));
			check("and a second snip does nothing and costs nothing", !second && shears().getDamage() == after);
		}
		check("shears on a twin left as it was do nothing to it: the twins aren't snipped",
				!Snips.snipped(world, bx(SAPLING, false), y, z + SAPLING[2]) && !Snips.snipped(world, bx(CANE, false), y, z + CANE[2]));
		int wear = shears().getDamage();
		check("shears on grass or stone do nothing", !snip(bx(PUMPKIN, false) + 1, y - 1, z + PUMPKIN[2]) && !snip(x, y - 1, z) && shears().getDamage() == wear);
		// The snipped cane's top, which counts with the column.
		check("snipping the cane's bottom snips its top too", Snips.snipped(world, bx(CANE, true), y + 1, z + CANE[2]) && !snip(bx(CANE, true), y + 1, z + CANE[2]));
	}

	private void bonemeal() {
		ItemStack meal = new ItemStack(Item.DYE, 2, 15);
		boolean used = meal.useOnBlock(player, world, bx(SAPLING, true), y, z + SAPLING[2], 1);
		boolean wheat = meal.useOnBlock(player, world, bx(WHEAT, true), y, z + WHEAT[2], 1);
		check("bonemeal on a snipped sapling or wheat does nothing and isn't used: " + meal.count + " left",
				!used && !wheat && meal.count == 2 && world.getBlockId(bx(SAPLING, true), y, z + SAPLING[2]) == Block.SAPLING.id && world.getBlockMeta(bx(WHEAT, true), y, z + WHEAT[2]) == 0);
		boolean grew = meal.useOnBlock(player, world, x + BONEMEAL_WHEAT[0], y, z + BONEMEAL_WHEAT[2], 1);
		check("on wheat not snipped it ripens it at once, as beta's does", grew && meal.count == 1 && world.getBlockMeta(x + BONEMEAL_WHEAT[0], y, z + BONEMEAL_WHEAT[2]) == 7);
	}

	/** The snipped ones first, while nothing grown is near them; then their twins. */
	private void grow() {
		Spreading.grew = c -> grown++;
		try {
			ticks(bx(SAPLING, true), z + SAPLING[2], ROUNDS);
			ticks(bx(WHEAT, true), z + WHEAT[2], ROUNDS);
			ticks(bx(PUMPKIN, true), z + PUMPKIN[2], PUMPKIN_ROUNDS);
			ticks(bx(MUSHROOM, true), z + MUSHROOM[2], ROUNDS);
			ticks(bx(CANE, true), z + CANE[2], ROUNDS);
			ticks(bx(CACTUS, true), z + CACTUS[2], ROUNDS);
			check("a snipped sapling stays a sapling, as it was", world.getBlockId(bx(SAPLING, true), y, z + SAPLING[2]) == Block.SAPLING.id && world.getBlockMeta(bx(SAPLING, true), y, z + SAPLING[2]) == 0);
			check("snipped wheat stays at its stage", world.getBlockMeta(bx(WHEAT, true), y, z + WHEAT[2]) == 0);
			check("a snipped pumpkin sets none round it", grown == 0);
			check("a snipped mushroom doesn't spread", mushrooms(bx(MUSHROOM, true), z + MUSHROOM[2]) == 1);
			check("snipped sugar cane stays two high", height(bx(CANE, true), z + CANE[2], Block.SUGAR_CANE.id) == 2);
			check("a snipped cactus stays one high", height(bx(CACTUS, true), z + CACTUS[2], Block.CACTUS.id) == 1);
			note(ROUNDS + " random ticks each, about " + (int) (ROUNDS * 409.6 / 24000) + " in-game days; the pumpkin " + PUMPKIN_ROUNDS);

			ticks(bx(SAPLING, false), z + SAPLING[2], ROUNDS);
			ticks(bx(WHEAT, false), z + WHEAT[2], ROUNDS);
			ticks(bx(PUMPKIN, false), z + PUMPKIN[2], PUMPKIN_ROUNDS);
			ticks(bx(MUSHROOM, false), z + MUSHROOM[2], ROUNDS);
			ticks(bx(CANE, false), z + CANE[2], ROUNDS);
			ticks(bx(CACTUS, false), z + CACTUS[2], ROUNDS);
			check("its twin becomes a tree", world.getBlockId(bx(SAPLING, false), y, z + SAPLING[2]) == Block.LOG.id);
			check("wheat ripens", world.getBlockMeta(bx(WHEAT, false), y, z + WHEAT[2]) == 7);
			check("a pumpkin spreads: " + grown, grown > 0);
			check("a mushroom spreads: " + mushrooms(bx(MUSHROOM, false), z + MUSHROOM[2]), mushrooms(bx(MUSHROOM, false), z + MUSHROOM[2]) > 1);
			check("sugar cane grows to three", height(bx(CANE, false), z + CANE[2], Block.SUGAR_CANE.id) == 3);
			check("a cactus grows to three", height(bx(CACTUS, false), z + CACTUS[2], Block.CACTUS.id) == 3);
		} finally {
			Spreading.grew = null;
		}
	}

	/** The snipped cane's top broken off, then another put on it: neither starts it. */
	private void cane() {
		int cx = bx(CANE, true), cz = z + CANE[2];
		world.setBlock(cx, y + 1, cz, 0);
		ticks(cx, cz, ROUNDS);
		check("snipped sugar cane with its top broken off stays one high", height(cx, cz, Block.SUGAR_CANE.id) == 1 && Snips.snipped(world, cx, y, cz));
		world.setBlock(cx, y + 1, cz, Block.SUGAR_CANE.id);
		ticks(cx, cz, ROUNDS);
		check("and with another put on it, two", height(cx, cz, Block.SUGAR_CANE.id) == 2);
	}

	/** A snipped sapling broken as a player breaks it: what lies there is a plain sapling, and one planted in its place grows. */
	private void drops() {
		int sx = bx(SAPLING, true), sz = z + SAPLING[2];
		Block.SAPLING.dropStacks(world, sx, y, sz, world.getBlockMeta(sx, y, sz));
		world.setBlock(sx, y, sz, 0);
		ItemStack dropped = null;
		for (Object o : world.entities) {
			if (o instanceof ItemEntity && ((ItemEntity) o).getSquaredDistance(sx + 0.5, y + 0.5, sz + 0.5) < 4 && ((ItemEntity) o).stack.itemId == Block.SAPLING.id) dropped = ((ItemEntity) o).stack;
		}
		check("a broken snipped sapling drops a plain oak sapling", dropped != null && ItemStack.areEqual(dropped, new ItemStack(Block.SAPLING, dropped.count, 0)));
		world.setBlock(sx, y, sz, Block.SAPLING.id, 0);
		check("and one planted where it stood isn't snipped", !Snips.snipped(world, sx, y, sz));
		ticks(sx, sz, ROUNDS);
		check("so it grows", world.getBlockId(sx, y, sz) == Block.LOG.id);
	}

	@Override
	protected void script() {
		if (restarted()) {
			then(() -> {
				check("after a restart, each is still snipped",
						Snips.snipped(world, bx(WHEAT, true), y, z + WHEAT[2]) && Snips.snipped(world, bx(PUMPKIN, true), y, z + PUMPKIN[2]) && Snips.snipped(world, bx(MUSHROOM, true), y, z + MUSHROOM[2])
								&& Snips.snipped(world, bx(CANE, true), y, z + CANE[2]) && Snips.snipped(world, bx(CACTUS, true), y, z + CACTUS[2]));
				check("and the twins aren't", !Snips.snipped(world, bx(WHEAT, false), y, z + WHEAT[2]) && !Snips.snipped(world, bx(CACTUS, false), y, z + CACTUS[2]));
				ticks(bx(WHEAT, true), z + WHEAT[2], ROUNDS);
				ticks(bx(CACTUS, true), z + CACTUS[2], ROUNDS);
				check("and still doesn't grow", world.getBlockMeta(bx(WHEAT, true), y, z + WHEAT[2]) == 0 && height(bx(CACTUS, true), z + CACTUS[2], Block.CACTUS.id) == 1);
			});
			return;
		}
		// For the client's real clicks: fresh shears in the hand and wheat to snip, and what the world says after.
		scene("fresh", a -> {
			world.setBlock(x + CLICKED[0], y - 1, z + CLICKED[2], Block.FARMLAND.id);
			world.setBlock(x + CLICKED[0], y, z + CLICKED[2], Block.WHEAT.id, 3);
			hold(new ItemStack(Item.SHEARS));
		});
		scene("snipped", a -> check("where the world runs, the wheat the client clicked is snipped, at one use of the shears (" + shears().getDamage() + ")",
				Snips.snipped(world, x + CLICKED[0], y, z + CLICKED[2]) && shears().getDamage() == 1));
		then(() -> {
			// Noon, for the light saplings and wheat grow by; the next tick sets the sky's light from it.
			world.setTime((world.getTime() / 24000 + 1) * 24000 + 6000);
			Rules.change(Pumpkins.spreading, "true");
			hold(new ItemStack(Item.SHEARS));
			lay();
		});
		wait(5);
		then(() -> {
			snipAll();
			bonemeal();
			grow();
			cane();
			drops();
		});
	}
}
