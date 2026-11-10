package org.studioquire.oldfaithful.core.api;

import java.util.function.IntUnaryOperator;

import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.DayClock;

/**
 * The length of the day, and the hour. Beta's world time is two things at once: a count of ticks, which scheduled block ticks, repeaters and saves run on, and the sun's clock. A day of another
 * length parts them. {@code world.getTime()} stays the count of ticks, so everything on it keeps its pace. {@link #time} is the sun's clock, and it's what the sky, darkness, beds and a real clock
 * go by, what {@code world.setTime} and {@code /time} set, and what a server sends its clients. In a world where nothing has ever changed the day's length, the two are one number.
 * <p>
 * A client with the same mods runs the sun smoothly at the mod's pace. One without runs it at beta's, so in a day of another length a server sends the time every tick rather than each second, and
 * such a client's sun goes by that.
 */
public final class Days {
	/** Ticks in beta's day, and the sun's clock always counts this many to a day, whatever the day's length. */
	public static final int NORMAL = 24000;

	private static final Listeners<IntUnaryOperator> LENGTH = new Listeners<>("Days.length");

	private Days() {
	}

	/**
	 * Filter, in order, each seeing what the last said: how many ticks a day takes, {@link #NORMAL} in beta. Asked often, on every side, a client on a server included, so read a rule or a field
	 * and return. The sun moves on from where it is when the answer changes. Anything under 1 is passed over.
	 */
	public static void length(IntUnaryOperator listener) {
		LENGTH.add(listener);
	}

	/** How many ticks a day takes here now. */
	public static int length() {
		if (LENGTH.isEmpty()) return NORMAL;
		int[] length = {NORMAL};
		LENGTH.each(l -> {
			int next = l.applyAsInt(length[0]);
			if (next > 0) length[0] = next;
		});
		return length[0];
	}

	/** The sun's clock, in beta's units whatever the day's length: {@code time % NORMAL} is the hour, 0 at sunrise, 6000 noon, 18000 midnight. On a client too, where it's what the server says. */
	public static long time(World world) {
		return DayClock.time(world.getProperties());
	}
}
