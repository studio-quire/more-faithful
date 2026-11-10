package org.studioquire.faithful.wolfpacks;

import net.minecraft.entity.passive.WolfEntity;
import org.studioquire.faithful.heartyfood.HeartyFood;

/**
 * Hearty Food's healing for a tamed wolf, exactly as {@code HeartyFood} heals a player, its numbers read from it: only loaded when Hearty Food is. What's
 * pending never exceeds the wolf's missing health, held to that each tick; a hit doesn't touch it, so the end point falls with the hit, and death ends it.
 */
final class HeartyWolves {
	private HeartyWolves() {
	}

	static void eat(WolfEntity wolf, int restored) {
		if (wolf.health <= 0) return;
		if (HeartyFood.isWhole(restored)) {
			wolf.heal(restored);
			clear(wolf);
			return;
		}
		int due = WolfFood.pending.get(wolf);
		int added = Math.max(0, Math.min(HeartyFood.healing(restored), HeartyFood.FULL - wolf.health - due));
		if (added > 0) WolfFood.pending.set(wolf, due + added);
	}

	static void tick(WolfEntity wolf) {
		if (!WolfFood.pending.has(wolf)) return;
		if (wolf.health <= 0) {
			clear(wolf);
			return;
		}
		Hunt hunt = ((Hunt.Holder) wolf).wolfpacks$hunt();
		int due = Math.min(WolfFood.pending.get(wolf), HeartyFood.FULL - wolf.health);
		int time = hunt.waited + 1;
		if (due > 0 && time >= HeartyFood.PACE) {
			healOne(wolf);
			due--;
			time = 0;
		}
		if (due <= 0) {
			clear(wolf);
			return;
		}
		WolfFood.pending.set(wolf, due);
		hunt.waited = time;
	}

	/** As Hearty Food's: {@code heal} ends the half second of protection after a hit, which a wolf in a fight would pay for every second. */
	private static void healOne(WolfEntity wolf) {
		int protection = wolf.hearts;
		wolf.heal(1);
		if (protection > wolf.hearts) wolf.hearts = protection;
	}

	private static void clear(WolfEntity wolf) {
		WolfFood.pending.set(wolf, null);
		((Hunt.Holder) wolf).wolfpacks$hunt().waited = 0;
	}
}
