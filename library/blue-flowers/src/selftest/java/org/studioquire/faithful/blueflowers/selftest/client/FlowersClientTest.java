package org.studioquire.faithful.blueflowers.selftest.client;

import net.minecraft.block.Block;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.blueflowers.BlueFlowers;
import org.studioquire.faithful.blueflowers.client.BlueFlowersClient;
import org.studioquire.faithful.blueflowers.selftest.FlowersTest;
import org.studioquire.faithful.harness.test.client.ClientTest;

/** A client's real right click and left click, its name, the row of three for looking at, and in single player a patch as it stands. */
public final class FlowersClientTest extends ClientTest {
	private static final int LIMIT = 60;

	private int x, feet, z;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private int carried(Item item) {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private void place(int dx, int slot) {
		then(() -> {
			mc.player.inventory.selectedSlot = slot;
			mc.interactionManager.interactBlock(mc.player, mc.world, held(), x + dx, feet - 1, z + 2, 1);
		});
	}

	/** One step toward a point, small enough that a server takes it as walking; true once there. */
	private boolean stepToward(double tx, double tz) {
		double dx = tx - mc.player.x, dz = tz - mc.player.z, d = Math.sqrt(dx * dx + dz * dz);
		if (d < 0.05) return true;
		double s = Math.min(0.1, d) / d;
		mc.player.setPosition(mc.player.x + dx * s, mc.player.y, mc.player.z + dz * s);
		return false;
	}

	/** The nearest dropped stack of the item the client knows of, or null. */
	private ItemEntity drop(Item item) {
		ItemEntity best = null;
		double nearest = 8 * 8;
		for (Object o : mc.world.entities) {
			if (!(o instanceof ItemEntity)) continue;
			ItemEntity e = (ItemEntity) o;
			if (e.dead || e.stack == null || e.stack.getItem() != item) continue;
			double d = e.getSquaredDistance(mc.player);
			if (d < nearest) {
				nearest = d;
				best = e;
			}
		}
		return best;
	}

	@Override
	protected void script() {
		if (restarted()) return;
		x = (int) Math.floor(mc.player.x);
		feet = (int) Math.floor(mc.player.boundingBox.minY + 0.01);
		z = (int) Math.floor(mc.player.z);
		Item flowerItem = Item.ITEMS[BlueFlowers.flower.get().id];
		int id = BlueFlowers.flower.get().id;
		then(() -> {
			TranslationStorage names = TranslationStorage.getInstance();
			String yellow = Item.ITEMS[Block.DANDELION.id].getTranslationKey() + ".name";
			check("it's called Bluebell", "Bluebell".equals(names.get(flowerItem.getTranslationKey() + ".name")));
			check("beta's yellow flower is called Dandelion", "Dandelion".equals(names.get(yellow)));
			BlueFlowersClient.dandelion(false);
			String off = names.get(yellow);
			BlueFlowersClient.dandelion(true);
			check("and Flower with the switch off, Dandelion again on: " + off, "Flower".equals(off) && "Dandelion".equals(names.get(yellow)));
		});
		scene("grass row");
		scene("kit");
		await("the kit in the hand", LIMIT, () -> carried(flowerItem) == 4);
		then(() -> {
			mc.player.inventory.selectedSlot = 2;
			mc.interactionManager.interactBlock(mc.player, mc.world, held(), x + 1, feet - 1, z + 1, 1);
		});
		await("the flower to stand on the client", LIMIT, () -> mc.world.getBlockId(x + 1, feet, z + 1) == id);
		wait(10);
		scene("standing");
		then(() -> mc.interactionManager.attackBlock(x + 1, feet, z + 1, 1));
		await("it to break on one hit", LIMIT, () -> mc.world.getBlockId(x + 1, feet, z + 1) == 0);
		wait(10);
		scene("broken");
		double[] home = new double[2];
		then(() -> {
			home[0] = mc.player.x;
			home[1] = mc.player.z;
		});
		// To wherever it landed: a broken block throws its drop sideways at a random speed, so a fixed walk sometimes
		// stopped out of reach.
		until("the player to pick it up", 200, () -> {
			if (carried(flowerItem) == 4) return true;
			ItemEntity e = drop(flowerItem);
			if (e != null) stepToward(e.x, e.z);
			return false;
		});
		wait(10);
		scene("carrying");
		await("back where it started", LIMIT, () -> stepToward(home[0], home[1]));
		wait(10);

		place(-1, 0);
		place(0, 1);
		place(1, 2);
		await("all three to stand", LIMIT, () -> mc.world.getBlockId(x + 1, feet, z + 2) == id && mc.world.getBlockId(x, feet, z + 2) != 0 && mc.world.getBlockId(x - 1, feet, z + 2) != 0);
		then(() -> {
			mc.player.yaw = 0;
			mc.player.pitch = 24;
		});
		wait(20);
		picture("row");
		scene("clear");
		if (remote()) return;
		then(() -> {
			if (FlowersTest.views.isEmpty()) note("no clear view of a patch, so no pictures of one");
			mc.world.setTime((mc.world.getTime() / 24000 + 1) * 24000 + 6000);
			mc.world.getProperties().setRaining(false);
		});
		// The views are known once the world test has looked at the land, so this step lays out their steps then.
		then(() -> {
			for (FlowersTest.View view : FlowersTest.views) {
				then(() -> {
					mc.options.hideHud = true;
					mc.player.setPosition(view.x, view.y, view.z);
					mc.player.yaw = view.yaw;
					mc.player.pitch = view.pitch;
				});
				wait(240);
				picture("patch-" + view.name);
			}
		});
		then(() -> {
			mc.options.hideHud = false;
			mc.player.setPosition(x + 0.5, feet + 1.62, z + 0.5);
		});
		wait(20);
	}
}
