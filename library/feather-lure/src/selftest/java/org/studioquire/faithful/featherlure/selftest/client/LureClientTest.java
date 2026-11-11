package org.studioquire.faithful.featherlure.selftest.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.featherlure.FeatherLure;
import org.studioquire.faithful.featherlure.client.FeatherLureClient;
import org.studioquire.faithful.harness.test.client.ClientTest;

/**
 * What a client sees: that the server told it of each feather a fish took, and in single player, pictures of the rod in hand, cast (beta's cast rod, from vanilla's sheet
 * though the rod's own tile is in a mod's atlas) and a lost feather's bits. On a server beta's client never knows its own bobber, so the hand never shows the cast rod
 * there, for any rod. Then a bobber afloat in the pool, with the feather while the Lured Fishing Rod is in hand and without it once a plain rod is, which on a server
 * this client knows only from the server, with pictures of both.
 */
public final class LureClientTest extends ClientTest {
	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	@Override
	protected void script() {
		if (restarted()) return;
		check("the client heard of the feathers fish took: " + FeatherLureClient.heard + " times", FeatherLureClient.heard > 0);
		if (!remote()) inHand();
		bobbers();
	}

	private void inHand() {
		then(() -> {
			// Toward the pool, a little down.
			mc.player.yaw = -90.0F;
			mc.player.pitch = 20.0F;
		});
		scene("hold");
		wait(5);
		then(() -> {
			ItemStack held = held();
			check("held, the hand draws the Lured Fishing Rod's own tile", held != null && held.getItem() == FeatherLure.luredRod.get()
					&& mc.player.getItemStackTextureId(held) == held.getTextureId());
			picture("held");
		});
		scene("cast");
		wait(10);
		then(() -> {
			ItemStack held = held();
			check("cast, beta's cast rod", held != null && mc.player.fishHook != null && mc.player.getItemStackTextureId(held) == Item.FISHING_ROD.getTextureId(0) + 16);
			picture("cast");
		});
		scene("reel");
		then(() -> {
			mc.player.pitch = 0.0F;
		});
		wait(2);
		// Two blocks ahead of the eyes, which single player keeps at the player's position.
		then(() -> {
			double yaw = Math.toRadians(mc.player.yaw), pitch = Math.toRadians(mc.player.pitch);
			bits = new String[] {String.valueOf(mc.player.x - Math.sin(yaw) * Math.cos(pitch) * 2.0), String.valueOf(mc.player.y - Math.sin(pitch) * 2.0),
					String.valueOf(mc.player.z + Math.cos(yaw) * Math.cos(pitch) * 2.0)};
		});
		then(() -> scene("lose", (Object[]) bits));
		wait(3);
		then(() -> picture("bits"));
	}

	/** The bobber this client sees, alive; on a server its owner is never known here, so not {@code fishHook}. */
	private FishingBobberEntity bobber() {
		FishingBobberEntity found = null;
		for (Object e : mc.world.entities) {
			if (e instanceof FishingBobberEntity && !((Entity) e).dead) found = (FishingBobberEntity) e;
		}
		return found;
	}

	private void bobbers() {
		String where = remote() ? ", by the server's word" : "";
		scene("hold");
		scene("float");
		until("the bobber afloat shows the feather with the Lured Fishing Rod in hand" + where, 100, () -> bobber() != null && FeatherLure.looksLured(bobber()));
		// Two cells nearer the pool, looking down into it, for pictures where the feather is more than a few pixels, once the bobber's settled.
		then(() -> {
			mc.player.setPosition(mc.player.x + 2.0, mc.player.y, mc.player.z);
			mc.player.yaw = -90.0F;
			mc.player.pitch = 40.0F;
		});
		wait(10);
		picture("bobber-lured");
		scene("plain");
		until("and beta's bobber once a plain fishing rod is in hand, the line still out" + where, 100, () -> bobber() != null && !FeatherLure.looksLured(bobber()));
		picture("bobber-plain");
		scene("reel");
		then(() -> mc.player.setPosition(mc.player.x - 2.0, mc.player.y, mc.player.z));
	}

	private String[] bits;
}
