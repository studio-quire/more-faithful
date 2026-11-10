package org.studioquire.faithful.harness.test;

/**
 * What a mod's test says, on either side: the one reporting call the harness reads. A {@link Script} has these built in; anything else (a server command of a test's own) keeps one of
 * these. A failure fails the run under the mod's name, and a mod with a test that reports no checks fails too. Printing PASS or FAIL any other way counts for nothing.
 */
public final class Checks {
	private static final String TAG = "[faithful-test] ";

	private final String mod;

	private Checks(String mod) {
		this.mod = mod;
	}

	/** {@code mod}: the id of the mod under test, not of its selftest. */
	public static Checks of(String mod) {
		return new Checks(mod);
	}

	public String mod() {
		return mod;
	}

	/** Passes or fails; returns {@code ok}, so a test can go on from it. */
	public boolean that(String what, boolean ok) {
		if (ok) pass(what);
		else fail(what);
		return ok;
	}

	public void pass(String what) {
		System.out.println(TAG + "PASS " + mod + " | " + what);
	}

	public void fail(String what) {
		System.out.println(TAG + "FAIL " + mod + " | " + what);
	}

	/** With where it was thrown, on lines the harness won't count a second time as a stack trace of ours. */
	public void fail(String what, Throwable t) {
		fail(what + ": " + t);
		StackTraceElement[] frames = t.getStackTrace();
		for (int i = 0; i < Math.min(frames.length, 12); i++) System.out.println(TAG + "  " + frames[i]);
		if (t.getCause() != null) System.out.println(TAG + "  from " + t.getCause());
	}

	/** Worth seeing in the run's report: a measurement, say. Not a check. */
	public void note(String what) {
		System.out.println(TAG + "NOTE " + mod + " | " + what);
	}
}
