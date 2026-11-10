package org.studioquire.faithful.wolfpacks.selftest.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.client.ClientTest;

/**
 * A real right click with fish on a tamed wolf, through the client's own interaction. A client never guesses a feeding (`WolfFood`), as beta's client
 * guesses none with fish, so on a server the fish leaving the hand is the server's answer, arriving as the slot: what a client without the mod sees too.
 */
public final class FeedingClientTest extends ClientTest {
	private WolfEntity wolf;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private WolfEntity nearWolf() {
		for (Object o : mc.world.entities) {
			Entity e = (Entity) o;
			if (e instanceof WolfEntity && !e.dead && e.getDistance(mc.player) < 4) return (WolfEntity) e;
		}
		return null;
	}

	@Override
	protected void script() {
		scene("feeder");
		await("the wolf and three fish in hand", 100, () -> (wolf = nearWolf()) != null && held() != null && held().itemId == Item.RAW_FISH.id && held().count == 3);
		then(() -> mc.interactionManager.interactEntity(mc.player, wolf));
		await("one fish gone from the client's hand", 40, () -> held() != null && held().count == 2);
		scene("fed");
	}
}
