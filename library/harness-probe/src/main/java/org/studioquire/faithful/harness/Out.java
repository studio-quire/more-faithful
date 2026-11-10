package org.studioquire.faithful.harness;

/** What the harness task reads from the game's output: a failure names the mod and the rule, so whoever reads it can act alone. */
public final class Out {
	private static final String TAG = "[library-harness] ";

	private Out() {
	}

	/** {@code mod}: the mod id the failure belongs to. {@code rule}: short, stable, the same words in the docs. */
	public static void fail(String mod, String rule, String detail) {
		System.out.println(TAG + "FAIL " + mod + " | " + rule + " | " + detail);
	}

	/** Worth listing, not a failure. */
	public static void note(String mod, String rule, String detail) {
		System.out.println(TAG + "NOTE " + mod + " | " + rule + " | " + detail);
	}

	public static void info(String line) {
		System.out.println(TAG + line);
	}

	public static void done(String scene) {
		System.out.println(TAG + "DONE " + scene);
	}
}
