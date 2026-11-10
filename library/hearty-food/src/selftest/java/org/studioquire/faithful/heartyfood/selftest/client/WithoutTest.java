package org.studioquire.faithful.heartyfood.selftest.client;

import net.minecraft.item.Item;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.heartyfood.HeartyFood;

/** A client with the mod on a server without it: beta's healing, at once, and no ghost hearts. Hurt by a real fall onto a floor of its own, fed with {@code /give}. */
public final class WithoutTest extends ClientTest {
	private static final int LIMIT = 100, FALL = 9, LANDED = 40;

	private int before;

	@Override
	protected void script() {
		then(() -> check("the client is admitted", mc.world != null && remote()));
		// The fall, and the player's height over it.
		ownGround(FALL + 2);
		then(() -> mc.player.sendChatMessage("/give " + mc.player.name + " " + Item.BREAD.id + " 1"));
		await("the server's gift", LIMIT, () -> hotbarSlot(Item.BREAD.id) >= 0);
		then(() -> {
			mc.player.inventory.selectedSlot = hotbarSlot(Item.BREAD.id);
			mc.player.setPosition(mc.player.x, mc.player.y + FALL, mc.player.z);
		});
		wait(LANDED);
		then(() -> check("a fall hurts: " + mc.player.health, mc.player.health < HeartyFood.FULL));
		then(() -> {
			before = mc.player.health;
			mc.interactionManager.interactItem(mc.player, mc.world, mc.player.inventory.getSelectedItem());
		});
		// At least: the real test server is Peaceful, which goes on healing by itself.
		until("beta's healing comes at once, with no half-hearts to wait for", 10, () -> mc.player.health >= Math.min(HeartyFood.FULL, before + 5));
		wait(30);
		then(() -> check("with no ghost hearts", !HeartyFood.endsAt.has(mc.player)));
	}
}
