package org.studioquire.faithful.featherlure.selftest;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.faithful.featherlure.FeatherLure;
import org.studioquire.faithful.featherlure.mixin.FishingBobberAccess;
import org.studioquire.faithful.harness.test.WorldTest;

/**
 * Where the world runs: the recipe, a lured bobber's bites against a plain one's by the game's own roll, the feather lost to fish, what reeling costs, and the mark a
 * bobber's look goes by. The bobbers are beta's, made as a cast makes them and ticked here, floating in a pool on the plot, so every tick is one roll for a bite.
 */
public final class LureTest extends WorldTest {
	/** Ticks of each bobber: at 1 in 500 that's 400 bites give or take 20, at 1 in 250 800 give or take 28. */
	private static final int ROLLS = 200_000;
	/**
	 * A bobber's tick takes about 170 of beta's cached boxes and vectors, and beta empties those caches once a frame but never shrinks them: 25,000 ticks in one
	 * frame left about 350 MB held for the rest of the game, and the library test's single-player client ran out of its 1 GB later (2026-11-11).
	 */
	private static final int ROLLS_A_TICK = 2_500;
	/** Fish reeled in on a lured line: at 1 in 8, 250 feathers lost give or take 15. */
	private static final int REELS = 2_000;
	private static final int REELS_A_TICK = 200;
	/** The pool's cell, from the plot's middle: in the floor, four cells off so the player is never within a hook's reach of the bobber. */
	private static final int POOL_X = 4, POOL_Y = -1, POOL_Z = 0;

	private Item lured;

	private static ItemStack crafted(int side, ItemStack... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, side, side);
		for (int i = 0; i < cells.length; i++) grid.setStack(i, cells[i]);
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private static boolean is(ItemStack stack, Item item, int damage) {
		return stack != null && stack.getItem() == item && stack.count == 1 && stack.getDamage() == damage;
	}

	private static String said(ItemStack stack) {
		return stack == null ? "nothing" : stack.count + " of " + stack.getItem().getTranslationKey() + " at " + stack.getDamage();
	}

	private static FishingBobberAccess access(FishingBobberEntity bobber) {
		return (FishingBobberAccess) bobber;
	}

	/** A bobber as a cast makes one, not in the world: this test ticks it. */
	private FishingBobberEntity cast() {
		FishingBobberEntity bobber = new FishingBobberEntity(world, player);
		afloat(bobber);
		return bobber;
	}

	/** Back in the middle of the pool, still, with nothing on its hook. */
	private void afloat(FishingBobberEntity bobber) {
		bobber.setPosition(x + POOL_X + 0.5, y + POOL_Y + 0.4, z + POOL_Z + 0.5);
		bobber.velocityX = bobber.velocityY = bobber.velocityZ = 0.0;
		bobber.hookedEntity = null;
		access(bobber).featherlure$setInGround(false);
	}

	private void recipe() {
		ItemStack made = crafted(2, new ItemStack(Item.FISHING_ROD, 1, 37), new ItemStack(Item.FEATHER));
		check("a worn fishing rod and a feather make a Lured Fishing Rod with the same wear: " + said(made), is(made, lured, 37));
		made = crafted(2, new ItemStack(Item.FEATHER), null, null, new ItemStack(Item.FISHING_ROD));
		check("a new one, the feather first: " + said(made), is(made, lured, 0));
		made = crafted(3, new ItemStack(Item.FISHING_ROD, 1, 64), null, null, null, null, null, null, null, new ItemStack(Item.FEATHER));
		check("at the bench, cells apart, a rod on its last use: " + said(made), is(made, lured, 64));
		check("not with two feathers", crafted(2, new ItemStack(Item.FISHING_ROD), new ItemStack(Item.FEATHER), new ItemStack(Item.FEATHER)) == null);
		check("and a lured rod takes no second feather", crafted(2, new ItemStack(lured), new ItemStack(Item.FEATHER)) == null);
	}

	/** Still water in the floor with stone under it, and a roof two cells over it, so rain never falls on the bobber and beta's odds are its clear-weather ones. */
	private void pool() {
		set(POOL_X, POOL_Y - 1, POOL_Z, Block.STONE);
		set(POOL_X, POOL_Y, POOL_Z, Block.WATER);
		set(POOL_X, POOL_Y + 2, POOL_Z, Block.STONE);
	}

	private void stays() {
		hold(new ItemStack(lured));
		FishingBobberEntity bobber = cast();
		bobber.tick();
		check("with a Lured Fishing Rod in hand the bobber stays out, marked as on a lured line", !bobber.dead && FeatherLure.looksLured(bobber));
		hold(new ItemStack(Item.FISHING_ROD));
		bobber.tick();
		check("with a plain fishing rod in hand instead it stays, unmarked", !bobber.dead && !FeatherLure.looksLured(bobber));
		hold(new ItemStack(Item.STICK));
		bobber.tick();
		check("and with a stick in hand it goes, as beta's does", bobber.dead);
		player.fishHook = null;
		check("and nothing about the rain on the pool", !world.isRaining(x + POOL_X, y + POOL_Y + 1, z + POOL_Z));
	}

	/** Every tick of a bobber afloat with no bite under way is one roll; how many came up a bite, against beta's odds or half of them. */
	private void bites(Supplier<ItemStack> rod, String whose, int oneIn) {
		int[] n = new int[2];
		FishingBobberEntity[] bobber = new FishingBobberEntity[1];
		then(() -> {
			hold(rod.get());
			bobber[0] = cast();
		});
		until(whose + " bobber rolls " + ROLLS + " times", ROLLS / ROLLS_A_TICK + 40, () -> {
			FishingBobberEntity b = bobber[0];
			for (int i = 0; i < ROLLS_A_TICK && n[0] < ROLLS && !b.dead; i++) {
				afloat(b);
				access(b).featherlure$setHookCountdown(0);
				b.tick();
				n[0]++;
				if (access(b).featherlure$hookCountdown() > 0) n[1]++;
			}
			return n[0] >= ROLLS || b.dead;
		});
		then(() -> {
			double p = 1.0 / oneIn, mean = ROLLS * p, spread = Math.sqrt(ROLLS * p * (1 - p));
			check(String.format("%s bobber bit %d times in %d rolls, 1 in %.0f, where 1 in %d gives %.0f give or take %.0f (six times that allowed)", whose, n[1], n[0],
					n[0] / Math.max(1.0, n[1]), oneIn, mean, spread), n[0] == ROLLS && Math.abs(n[1] - mean) <= 6 * spread);
			player.fishHook = null;
		});
	}

	/** Beta's costs for a reel, on a lured rod: a fish's is in {@link #losing}. */
	private void costs() {
		ItemStack rod = new ItemStack(lured, 1, 10);
		hold(rod);
		cast();
		ItemStack after = rod.use(world, player);
		check("reeling in an empty line costs nothing: " + said(after), is(after, lured, 10));
		FishingBobberEntity bobber = cast();
		PigEntity pig = new PigEntity(world);
		pig.setPosition(x + POOL_X + 0.5, y, z + POOL_Z + 1.5);
		bobber.hookedEntity = pig;
		after = rod.use(world, player);
		check("something hooked costs 3: " + said(after), is(after, lured, 13));
		bobber = cast();
		access(bobber).featherlure$setInGround(true);
		after = rod.use(world, player);
		check("off the ground, 2: " + said(after), is(after, lured, 15));
		check("and the line's in", player.fishHook == null);
	}

	/** A fish on every reel: each costs 1, and about one in eight takes the feather, leaving a plain rod with the same wear. */
	private void losing() {
		int[] n = new int[3];
		until(REELS + " fish reeled in on a lured line", 40, () -> {
			for (int i = 0; i < REELS_A_TICK && n[0] < REELS; i++) {
				ItemStack rod = new ItemStack(lured, 1, 30);
				hold(rod);
				access(cast()).featherlure$setHookCountdown(20);
				int before = world.entities.size();
				ItemStack after = rod.use(world, player);
				// The fish, thrown at the player: nobody needs two thousand of them.
				for (int k = before; k < world.entities.size(); k++) ((Entity) world.entities.get(k)).markDead();
				n[0]++;
				if (is(after, Item.FISHING_ROD, 31)) n[1]++;
				else if (!is(after, lured, 31)) n[2]++;
			}
			return n[0] >= REELS;
		});
		then(() -> {
			check("every fish cost the rod 1, leaving the Lured Fishing Rod or a plain one (" + n[2] + " didn't)", n[2] == 0);
			check(String.format("a fish took the feather %d times in %d, 1 in %.1f, where 1 in %d gives %d give or take 15 (between 1 in 12 and 1 in 6 allowed)", n[1], n[0],
					n[0] / Math.max(1.0, n[1]), FeatherLure.LOSE_ONE_IN, REELS / FeatherLure.LOSE_ONE_IN), n[1] >= REELS / 12 && n[1] <= REELS / 6);
			hold(null);
		});
	}

	private void scenes() {
		scene("hold", a -> hold(new ItemStack(lured, 1, 30)));
		scene("cast", a -> world.spawnEntity(new FishingBobberEntity(world, player)));
		// A bobber afloat in the pool on the line of the rod in hand, at the height where it floats, and a plain rod taking the lured one's place with it out.
		scene("float", a -> {
			FishingBobberEntity bobber = cast();
			bobber.setPosition(bobber.x, y + POOL_Y + 0.9, bobber.z);
			world.spawnEntity(bobber);
		});
		scene("plain", a -> hold(new ItemStack(Item.FISHING_ROD, 1, 30)));
		scene("reel", a -> {
			if (player.fishHook != null) player.fishHook.markDead();
			player.fishHook = null;
			hold(null);
		});
		scene("lose", a -> FeatherLure.lose(player, Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2])));
	}

	@Override
	protected void script() {
		lured = FeatherLure.luredRod.get();
		if (restarted()) {
			ItemStack kept = player.inventory.main[8];
			check("after a restart, a worn Lured Fishing Rod is still one, its wear kept: " + said(kept), is(kept, lured, 23));
			return;
		}
		scenes();
		recipe();
		pool();
		then(this::stays);
		bites(() -> new ItemStack(Item.FISHING_ROD), "a plain rod's", 500);
		bites(() -> new ItemStack(lured), "a lured rod's", 500 / FeatherLure.BITE_DIVISOR);
		then(this::costs);
		losing();
		then(() -> {
			player.inventory.main[8] = new ItemStack(lured, 1, 23);
			player.currentScreenHandler.sendContentUpdates();
		});
	}
}
