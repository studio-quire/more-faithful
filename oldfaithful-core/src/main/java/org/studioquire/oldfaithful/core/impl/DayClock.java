package org.studioquire.oldfaithful.core.impl;

import net.minecraft.world.WorldProperties;
import org.studioquire.oldfaithful.core.api.Days;

/**
 * The sun's clock ({@link Days}), kept as how far it has drifted from the world's tick count. It lives on {@code WorldProperties} beside the time, so it goes wherever the time goes: through a
 * single player portal, into level.dat, and separately in a server's two worlds. Nobody changed the day: no drift, nothing saved, and every number here is the game's own. On a client on a
 * server the world's time already is the sun's clock, as the server sends it, so there's no drift there either.
 * <p>
 * At a day of {@code n} ticks the clock gains {@code NORMAL / n} each tick; {@code part} is the remainder, in units of {@code 1 / n}, which is what keeps the sun smooth between its whole steps.
 */
public final class DayClock {
	/** Implemented by the game's WorldProperties. */
	public interface Holder {
		long faithful$drift();

		void faithful$drift(long drift);

		int faithful$part();

		void faithful$part(int part);
	}

	private DayClock() {
	}

	public static long time(WorldProperties properties) {
		return properties.getTime() + ((Holder) properties).faithful$drift();
	}

	private static long gained(Holder clock, int length) {
		long part = clock.faithful$part() + (long) Days.NORMAL;
		clock.faithful$part((int) (part % length));
		return part / length;
	}

	/** Where the world runs: the time has just gone on by one. */
	public static void ticked(WorldProperties properties) {
		Holder clock = (Holder) properties;
		int length = Days.length();
		if (length == Days.NORMAL) clock.faithful$part(0);
		else clock.faithful$drift(clock.faithful$drift() + gained(clock, length) - 1);
	}

	/** On a client on a server: what the time goes on to from {@code next}, beta's one tick on. */
	public static long clientTicked(WorldProperties properties, long next) {
		Holder clock = (Holder) properties;
		int length = Days.length();
		if (length == Days.NORMAL) {
			clock.faithful$part(0);
			return next;
		}
		return properties.getTime() + gained(clock, length);
	}

	/** The time was set: it's the hour that's meant, so the clock reads that. */
	public static void set(WorldProperties properties) {
		((Holder) properties).faithful$drift(0);
	}

	/** Everyone slept: beta has moved the time to a sunrise of its own count, and the clock goes to the next of its. */
	public static void nightSkipped(WorldProperties properties, long clockBefore) {
		Holder clock = (Holder) properties;
		long sunrise = clockBefore + Days.NORMAL;
		clock.faithful$drift(sunrise - sunrise % Days.NORMAL - properties.getTime());
		clock.faithful$part(0);
	}

	/** The fraction of a clock tick a frame is into, from its fraction of a game tick. */
	public static float delta(WorldProperties properties, float tickDelta) {
		int part = ((Holder) properties).faithful$part();
		int length = Days.length();
		if (length == Days.NORMAL && part == 0) return tickDelta;
		return (part + tickDelta * Days.NORMAL) / length;
	}
}
