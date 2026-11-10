package org.studioquire.faithful.waterbottles.selftest.client;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.waterbottles.WaterBottles;

/** A client's real right clicks and bench clicks, where a server has the last word, and the pictures. */
public final class BottlesClientTest extends ClientTest {
	private static final int LIMIT = 40;

	private Item glass, water;
	private int x, feet, z;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private boolean holding(Item item, int count) {
		return held() != null && held().getItem() == item && held().count == count;
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private void click() {
		mc.interactionManager.interactItem(mc.player, mc.world, held());
	}

	/** Looks at a cell by its offset from the plot's middle, then gives the server time to hear. */
	private void aim(int dx, int dy, int dz) {
		then(() -> {
			double tx = x + dx + 0.5 - mc.player.x, ty = feet + dy + 0.5 - mc.player.y, tz = z + dz + 0.5 - mc.player.z;
			mc.player.yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
			mc.player.pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.hypot(tx, tz)));
		});
		wait(5);
	}

	private void fills() {
		scene("hold", "glass", 1);
		await("the bottle in the hand", LIMIT, () -> holding(glass, 1));
		then(() -> {
			click();
			if (remote()) check("no guess on a client: it waits for the server's word", holding(glass, 1));
		});
		until("the hand holds a water bottle", LIMIT, () -> holding(water, 1));
		scene("has", 1, 0);
	}

	private void drinks() {
		then(() -> {
			click();
			check("drinking is at once on the client too", holding(glass, 1));
		});
		wait(10);
		then(() -> check("and stays that way", holding(glass, 1) && count(water) == 0));
		scene("has", 0, 1);
	}

	private void fillsFromStack() {
		scene("hold", "glass", 3);
		await("the stack in the hand", LIMIT, () -> holding(glass, 3));
		then(this::click);
		until("a stack of three: two in the hand, and the client hears of the water bottle in another slot", LIMIT, () -> holding(glass, 2) && count(water) == 1);
		scene("has", 1, 2);
	}

	private void disagrees() {
		scene("hold", "glass", 1);
		await("the bottle in the hand", LIMIT, () -> holding(glass, 1));
		scene("look away");
		then(this::click);
		wait(10);
		then(() -> check("the client looks at water and the server doesn't: no fill on either side", holding(glass, 1)));
		scene("has", 0, 1);

		scene("hold", "water", 1);
		await("the water bottle in the hand", LIMIT, () -> holding(water, 1));
		scene("ghost");
		then(() -> {
			click();
			check("a client that's wrong about its hand guesses a drink", holding(glass, 1));
		});
		until("and the server puts the hand right", LIMIT, () -> held() != null && held().itemId == Block.DIRT.id && held().count == 5);
	}

	private void crafts() {
		scene("hold", "glass block", 3);
		scene("bench");
		await("the glass in the hand", LIMIT, () -> held() != null && held().count == 3);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, held(), x - 2, feet, z, 1));
		await("the bench to open", LIMIT, () -> mc.player.currentScreenHandler instanceof CraftingScreenHandler);
		then(() -> {
			int sync = mc.player.currentScreenHandler.syncId, hand = 37 + mc.player.inventory.selectedSlot;
			mc.interactionManager.clickSlot(sync, hand, 0, false, mc.player);
			for (int cell : new int[] {1, 3, 5}) mc.interactionManager.clickSlot(sync, cell, 1, false, mc.player);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the bench shows three bottles for a V of glass", result != null && result.getItem() == glass && result.count == 3);
			int sync = mc.player.currentScreenHandler.syncId, hand = 37 + mc.player.inventory.selectedSlot;
			mc.interactionManager.clickSlot(sync, 0, 0, false, mc.player);
			mc.interactionManager.clickSlot(sync, hand, 0, false, mc.player);
			mc.player.closeHandledScreen();
		});
		until("the bottles are in the hand", LIMIT, () -> holding(glass, 3));
		wait(10);
		then(() -> check("and stay there once the server has answered", holding(glass, 3)));
		scene("has", 0, 3);
	}

	private void pictures() {
		scene("beside beta");
		then(() -> mc.setScreen(new InventoryScreen(mc.player)));
		wait(5);
		picture("inventory");
		then(() -> {
			mc.setScreen(null);
			mc.player.pitch = 0.0F;
		});
		for (int slot : new int[] {0, 1, 2, 3, 5}) {
			then(() -> mc.player.inventory.selectedSlot = slot);
			wait(20);
			picture("hand-" + slot);
		}
		then(() -> {
			mc.player.inventory.selectedSlot = 8;
			mc.options.hideHud = true;
		});
		scene("on the ground");
		aim(0, 0, 2);
		wait(10);
		picture("ground");
		then(() -> {
			mc.player.yaw = 0.0F;
			mc.player.pitch = 55.0F;
		});
		wait(3);
		picture("ground-close");
		then(() -> {
			mc.options.hideHud = false;
			mc.player.inventory.selectedSlot = 0;
		});
	}

	@Override
	protected void script() {
		glass = WaterBottles.glassBottle.get();
		water = WaterBottles.waterBottle.get();
		if (restarted()) {
			then(() -> {
				check("the client has the bottles back", mc.player.inventory.main[0] != null && mc.player.inventory.main[0].getItem() == water && count(glass) == 5);
				mc.player.inventory.selectedSlot = 0;
				click();
				check("and the water bottle still drinks", holding(glass, 1));
			});
			wait(5);
			scene("has", 0, 6);
			return;
		}
		then(() -> {
			x = MathHelper.floor(mc.player.x);
			feet = MathHelper.floor(mc.player.boundingBox.minY);
			z = MathHelper.floor(mc.player.z);
			mc.setScreen(null);
		});
		aim(2, -1, 0);
		fills();
		drinks();
		fillsFromStack();
		if (remote()) disagrees();
		crafts();
		pictures();
		scene("leave");
	}
}
