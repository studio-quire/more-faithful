package org.studioquire.faithful.towercheck.server;

/**
 * The server's ticks back to back while the driver has chunks to load or ticks to count, for a test that only waits on them ({@code -Dtowercheck.hurry=true}, which {@code :harness:buriedTowersTest}
 * sets). The game sees the same ticks in the same order, eight loads in each and forty to settle; only the 50 ms between them goes (16 s of a 48-chunk square's making, which takes 9 to 12 s
 * without it beside three other servers). Off unless asked, so
 * {@code tools/gen_bench.py} times loads at the game's own pace as it always has.
 * <p>
 * How: the server's loop runs a tick for each 50 ms its clock has moved, and {@link #clock} is that clock (the loop's alone, through {@code MinecraftServerPaceMixin}). While hurrying, every
 * reading is 50 ms after the last, so each pass of the loop runs exactly one tick; after it the clock goes on from there at the real rate, so nothing is owed and no tick is caught up.
 */
public final class Pace {
	private static final boolean ASKED = Boolean.getBoolean("towercheck.hurry");
	private static boolean hurrying;
	private static long last, ahead;

	private Pace() {
	}

	/** The driver says whether it's waiting on ticks; on the server's thread, as the loop is. */
	static void hurry(boolean waiting) {
		hurrying = ASKED && waiting;
	}

	public static long clock(long real) {
		if (hurrying) ahead = last + 50 - real;
		last = real + ahead;
		return last;
	}
}
