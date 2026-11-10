package org.studioquire.faithful.heartyfood.selftest;

import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.heartyfood.HeartyFood;
import org.studioquire.oldfaithful.core.api.Server;

/** The sums where the world runs, each food through its own {@code use} and cake through its own block. The client's side of it, real clicks and pictures, is {@code HeartyClientTest}. */
public final class HeartyTest extends WorldTest {
	/** The selftest server has no monsters, which makes it Peaceful, whose own healing would count in everything timed here. */
	private static final int TIMED = 1;

	private int peacefulBefore;
	/** Alchemy's ward taken off: its golden apple gives one as well as the heal, and the hits here are on a player with nothing to protect them. By reflection, as this mod doesn't depend on that one. */
	private Method clearAlchemyWard;

	/**
	 * One player goes through every mod's test, and the harness fills their bar as each begins, on both sides of the restart. A full bar ends what's pending on the next tick, as it should. So what's
	 * pending only reaches this test across a restart with no other mod's test in the run: one after this one takes it before the restart, and one before this one takes it after.
	 */
	public static boolean aloneInRun() {
		return TestHost.mods().size() == 1;
	}

	/** The player now: the one this test began with until they die. */
	private PlayerEntity me() {
		PlayerEntity now = Server.player(player.name);
		return now == null ? player : now;
	}

	private void reset(int health) {
		PlayerEntity p = me();
		HeartyFood.pending.set(p, null);
		HeartyFood.waited.set(p, null);
		HeartyFood.endsAt.set(p, null);
		p.health = health;
		p.hearts = 0;
		if (clearAlchemyWard != null) {
			try {
				clearAlchemyWard.invoke(null, p);
			} catch (ReflectiveOperationException e) {
				check("Alchemy's ward comes off: " + e, false);
			}
		}
	}

	private void eat(Item item) {
		PlayerEntity p = me();
		p.inventory.main[p.inventory.selectedSlot] = new ItemStack(item);
		ItemStack after = p.inventory.getSelectedItem().use(world, p);
		p.inventory.main[p.inventory.selectedSlot] = after.count == 0 ? null : after;
	}

	private int due() {
		return HeartyFood.pending.get(me());
	}

	private int end() {
		return HeartyFood.endsAt.get(me());
	}

	private void table() {
		Object[][] foods = {{Item.RAW_FISH, 3}, {Item.RAW_PORKCHOP, 5}, {Item.APPLE, 6}, {Item.BREAD, 8}, {Item.COOKED_FISH, 8}, {Item.COOKED_PORKCHOP, 12}, {Item.MUSHROOM_STEW, 15}, {Item.COOKIE, 2}};
		for (Object[] food : foods) {
			Item item = (Item) food[0];
			int amount = (Integer) food[1];
			reset(1);
			eat(item);
			check(item.getTranslationKey() + " is pending as " + amount + " half-hearts, and heals nothing at once", due() == amount && end() == 1 + amount && player.health == 1);
		}
		reset(1);
		eat(Item.MUSHROOM_STEW);
		check("stew leaves the bowl", player.inventory.getSelectedItem() != null && player.inventory.getSelectedItem().getItem() == Item.BOWL);
		reset(1);
		eat(Item.GOLDEN_APPLE);
		check("a golden apple heals everything at once", due() == 0 && !HeartyFood.pending.has(player) && player.health == 20);
		reset(20);
		eat(Item.BREAD);
		check("food at full health is wasted, as in beta", due() == 0 && !HeartyFood.pending.has(player) && player.inventory.getSelectedItem() == null);
		reset(17);
		eat(Item.COOKED_PORKCHOP);
		check("what's pending is capped at what's missing", due() == 3 && end() == 20);
		eat(Item.COOKED_PORKCHOP);
		check("and a second past that is wasted", due() == 3 && end() == 20);
		reset(4);
		eat(Item.BREAD);
		eat(Item.APPLE);
		check("more food adds to what's pending", due() == 14 && end() == 18);
		reset(4);
		eat(Item.BREAD);
		eat(Item.GOLDEN_APPLE);
		check("and a golden apple takes what's pending with it, leaving no ghost hearts", due() == 0 && !HeartyFood.endsAt.has(player) && player.health == 20);
	}

	private void cake() {
		set(1, 0, 0, Block.CAKE);
		reset(1);
		check("a slice of cake is 5 half-hearts", Block.CAKE.onUse(world, x + 1, y, z, player) && due() == 5 && player.health == 1 && meta(1, 0, 0) == 1);
		reset(1);
		Block.CAKE.onBlockBreakStart(world, x + 1, y, z, player);
		check("left-clicked too", due() == 5 && player.health == 1 && meta(1, 0, 0) == 2);
		reset(20);
		Block.CAKE.onUse(world, x + 1, y, z, player);
		check("at full health no slice goes, as in beta", meta(1, 0, 0) == 2 && due() == 0);
		reset(10);
		eat(Item.MUSHROOM_STEW);
		Block.CAKE.onUse(world, x + 1, y, z, player);
		check("nor with a full bar on its way", meta(1, 0, 0) == 2 && due() == 10);
		reset(19);
		Block.CAKE.onUse(world, x + 1, y, z, player);
		check("with one half-heart missing a slice still goes, and the rest of it is wasted", meta(1, 0, 0) == 3 && due() == 1);
		set(1, 0, 0, null);
	}

	private void saving() {
		reset(8);
		eat(Item.APPLE);
		NbtCompound nbt = new NbtCompound();
		player.write(nbt);
		HeartyFood.pending.set(player, null);
		player.read(nbt);
		check("what's pending is in the player's save", due() == 6);
		reset(20);
		nbt = new NbtCompound();
		player.write(nbt);
		check("and nothing is, with nothing pending", !nbt.getCompound("Faithful").getCompound("Attachments").contains("hearty-food:pending"));
	}

	/** Steps until the player's health is {@code health}, then a check on how long that took. {@code ticks} within one of a tick's order among the listeners. */
	private void healsAfter(String what, int health, int ticks) {
		int[] counted = {0};
		await(what, ticks + 10, () -> {
			counted[0]++;
			return me().health == health;
		});
		then(() -> check(what + ": " + counted[0] + " ticks", Math.abs(counted[0] - ticks) <= 1));
	}

	private void pace() {
		then(() -> {
			reset(10);
			eat(Item.BREAD);
		});
		healsAfter("a half-heart a second after eating", 11, HeartyFood.PACE);
		healsAfter("and another a second later", 12, HeartyFood.PACE);
		then(() -> check("with the end point where it was", due() == 6 && end() == 18));

		then(() -> {
			reset(4);
			eat(Item.BREAD);
			eat(Item.APPLE);
		});
		wait(42);
		then(() -> check("more food doesn't heal faster: " + player.health, player.health == 6));
	}

	private void hits() {
		then(() -> {
			reset(12);
			eat(Item.MUSHROOM_STEW);
			check("a hit lands", player.damage(null, 4) && player.health == 8);
			check("and doesn't touch what's pending", due() == 8);
		});
		until("so the end point falls with it", 3, () -> end() == 16);
		wait(22);
		then(() -> check("and the food goes on healing", player.health == 9 && due() == 7 && end() == 16));

		then(() -> {
			reset(10);
			eat(Item.BREAD);
		});
		await("nearly time for a half-heart", 25, () -> HeartyFood.waited.get(me()) >= HeartyFood.PACE - 3);
		then(() -> check("a hit just before a half-heart", player.damage(null, 2) && player.health == 8));
		await("the half-heart", 8, () -> player.health == 9);
		then(() -> {
			check("the half-heart doesn't end the half second of protection: " + player.hearts, player.hearts > player.maxHealth / 2);
			check("so a smaller blow still does nothing", !player.damage(null, 1) && player.health == 9);
		});
	}

	private void otherHealing() {
		then(() -> {
			reset(10);
			eat(Item.BREAD);
			player.heal(4);
		});
		until("healed from elsewhere, what's pending shrinks only if it would overfill the bar", 3, () -> due() == 6 && end() == 20 && player.health == 14);
		then(() -> {
			reset(10);
			eat(Item.BREAD);
			player.heal(1);
		});
		until("and with room, it stays, and the end point rises", 3, () -> due() == 8 && end() == 19 && player.health == 11);
		then(() -> {
			reset(19);
			eat(Item.COOKIE);
			player.heal(1);
		});
		until("filled from elsewhere, nothing's left", 3, () -> !HeartyFood.pending.has(me()) && !HeartyFood.endsAt.has(me()));
	}

	/** On a server: in single player the game sets the difficulty from the options every tick. */
	private void peaceful() {
		then(() -> {
			world.difficulty = 0;
			reset(10);
		});
		wait(45);
		then(() -> check("Peaceful's healing is beta's, half a heart a second with no food: " + player.health, player.health == 12 || player.health == 13));
		then(() -> {
			reset(10);
			eat(Item.BREAD);
		});
		wait(45);
		then(() -> {
			check("and food goes on beside it: " + player.health, player.health >= 13);
			world.difficulty = TIMED;
			reset(20);
		});
	}

	private void difficultyForTiming() {
		if (singlePlayer()) return;
		peacefulBefore = world.difficulty;
		world.difficulty = TIMED;
	}

	private void scenes() {
		scene("show", a -> {
			int health = Integer.parseInt(a[0]), end = Integer.parseInt(a[1]);
			reset(health);
			if (end > health) {
				HeartyFood.pending.set(me(), end - health);
				// Long enough for a picture, with the hearts holding still.
				HeartyFood.waited.set(me(), -100);
			}
		});
		scene("hold", a -> {
			PlayerEntity p = me();
			Item item = a[0].equals("bread") ? Item.BREAD : a[0].equals("stew") ? Item.MUSHROOM_STEW : null;
			p.inventory.main[p.inventory.selectedSlot] = item == null ? null : new ItemStack(item);
			p.currentScreenHandler.sendContentUpdates();
		});
		scene("is pending", a -> check("where the world runs: at least " + a[0] + " pending, ending at " + a[1] + " (" + due() + ", " + end() + ")", due() >= Integer.parseInt(a[0]) && end() == Integer.parseInt(a[1])));
		scene("cake", a -> set(1, 0, 0, Block.CAKE));
		scene("cake meta", a -> check("where the world runs: the cake's on " + a[0] + " (" + meta(1, 0, 0) + ")", meta(1, 0, 0) == Integer.parseInt(a[0])));
		scene("die", a -> {
			reset(10);
			eat(Item.BREAD);
			check("with food pending", due() == 8);
			me().damage(null, 1000);
		});
		scene("reborn", a -> check("after a respawn nothing's pending: " + me().health + ", " + due(), me() != player && me().health == 20 && !HeartyFood.pending.has(me()) && !HeartyFood.endsAt.has(me())));
		scene("leave", a -> {
			if (!singlePlayer()) world.difficulty = peacefulBefore;
			set(1, 0, 0, null);
			reset(1);
			eat(Item.MUSHROOM_STEW);
			eat(Item.APPLE);
			PlayerEntity p = me();
			p.inventory.main[p.inventory.selectedSlot] = null;
			p.currentScreenHandler.sendContentUpdates();
		});
		scene("over", a -> {
			if (!singlePlayer()) world.difficulty = peacefulBefore;
			reset(20);
		});
	}

	@Override
	protected void script() {
		if (FabricLoader.getInstance().isModLoaded("alchemy")) {
			try {
				clearAlchemyWard = Class.forName("org.studioquire.faithful.alchemy.Ward").getMethod("clear", PlayerEntity.class);
			} catch (ReflectiveOperationException e) {
				check("Alchemy has what this test reads: " + e, false);
			}
		}
		scenes();
		difficultyForTiming();
		if (restarted()) {
			// The harness has just filled the bar, but no tick has seen that yet.
			int left = due();
			check("no more pending after the restart than was left: " + left, left <= 19);
			if (aloneInRun()) {
				check("and food pending came through a restart: " + left, left > 0);
				player.health = HeartyFood.FULL - left;
			} else {
				note("another mod's test in this run filled the bar, so what's pending is gone by now");
			}
			return;
		}
		check("healing is half as much again, rounding up", HeartyFood.healing(1) == 2 && HeartyFood.healing(2) == 3 && HeartyFood.healing(3) == 5 && HeartyFood.healing(4) == 6 && HeartyFood.healing(5) == 8 && HeartyFood.healing(8) == 12 && HeartyFood.healing(10) == 15);
		table();
		cake();
		saving();
		pace();
		hits();
		otherHealing();
		if (!singlePlayer()) peaceful();
	}
}
