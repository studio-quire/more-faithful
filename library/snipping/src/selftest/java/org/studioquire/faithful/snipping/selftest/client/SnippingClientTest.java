package org.studioquire.faithful.snipping.selftest.client;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.snipping.Snips;
import org.studioquire.faithful.snipping.selftest.SnippingTest;

/**
 * A player's real right clicks with shears on wheat: the first is taken (so the arm swings) and snips it where the world runs; the client hears of it, and the second click
 * isn't taken, so nothing at all shows. On a server, that's the snipped mark reaching the client.
 */
public final class SnippingClientTest extends ClientTest {
	private static final int LIMIT = 100;

	private int wx, wy, wz;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private boolean click() {
		return mc.interactionManager.interactBlock(mc.player, mc.world, held(), wx, wy, wz, 1);
	}

	@Override
	protected void script() {
		if (restarted()) return;
		then(() -> {
			mc.player.inventory.selectedSlot = 0;
			wx = (int) Math.floor(mc.player.x) + SnippingTest.CLICKED[0];
			wy = (int) Math.floor(mc.player.boundingBox.minY + 0.01);
			wz = (int) Math.floor(mc.player.z) + SnippingTest.CLICKED[2];
		});
		scene("fresh");
		await("fresh shears in the hand and the wheat on the client", LIMIT,
				() -> held() != null && held().itemId == Item.SHEARS.id && held().getDamage() == 0 && mc.world.getBlockId(wx, wy, wz) == Block.WHEAT.id);
		then(() -> check("the first click on wheat with shears is taken, so the arm swings", click()));
		until("the client hears it's snipped", LIMIT, () -> Snips.snipped(mc.world, wx, wy, wz));
		scene("snipped");
		then(() -> check("a second click isn't taken: no swing, nothing", !click()));
		wait(10);
		scene("snipped");
	}
}
