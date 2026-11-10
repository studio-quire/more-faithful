package org.studioquire.faithful.wolfpacks;

import net.minecraft.entity.Entity;

/** A tamed wolf's part in its pack's fight, from tick to tick (its mixin). Never saved: a fight doesn't outlast a save. */
public final class Hunt {
	public interface Holder {
		Hunt wolfpacks$hunt();
	}

	/** The enemy it was last seen holding, and when it was last within reach of it. */
	Entity enemy;
	long closeAt;
	/** One it gave up on, left alone until then. */
	Entity forgotten;
	long forgetUntil;
	/** Ticks toward Hearty Food's next half-heart, as a player's {@code hearty-food:waited}. */
	int waited;

	boolean forgets(Entity e, long now) {
		return e == forgotten && now < forgetUntil;
	}
}
