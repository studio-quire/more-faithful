package org.studioquire.faithful.waterbottles.selftest;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.waterbottles.WaterBottles;

/** Every rule of the design where the world runs, each through the item's own {@code use}, with the player's look set as a client's packets would set it. */
public final class BottlesTest extends WorldTest {
	private Item glass, water;

	private void aim(int dx, int dy, int dz) {
		double tx = x + dx + 0.5 - player.x, ty = y + dy + 0.5 - (player.y + 1.62 - player.standingEyeHeight), tz = z + dz + 0.5 - player.z;
		player.yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
		player.pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.hypot(tx, tz)));
	}

	/** What the game does with a right click that no block took. */
	private void click() {
		ItemStack held = player.inventory.getSelectedItem();
		ItemStack after = held.use(world, player);
		player.inventory.main[player.inventory.selectedSlot] = after.count == 0 ? null : after;
	}

	private void clearInventory() {
		Arrays.fill(player.inventory.main, null);
		player.currentScreenHandler.sendContentUpdates();
	}

	private boolean holding(Item item, int count) {
		ItemStack held = player.inventory.getSelectedItem();
		return held != null && held.getItem() == item && held.count == count;
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private void fillFrom(String what, int dx, int dy, int dz, boolean fills) {
		clearInventory();
		hold(new ItemStack(glass));
		aim(dx, dy, dz);
		click();
		check(what, holding(fills ? water : glass, 1));
	}

	private ItemStack crafted(int... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, 3, 3);
		for (int cell : cells) grid.setStack(cell, new ItemStack(Block.GLASS));
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private void leaveForRestart() {
		clearInventory();
		player.inventory.main[0] = new ItemStack(water);
		player.inventory.main[1] = new ItemStack(glass, 5);
		player.currentScreenHandler.sendContentUpdates();
	}

	private void scenes() {
		scene("hold", a -> {
			clearInventory();
			hold(a[0].equals("water") ? new ItemStack(water, Integer.parseInt(a[1])) : a[0].equals("glass") ? new ItemStack(glass, Integer.parseInt(a[1])) : new ItemStack(Block.GLASS, Integer.parseInt(a[1])));
		});
		scene("has", a -> check("where the world runs: " + a[0] + " water, " + a[1] + " glass bottles (" + count(water) + ", " + count(glass) + ")", count(water) == Integer.parseInt(a[0]) && count(glass) == Integer.parseInt(a[1])));
		scene("look away", a -> player.pitch = -90.0F);
		// The client goes on believing in its water bottle: the hand changes here with nothing sent, as the tracked copy says it already was.
		scene("ghost", a -> {
			ItemStack dirt = new ItemStack(Block.DIRT, 5);
			player.inventory.main[player.inventory.selectedSlot] = dirt;
			player.playerScreenHandler.trackedStacks.set(36 + player.inventory.selectedSlot, dirt.copy());
		});
		scene("bench", a -> set(-2, 0, 0, Block.CRAFTING_TABLE));
		scene("beside beta", a -> {
			clearInventory();
			ItemStack[] row = {new ItemStack(glass, 12), new ItemStack(water), new ItemStack(Item.BOWL, 12), new ItemStack(Item.MUSHROOM_STEW), new ItemStack(Item.BUCKET),
					new ItemStack(Item.WATER_BUCKET), new ItemStack(Item.MILK_BUCKET), new ItemStack(Block.GLASS, 12), new ItemStack(Item.APPLE)};
			for (int i = 0; i < row.length; i++) {
				player.inventory.main[i] = row[i];
				player.inventory.main[9 + i] = row[row.length - 1 - i].copy();
			}
			player.currentScreenHandler.sendContentUpdates();
			world.setTime(6000);
		});
		scene("on the ground", a -> {
			Item[] row = {glass, Item.BOWL, water, Item.WATER_BUCKET, Item.MUSHROOM_STEW};
			for (int i = 0; i < row.length; i++) {
				ItemEntity item = new ItemEntity(world, x + 0.5 + (i - 2) * 0.7, y + 0.2, z + 2.5, new ItemStack(row[i]));
				item.velocityX = item.velocityY = item.velocityZ = 0;
				item.pickupDelay = 30000;
				world.spawnEntity(item);
			}
		});
		scene("leave", a -> {
			for (Object e : world.entities) if (e instanceof ItemEntity) ((Entity) e).markDead();
			leaveForRestart();
		});
	}

	@Override
	protected void script() {
		glass = WaterBottles.glassBottle.get();
		water = WaterBottles.waterBottle.get();
		scenes();
		if (restarted()) {
			check("a water bottle and a stack of bottles came through the restart by name", player.inventory.main[0] != null && player.inventory.main[0].getItem() == water
					&& player.inventory.main[1] != null && player.inventory.main[1].getItem() == glass && player.inventory.main[1].count == 5);
			return;
		}

		check("bottles stack, water bottles don't", glass.getMaxCount() == 64 && water.getMaxCount() == 1);
		ItemStack made = crafted(0, 2, 4);
		check("three glass in a V make three bottles", made != null && made.getItem() == glass && made.count == 3);
		made = crafted(3, 5, 7);
		check("lower in the grid too", made != null && made.getItem() == glass && made.count == 3);
		check("and three in a row make none", crafted(0, 1, 2) == null);

		set(2, -1, 0, Block.WATER);
		// The client test fills from this cell later on, and in a cold biome beta's chunk tick turns still water open to the sky to ice: a block at the top of the world over it keeps it water.
		world.setBlock(x + 2, 127, z, Block.STONE.id);
		fillFrom("a bottle fills from still water", 2, -1, 0, true);
		check("and the water isn't used up", block(2, -1, 0) == Block.WATER.id && meta(2, -1, 0) == 0);

		set(-2, -1, 0, Block.FLOWING_WATER, 3);
		fillFrom("and from flowing water", -2, -1, 0, true);
		check("which isn't used up either", block(-2, -1, 0) == Block.FLOWING_WATER.id);
		set(-2, -1, 0, Block.STONE);

		fillFrom("not from stone", 1, -1, 1, false);
		player.pitch = -90.0F;
		click();
		check("not from the sky", holding(glass, 1));

		set(5, -1, 0, Block.WATER);
		fillFrom("nor from water past a bucket's reach", 5, -1, 0, false);
		set(5, -1, 0, Block.STONE);

		set(0, -1, 2, Block.WATER);
		set(0, 0, 1, Block.STONE);
		fillFrom("nor through a wall", 0, -1, 2, false);
		set(0, 0, 1, null);
		set(0, -1, 2, Block.STONE);

		clearInventory();
		hold(new ItemStack(glass, 3));
		aim(2, -1, 0);
		click();
		check("from a stack, one fills and the rest stay in the hand", holding(glass, 2) && count(water) == 1);
		click();
		click();
		check("until the last, which fills in the hand", holding(water, 1) && count(water) == 3 && count(glass) == 0);

		clearInventory();
		for (int i = 0; i < player.inventory.main.length; i++) player.inventory.main[i] = new ItemStack(Block.DIRT);
		hold(new ItemStack(glass, 2));
		click();
		check("with nowhere to put it, a stack still fills one", holding(glass, 1) && count(water) == 0);
		then(() -> {
			int dropped = 0;
			for (Object e : world.entities) {
				if (e instanceof ItemEntity && ((ItemEntity) e).stack.getItem() == water) {
					dropped++;
					((ItemEntity) e).markDead();
				}
			}
			check("and it drops at the player's feet", dropped == 1);
		});

		then(() -> {
			clearInventory();
			player.health = 7;
			hold(new ItemStack(water));
			aim(2, -1, 0);
			click();
			check("drinking leaves the empty bottle, even looking at water", holding(glass, 1) && count(water) == 0);
			check("and does nothing else", player.health == 7);
			player.health = 20;

			set(0, -1, -2, Block.FARMLAND);
			hold(new ItemStack(water));
			aim(0, -1, -2);
			check("farmland doesn't take the bottle", !player.inventory.getSelectedItem().useOnBlock(player, world, x, y - 1, z - 2, 1));
			click();
			check("so it's drunk there as anywhere", holding(glass, 1));
			check("and the farmland stays dry", block(0, -1, -2) == Block.FARMLAND.id && meta(0, -1, -2) == 0);
			set(0, -1, -2, Block.STONE);

			// What a give command can make; nothing in play gets here.
			hold(new ItemStack(water, 3));
			click();
			check("three water bottles in one slot give one bottle back", holding(water, 2) && count(glass) == 1);
			leaveForRestart();
		});
	}
}
