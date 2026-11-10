package org.studioquire.faithful.heartyfood.selftest.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.minecraft.client.util.ScreenScaler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.heartyfood.HeartyFood;
import org.studioquire.faithful.heartyfood.selftest.HeartyTest;

/** A client's real clicks, where a server has the last word; what it hears; and the ghost hearts in pictures, read back half-heart by half-heart. */
public final class HeartyClientTest extends ClientTest {
	private static final int LIMIT = 40;

	private int x, feet, z;

	private int ends() {
		return HeartyFood.endsAt.get(mc.player);
	}

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	/** What's in a half-heart's middle: R red, G a ghost, . empty, ? anything else. */
	private static char reading(int rgb) {
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255;
		if (r < 70) return '.';
		if (r > 230 && g < 60) return 'R';
		if (r > 130 && r <= 230 && g >= 70 && g < 170) return 'G';
		return '?';
	}

	/** Every half-heart in a picture, by where beta draws its slot: columns 3 and 6 of a heart, in a row that's red in a full heart whichever way it has jittered. */
	private String read(String name) {
		BufferedImage image;
		try {
			image = ImageIO.read(new File("shots/" + checks().mod() + "/" + name + ".png"));
		} catch (IOException e) {
			return "unreadable: " + e;
		}
		ScreenScaler screen = new ScreenScaler(mc.options, mc.displayWidth, mc.displayHeight);
		int k = screen.scaleFactor, left = screen.getScaledWidth() / 2 - 91, row = screen.getScaledHeight() - 32 + 3;
		StringBuilder hearts = new StringBuilder();
		for (int half = 0; half < 20; half++) {
			int gx = left + half / 2 * 8 + (half % 2 == 0 ? 3 : 6);
			hearts.append(reading(image.getRGB(gx * k + k / 2, row * k + k / 2)));
		}
		return hearts.toString();
	}

	private static String expected(int health, int end) {
		StringBuilder hearts = new StringBuilder();
		for (int half = 1; half <= 20; half++) hearts.append(half <= health ? 'R' : half <= end ? 'G' : '.');
		return hearts.toString();
	}

	private void hearts(String name, int health, int end) {
		scene("show", health, end);
		await("the hearts as asked, no longer flashing", LIMIT, () -> mc.player.health == health && ends() == (end > health ? end : 0) && mc.player.hearts < 10);
		picture(name);
		then(() -> {
			String got = read(name);
			check(name + ": " + got, got.equals(expected(health, end)));
		});
	}

	private void pictures() {
		hearts("full", 20, 20);
		hearts("bread-at-10", 10, 18);
		hearts("half-to-half", 7, 13);
		hearts("half-to-whole", 7, 14);
		hearts("whole-to-half", 6, 13);
		hearts("half-more-on-a-half", 7, 8);
		hearts("half-more-on-a-whole", 6, 7);
		hearts("last-half", 19, 20);
		hearts("low-jitter", 3, 18);
		wait(2);
		picture("low-jitter-next");
		then(() -> {
			String got = read("low-jitter-next");
			check("the next tick too, as they jitter: " + got, got.equals(expected(3, 18)));
		});
		hearts("nearly-gone", 1, 20);

		// A hit's flash: the outlines turn white and what was lost shows in the flash's own hearts, for three ticks in six. These frames are for looking at.
		hearts("before-flash", 10, 18);
		then(() -> {
			mc.player.lastHealth = 16;
			mc.player.hearts = 17;
		});
		picture("flash");

		int[] was = {0};
		then(() -> was[0] = mc.options.guiScale);
		for (int scale : new int[] {1, 2, 3, 0}) {
			then(() -> mc.options.guiScale = scale);
			wait(2);
			hearts("gui-scale-" + scale, 7, 13);
		}
		then(() -> mc.options.guiScale = was[0]);
	}

	private void clicks() {
		scene("show", 10, 10);
		scene("hold", "bread");
		await("the bread in the hand", LIMIT, () -> held() != null && held().getItem() == Item.BREAD);
		then(() -> {
			mc.interactionManager.interactItem(mc.player, mc.world, held());
			check("the bread goes at once", held() == null || held().count == 0);
		});
		until("the client hears where it'll end up", LIMIT, () -> ends() == 18);
		scene("is pending", 6, 18);
		then(() -> check("having healed little yet: " + mc.player.health, mc.player.health <= 11));
		until("and the hearts fill", LIMIT, () -> mc.player.health >= 11);

		scene("show", 5, 5);
		scene("hold", "stew");
		await("the stew in the hand", LIMIT, () -> held() != null && held().getItem() == Item.MUSHROOM_STEW);
		then(() -> {
			mc.interactionManager.interactItem(mc.player, mc.world, held());
		});
		until("stew leaves the bowl", LIMIT, () -> held() != null && held().getItem() == Item.BOWL);
		until("and ends where it should", LIMIT, () -> ends() == 20);

		scene("cake");
		scene("show", 10, 10);
		scene("hold", "nothing");
		await("the cake and an empty hand", LIMIT, () -> mc.world.getBlockId(x + 1, feet, z) != 0 && held() == null);
		then(() -> {
			mc.interactionManager.interactBlock(mc.player, mc.world, null, x + 1, feet, z, 1);
		});
		until("a slice of cake by a real click", LIMIT, () -> ends() == 15);
		then(() -> check("the client's cake is a slice down", mc.world.getBlockMeta(x + 1, feet, z) == 1));
		scene("cake meta", 1);

		scene("show", 12, 20);
		await("a full bar on its way", LIMIT, () -> ends() == 20);
		then(() -> mc.interactionManager.interactBlock(mc.player, mc.world, null, x + 1, feet, z, 1));
		wait(10);
		then(() -> check("then the cake stays as it is", mc.world.getBlockMeta(x + 1, feet, z) == 1));
		scene("cake meta", 1);
	}

	private void death() {
		scene("die");
		await("the client to die", LIMIT, () -> mc.player.health <= 0);
		until("and its ghost hearts go with it", LIMIT, () -> ends() == 0);
		then(() -> {
			mc.setScreen(null);
			mc.player.respawn();
		});
		await("the new player", LIMIT * 2, () -> mc.player.health > 0);
		wait(20);
		then(() -> check("none on the new player: " + ends(), ends() == 0 && !HeartyFood.endsAt.has(mc.player)));
		scene("reborn");
	}

	@Override
	protected void script() {
		if (restarted()) {
			if (!HeartyTest.aloneInRun()) return;
			until("after a restart the client has its ghost hearts back: " + mc.player.health, LIMIT, () -> ends() == HeartyFood.FULL);
			scene("over");
			return;
		}
		then(() -> {
			x = MathHelper.floor(mc.player.x);
			feet = MathHelper.floor(mc.player.boundingBox.minY);
			z = MathHelper.floor(mc.player.z);
			mc.setScreen(null);
		});
		clicks();
		pictures();
		death();
		scene("leave");
	}
}
