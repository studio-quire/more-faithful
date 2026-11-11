package org.studioquire.faithful.wolfpacks;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;

/** A tamed wolf's part in its pack's fight and its watch, from tick to tick (its mixin). Never saved, but for the spot a wolf on watch left ({@link Watch#post}). */
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

	/** On watch and off its seat: the spot it goes back to, feet at x, y, z. Null on its seat and for a wolf not on watch. Read from the save once. */
	double[] post;
	boolean postRead;
	/** The spot it last came back to and where it came to rest there: unmoved since, it leaves from the spot itself, so many returns don't walk it across the floor. */
	double[] seat;
	double restX, restZ;
	/** Monsters it sees and found no way to, each left alone until its tick. */
	Map<Entity, Long> noWay;
	/** Going back: the nearest it has got, when it last got nearer, its path there and when it may next ask for one. */
	double homeBest = Double.MAX_VALUE;
	long homeBestAt, pathAt;
	Path homePath;
	/** It has just sat down by itself, and stops any jump it was in, as beta does for the owner's click. */
	boolean settling;
	/** What it's growling at and until when, when it last growled, and when it next may. */
	Entity alarm;
	long alarmedUntil = -1, growled = Long.MIN_VALUE / 2, growlAt;

	boolean forgets(Entity e, long now) {
		return e == forgotten && now < forgetUntil;
	}

	Map<Entity, Long> noWay() {
		if (noWay == null) noWay = new WeakHashMap<>();
		return noWay;
	}
}
