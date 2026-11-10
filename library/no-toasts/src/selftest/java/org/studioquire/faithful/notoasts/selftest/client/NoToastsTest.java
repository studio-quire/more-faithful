package org.studioquire.faithful.notoasts.selftest.client;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.achievement.Achievement;
import net.minecraft.achievement.Achievements;
import net.minecraft.client.gui.hud.toast.AchievementToast;
import net.minecraft.stat.PlayerStats;
import org.studioquire.faithful.harness.test.client.ClientTest;

/** What the toast draws from is what it's been told, and the game tells it the tutorial every tick until the inventory has been opened. */
public final class NoToastsTest extends ClientTest {
	/** Its one field of that type, found by type since a running game's names aren't the source's. */
	private Achievement told() {
		try {
			for (Field f : AchievementToast.class.getDeclaredFields()) {
				if (f.getType() == Achievement.class) {
					f.setAccessible(true);
					return (Achievement) f.get(mc.toast);
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		throw new IllegalStateException("no achievement field on the toast");
	}

	/** One player goes through every mod's test, and an earlier one may have opened the inventory: taken back, so the tutorial is due here whichever ran first. */
	private void forgetTheInventory() {
		try {
			for (Field f : PlayerStats.class.getDeclaredFields()) {
				if (f.getType() == Map.class) {
					f.setAccessible(true);
					((Map<?, ?>) f.get(mc.stats)).remove(Achievements.OPEN_INVENTORY);
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void script() {
		if (restarted()) {
			check("an achievement earned is still there after a restart", mc.stats.hasAchievement(Achievements.OPEN_INVENTORY));
			return;
		}
		forgetTheInventory();
		check("a fresh install: the inventory hasn't been opened, so the tutorial is due", !mc.stats.hasAchievement(Achievements.OPEN_INVENTORY));
		wait(40);
		then(() -> check("the tutorial was told to nobody in two seconds of play", told() == null));
		picture("no-tutorial");
		then(() -> {
			mc.player.increaseStat(Achievements.OPEN_INVENTORY, 1);
			check("an achievement is earned", mc.stats.hasAchievement(Achievements.OPEN_INVENTORY));
			check("and no toast is told of it", told() == null);
		});
		picture("no-achievement");
	}
}
