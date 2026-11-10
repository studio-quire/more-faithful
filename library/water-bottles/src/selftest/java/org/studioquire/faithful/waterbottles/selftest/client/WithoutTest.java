package org.studioquire.faithful.waterbottles.selftest.client;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.waterbottles.WaterBottles;

/**
 * A client with the mod on a server without it: the client's recipe shows bottles at the bench, the server takes them back, and no bottle is left behind.
 * On a real server's own terms: a bench and glass from {@code /give}, on a floor of its own, by real clicks.
 */
public final class WithoutTest extends ClientTest {
	private static final int LIMIT = 100;

	private int bottles() {
		int n = 0;
		int glass = WaterBottles.glassBottle.get().id, water = WaterBottles.waterBottle.get().id;
		for (ItemStack s : mc.player.inventory.main) if (s != null && (s.itemId == glass || s.itemId == water)) n += s.count;
		ItemStack cursor = mc.player.inventory.getCursorStack();
		return cursor != null && (cursor.itemId == glass || cursor.itemId == water) ? n + cursor.count : n;
	}

	@Override
	protected void script() {
		then(() -> check("the client is admitted", mc.world != null && remote()));
		// A bench, and the player beside it.
		ownGround(3);
		then(() -> {
			mc.player.sendChatMessage("/give " + mc.player.name + " " + Block.GLASS.id + " 3");
			mc.player.sendChatMessage("/give " + mc.player.name + " " + Block.CRAFTING_TABLE.id + " 1");
		});
		await("the server's gifts", LIMIT, () -> hotbarSlot(Block.GLASS.id) >= 0 && hotbarSlot(Block.CRAFTING_TABLE.id) >= 0);
		then(() -> mc.player.inventory.selectedSlot = hotbarSlot(Block.CRAFTING_TABLE.id));
		wait(3);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, mc.player.inventory.getSelectedItem(), groundX + 2, groundY - 1, groundZ, 1));
		await("the bench to stand", LIMIT, () -> mc.world.getBlockId(groundX + 2, groundY, groundZ) == Block.CRAFTING_TABLE.id);
		then(() -> mc.player.inventory.selectedSlot = hotbarSlot(Block.GLASS.id));
		wait(3);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, mc.player.inventory.getSelectedItem(), groundX + 2, groundY, groundZ, 1));
		await("the bench to open", LIMIT, () -> mc.player.currentScreenHandler instanceof CraftingScreenHandler);
		then(() -> {
			int sync = mc.player.currentScreenHandler.syncId;
			mc.interactionManager.clickSlot(sync, 37 + hotbarSlot(Block.GLASS.id), 0, false, mc.player);
			for (int cell : new int[] {1, 3, 5}) mc.interactionManager.clickSlot(sync, cell, 1, false, mc.player);
		});
		wait(10);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the client's own recipe shows bottles", result != null && result.getItem() == WaterBottles.glassBottle.get());
			mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, false, mc.player);
			check("and lets them be taken, as a guess", bottles() == 3);
		});
		until("the server takes them back", LIMIT, () -> bottles() == 0);
		then(() -> mc.player.closeHandledScreen());
		wait(40);
		then(() -> {
			check("no bottle is left on the client", bottles() == 0);
			check("and it's still in the game", mc.world != null && mc.player != null && mc.player.health > 0);
		});
	}
}
