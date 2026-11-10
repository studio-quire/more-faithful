package org.studioquire.faithful.worldrules.selftest.client;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.worldrules.WorldRules;
import org.studioquire.oldfaithful.core.api.Days;
import org.studioquire.oldfaithful.core.impl.Rules;

/** What a client with the rule shows of it: the server's value, and a sun that moves on smoothly at its pace, between ticks too; a bow's wear; after the restart, the lock. */
public final class RulesClientTest extends ClientTest {
	/** The server's corrections may move it a clock tick or so; one of those is 1/24000 of a day, a little over that where the sun's curve is steepest. */
	private static final float MOST_BACK = 3.0F / Days.NORMAL;
	/** A tick's milliseconds: 50, or fewer in a harness run with faster game time ({@code -Ptest.speed}). */
	private static final double TICK_MS = Integer.getInteger("oldfaithful.test.tickMs", 50);

	private void sun(String what, int divisor, int n) {
		float[] last = {-1.0F}, back = {0.0F};
		long[] from = new long[1], began = new long[1];
		int[] seen = {0};
		await(what, n + 5, () -> {
			if (seen[0] == 0) {
				from[0] = Days.time(mc.world);
				began[0] = System.currentTimeMillis();
			}
			for (float delta = 0.0F; delta < 1.0F; delta += 0.25F) {
				float sun = mc.world.getTime(delta);
				if (last[0] >= 0.0F) back[0] = Math.max(back[0], last[0] - sun);
				last[0] = sun;
			}
			return ++seen[0] >= n;
		});
		then(() -> {
			long moved = Days.time(mc.world) - from[0], ms = System.currentTimeMillis() - began[0];
			// A client on a server hears the sun by the server's ticks, so if a long frame cost it ticks of its own, the clock follows the time gone by and not its count.
			boolean paced = Math.abs(moved - (double) n / divisor) <= 3 || Math.abs(moved - ms / TICK_MS / divisor) <= 3;
			check(what + ": in " + n + " ticks (" + ms + " ms) the sun's clock moved " + moved + ", and never back by more than " + back[0] * Days.NORMAL + " of a day's 24000", paced && back[0] <= MOST_BACK);
		});
	}

	private static final int LIMIT = 60;

	private ItemStack held() {
		return mc.player.inventory.getSelectedItem();
	}

	private int arrows() {
		int n = 0;
		for (ItemStack s : mc.player.inventory.main) {
			if (s != null && s.itemId == Item.ARROW.id) n += s.count;
		}
		return n;
	}

	private boolean holding(int damage, int arrows) {
		return held() != null && held().itemId == Item.BOW.id && held().getDamage() == damage && arrows() == arrows;
	}

	/**
	 * A right click as the hand makes it, looking level so the arrow flies off the plot (the world's side clears it away). The look is levelled in
	 * {@link #bows()} ticks before: on a server the click's packet goes out before this tick's look, and the world shoots along the look it last heard,
	 * which the mod before might have left at the ground, where the arrow sticks at the player's feet and is picked up again.
	 */
	private void shoot() {
		mc.player.pitch = 0.0F;
		mc.interactionManager.interactItem(mc.player, mc.world, held());
	}

	/**
	 * Bows Wear Out on a real right click: this client's bow wears at once, as the world's does, so the world's answer changes nothing (beta lowers and raises a
	 * held stack that comes back different); the last shot uses it up here and there; with the rule off, nothing to show or wear. The client's most damage is the
	 * rule as this world or its server says, so it changes as the rule does.
	 */
	private void bows() {
		// The bow's slot, picked here: a client on a server doesn't hear the world's side pick one. The look levelled now, so the world has it by the first shot.
		then(() -> {
			mc.player.inventory.selectedSlot = 0;
			mc.player.pitch = 0.0F;
		});
		scene("rule", "bows_wear_out", "true");
		scene("bow kit", "300");
		await("a worn bow in the hand, eight arrows, and the rule heard", LIMIT, () -> holding(300, 8) && WorldRules.bowsWearOut.get());
		then(() -> check("this client's bow has later versions' most damage, and shows its wear", Item.BOW.getMaxDamage() == WorldRules.BOW_MOST_DAMAGE && held().isDamaged()));
		picture("worn-bow");
		then(() -> {
			shoot();
			check("a shot wears it here at once, as it does where the world runs: " + (held() == null ? "no bow" : "at " + held().getDamage()), holding(301, 7));
		});
		wait(10);
		then(() -> check("and the world's answer leaves it so", holding(301, 7)));
		scene("bow held", "301", "7");
		scene("bow kit", "384");
		await("a bow on its last shot", LIMIT, () -> holding(384, 8));
		then(() -> {
			shoot();
			check("the last shot uses it up here at once", held() == null && arrows() == 7);
		});
		wait(10);
		then(() -> check("and it doesn't come back from the world", held() == null));
		scene("bow held", "none", "7");
		scene("rule", "bows_wear_out", "false");
		scene("bow kit", "300");
		await("the worn bow back, and the rule heard off", LIMIT, () -> holding(300, 8) && !WorldRules.bowsWearOut.get());
		then(() -> check("off: no most damage here, so the worn bow shows nothing", Item.BOW.getMaxDamage() == 0 && !held().isDamaged()));
		picture("worn-bow-off");
		then(() -> {
			shoot();
			check("off: a shot doesn't wear it", holding(300, 7));
		});
		wait(10);
		scene("bow held", "300", "7");
		scene("bows clear");
	}

	@Override
	protected void script() {
		if (restarted()) {
			wait(5);
			then(() -> check("back to normal: the client hears that too", WorldRules.dayLength.get().equals("normal") && Days.length() == Days.NORMAL));
			scene("lock");
			wait(10);
			then(() -> check("locked where the world runs: the client hears it", Rules.locked()));
			scene("unlock");
			wait(10);
			then(() -> check("and unlocked by the world's file: that too", !Rules.locked()));
			return;
		}
		// The world's apple test drops about five thousand items here in a tick and takes them away the same tick; a client hears of each coming and going,
		// and while it holds thousands, beta's items, each checking every other for collisions as it moves, slow its ticks to a crawl (10 to 15 s on a busy
		// PC). The clock it catches up afterwards jumps, so the timing waits for them to go: down to a handful near this plot (in single player the client's
		// world is the whole world, other tests' plots 96 blocks apart, and in a full run a few items always lie within reach here).
		await("the apple test's items gone from this client", 1200, () -> mc.world.entities.stream().filter(e -> e instanceof ItemEntity && ((ItemEntity) e).getDistance(mc.player) < 48).count() < 64);
		wait(5);
		then(() -> check("the client has the day's length and the weather: " + Days.length(), Days.length() == 2 * Days.NORMAL && mc.world.isRaining()));
		sun("double", 2, 100);
		picture("rain");
		scene("rule", "day_length", "triple");
		wait(25);
		sun("triple", 3, 100);
		scene("rule", "day_length", "double");
		bows();
	}
}
