package org.studioquire.faithful.golems;

/**
 * An archer golem's arrow aimed to hit as often as a beta skeleton's standing in its place (the user, 2026-11-10: about beta's efficiency, from somewhere other than over the head). Still
 * Faithful's {@code SkeletonAim} maths, mirrored, since the library can't lean on that mod: a golem's arrow starts at its own eye, not 1.4 over a skeleton's head, so aimed beta's way it
 * climbs or drops more steeply than a skeleton's, and from a perch it hit about 9 points more often (66% against 57% at 4 down, `library/golems/tools/archer_arrows.py`). So it's aimed to cross the
 * target's distance at the height the skeleton's arrow would, unspread, and its spread is scaled by how much quicker or slower across it is, so it has spread as widely by then.
 */
final class ArrowAim {
	/** How far over the golem's start a skeleton standing there starts its arrow: its eye (1.8 * 0.85) less 0.1 and 1.4 up, against the golem's eye less 0.1. */
	static final double LIFT = 1.8F * 0.85F + 1.4F - GolemEntity.EYE;
	/** ArrowEntity's drag and fall a tick in air. */
	private static final double DRAG = 0.99, FALL = 0.03;
	private static final double STEEPEST = Math.toRadians(80.0);

	private ArrowAim() {
	}

	/** For beta's aim from the golem's start ({@code vy} up for {@code across} along), what to hand {@code setVelocity}: {vertical part, spread}. */
	static double[] aim(double across, double vy, double speed, double spread) {
		double betaVy = vy - LIFT;
		double want = LIFT + rise(across, betaVy, speed);
		if (across < 0.5 || Double.isInfinite(want) || Double.isNaN(want)) return new double[] {vy, spread};
		// Height at the target's distance has one peak over the angle: find it, then the lowest angle reaching the skeleton's height below it (the flat arc, as beta's is).
		double lo = -STEEPEST, hi = STEEPEST;
		for (int i = 0; i < 40; i++) {
			double a = lo + (hi - lo) / 3, b = hi - (hi - lo) / 3;
			if (riseAt(across, a, speed) < riseAt(across, b, speed)) lo = a;
			else hi = b;
		}
		double angle = (lo + hi) / 2;
		if (riseAt(across, angle, speed) > want) {
			lo = -STEEPEST;
			hi = angle;
			for (int i = 0; i < 40; i++) {
				double mid = (lo + hi) / 2;
				if (riseAt(across, mid, speed) < want) lo = mid;
				else hi = mid;
			}
			angle = (lo + hi) / 2;
		}
		double aimed = across * Math.tan(angle);
		// Time across goes as 1 / cos of the angle, and spread at the target as the time.
		return new double[] {aimed, spread * Math.hypot(across, betaVy) / Math.hypot(across, aimed)};
	}

	private static double riseAt(double across, double angle, double speed) {
		return rise(across, across * Math.tan(angle), speed);
	}

	/** How far above its start an unspread arrow handed (across, vy) is once it has gone {@code across}: the arrow's own steps run continuously. */
	private static double rise(double across, double vy, double speed) {
		double u = speed * across / Math.hypot(across, vy);
		double s = across / u;
		if (s * (1 - DRAG) >= 1) return Double.NEGATIVE_INFINITY;
		double n = Math.log(1 - (1 - DRAG) * s) / Math.log(DRAG);
		return vy - FALL * (n - s) / (1 - DRAG);
	}
}
