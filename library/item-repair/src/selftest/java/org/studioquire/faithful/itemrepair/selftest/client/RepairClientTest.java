package org.studioquire.faithful.itemrepair.selftest.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.itemrepair.ItemRepair;

/**
 * Real clicks in the inventory's grid with Repair Bonus on: the rule lives where the world runs, so on a server the client shows a result worked out from the value it heard, and the two
 * have to agree, or the click taking it is refused. It's also the first real use of the core's worked-out recipe results through the grid on both sides.
 */
public final class RepairClientTest extends ClientTest {
	private static final int LIMIT = 40;
	private static final int HOTBAR = 36;
	/** Diamond picks at 1000 and 1200, 562 and 362 uses left, with 78 more for the bonus. */
	private static final int WITH_BONUS = 560;

	private void slot(int index, int button, boolean shift) {
		mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, index, button, shift, mc.player);
	}

	private int picks() {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == Item.DIAMOND_PICKAXE) n++;
		return n;
	}

	@Override
	protected void script() {
		if (restarted()) {
			then(() -> check("the bonus came through the restart off", !ItemRepair.bonus.get()));
			return;
		}
		then(() -> mc.setScreen(null));
		scene("bonus", "true");
		scene("kit");
		await("the picks, and the bonus heard", LIMIT, () -> picks() == 2 && ItemRepair.bonus.get());
		then(() -> {
			check("the inventory's grid is the current screen", mc.player.currentScreenHandler == mc.player.playerScreenHandler);
			slot(HOTBAR, 0, false);
			slot(1, 0, false);
			slot(HOTBAR + 1, 0, false);
			slot(4, 0, false);
		});
		wait(5);
		then(() -> {
			ItemStack result = mc.player.currentScreenHandler.getSlot(0).getStack();
			check("the client shows one pick at " + WITH_BONUS + ": " + (result == null ? "nothing" : result.getItem().getTranslationKey() + " at " + result.getDamage()),
					result != null && result.getItem() == Item.DIAMOND_PICKAXE && result.count == 1 && result.getDamage() == WITH_BONUS);
		});
		scene("expect result", WITH_BONUS);
		then(() -> slot(0, 0, true));
		wait(10);
		then(() -> {
			ItemStack kept = null;
			for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == Item.DIAMOND_PICKAXE) kept = s;
			check("a shift-click takes it, the grid empty, on the client too", picks() == 1 && kept.getDamage() == WITH_BONUS && mc.player.currentScreenHandler.getSlot(1).getStack() == null
					&& mc.player.currentScreenHandler.getSlot(4).getStack() == null && mc.player.currentScreenHandler.getSlot(0).getStack() == null);
		});
		scene("expect taken", WITH_BONUS);
		scene("bonus", "false");
		scene("clear");
		await("the bonus heard off again", LIMIT, () -> !ItemRepair.bonus.get());
	}
}
