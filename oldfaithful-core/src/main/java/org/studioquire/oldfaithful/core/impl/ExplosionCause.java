package org.studioquire.oldfaithful.core.impl;

import org.studioquire.oldfaithful.core.api.Explosions;

/**
 * What's causing the explosion being made: ghast fireballs, TNT and beds all pass a null source, so their call sites say.
 * Explosions run on the game thread, one at a time.
 */
public final class ExplosionCause {
	private static Explosions.Cause current = Explosions.Cause.OTHER;

	private ExplosionCause() {
	}

	public static Explosions.Cause current() {
		return current;
	}

	public static void set(Explosions.Cause cause) {
		current = cause;
	}
}
