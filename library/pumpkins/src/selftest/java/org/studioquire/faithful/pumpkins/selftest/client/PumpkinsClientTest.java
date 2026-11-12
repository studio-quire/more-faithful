package org.studioquire.faithful.pumpkins.selftest.client;

import net.minecraft.block.Block;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.pumpkins.selftest.PumpkinsTest;

/** In single player, pictures of what the world test left: the patch grown from one pumpkin on the left, beta's own on the right, in the same meadow. */
public final class PumpkinsClientTest extends ClientTest {
	private int x, feet, z;
	private final int[] distance = {0};

	/** Held in the air there, every tick, while the land round it is drawn. */
	private void view(String name, double dx, double dy, double dz, float yaw, float pitch) {
		int[] held = {0};
		then(() -> {
			mc.options.hideHud = true;
			held[0] = 0;
		});
		await("the view " + name + " held while it's drawn", 400, () -> {
			mc.player.setPosition(x + 0.5 + dx, feet + 1.62 + dy, z + 0.5 + dz);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			mc.player.yaw = mc.player.prevYaw = yaw;
			mc.player.pitch = mc.player.prevPitch = pitch;
			return ++held[0] >= 240;
		});
		picture(name);
	}

	@Override
	protected void script() {
		if (restarted() || remote()) return;
		x = (int) Math.floor(mc.player.x);
		feet = (int) Math.floor(mc.player.boundingBox.minY + 0.01);
		z = (int) Math.floor(mc.player.z);
		then(() -> {
			check("the grown patch's first pumpkin is where the world test left it", mc.world.getBlockId(x + PumpkinsTest.GROWN, feet, z) == Block.PUMPKIN.id);
			mc.world.setTime((mc.world.getTime() / 24000 + 1) * 24000 + 6000);
			mc.world.getProperties().setRaining(false);
			// Normal render distance for these, where the test client draws Tiny.
			distance[0] = mc.options.viewDistance;
			mc.options.viewDistance = 1;
		});
		// From the south, both patches, grown on the left; then each from nearer, a little over a player's height.
		view("grown-and-wild", (PumpkinsTest.GROWN + PumpkinsTest.WILD) / 2.0, 7, 22, 180, 16);
		view("grown-near", PumpkinsTest.GROWN, 3, 9, 180, 24);
		view("wild-near", PumpkinsTest.WILD, 3, 9, 180, 24);
		then(() -> {
			mc.options.hideHud = false;
			mc.options.viewDistance = distance[0];
			mc.player.setPosition(x + 0.5, feet + 1.62, z + 0.5);
		});
		wait(20);
	}
}
