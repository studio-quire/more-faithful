package org.studioquire.faithful.buriedtowers.plan;

/** The land under a site, as beta's terrain makes it before caves ({@link #top}) and after them ({@link #surface}). */
public interface Ground {
	/** Beta's sea: land lower than this lies under water. */
	int SEA = 64;

	/** The lowest cell above the land in a column, not counting water as land. */
	int top(int x, int z);

	/**
	 * The same once beta's caves are cut: lower than {@link #top} where a cave has opened the land from above, which is the ground a player sees there. For what's laid on the land and has to rest on it,
	 * like the gate's path and fallen stones; the tower's shape keeps to {@link #top}.
	 */
	default int surface(int x, int z) {
		return top(x, z);
	}
}
