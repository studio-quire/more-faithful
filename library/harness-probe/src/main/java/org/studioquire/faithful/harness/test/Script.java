package org.studioquire.faithful.harness.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Steps run in order: what a test on a running game is. They run back to back in one tick until one holds (a {@code wait}, or an {@code until} or {@code await} whose condition
 * isn't met yet), so the game moves on between two steps only where one of those stands between them. {@code script()} runs first, at once: plain code in it happens then, and what has to wait for the game goes in steps.
 * A step that adds steps has them run next. A step that throws fails and ends the script, since what comes after would stand on it.
 */
public abstract class Script {
	private interface Step {
		/** {@code ticks}: since the step was reached. True when it's over. */
		boolean run(int ticks);
	}

	private final List<Step> steps = new ArrayList<>();
	private int next, insertAt = -1, ticks;
	private Checks checks;

	protected abstract void script();

	/** The harness's. */
	public final void begin(Checks checks) {
		this.checks = checks;
		steps.clear();
		next = 0;
		ticks = 0;
		run(this::script);
	}

	/** The harness's: one tick. True when the script is over. */
	public final boolean advance() {
		ticks++;
		while (next < steps.size()) {
			Step step = steps.get(next);
			insertAt = next + 1;
			boolean over;
			try {
				over = step.run(ticks);
			} catch (RuntimeException | LinkageError e) {
				checks.fail("a step threw", e);
				next = steps.size();
				return true;
			} finally {
				insertAt = -1;
			}
			if (!over) return false;
			next++;
			ticks = 0;
		}
		return true;
	}

	/** Runs code that may add steps, now: they go in next, ahead of what's waiting. */
	protected final void run(Runnable code) {
		int was = insertAt;
		if (was < 0) insertAt = next;
		try {
			code.run();
		} catch (RuntimeException | LinkageError e) {
			checks.fail("the script threw", e);
		} finally {
			insertAt = was;
		}
	}

	private void add(Step step) {
		if (insertAt < 0) steps.add(step);
		else steps.add(insertAt++, step);
	}

	/** A step: code run once, when the steps before it are over. */
	protected final void then(Runnable step) {
		add(ticks -> {
			step.run();
			return true;
		});
	}

	/** Lets the game run {@code ticks} ticks. */
	protected final void wait(int ticks) {
		add(t -> t >= ticks);
	}

	/** A step that passes once {@code ok} holds, and fails if it hasn't in {@code limit} ticks. Steps after it run either way. */
	protected final void until(String what, int limit, BooleanSupplier ok) {
		add(t -> {
			if (ok.getAsBoolean()) {
				pass(what + " (in " + t + " ticks)");
				return true;
			}
			if (t < limit) return false;
			fail(what + " (not in " + limit + " ticks)");
			return true;
		});
	}

	/** As {@link #until}, not counting as a check unless it fails: for waiting on what the test needs, not what it tests. */
	protected final void await(String what, int limit, BooleanSupplier ok) {
		add(t -> {
			if (ok.getAsBoolean()) return true;
			if (t < limit) return false;
			fail(what + " (not in " + limit + " ticks)");
			return true;
		});
	}

	/** A check, now: inside a step, or in plain code before the first wait. */
	protected final boolean check(String what, boolean ok) {
		return checks.that(what, ok);
	}

	protected final void pass(String what) {
		checks.pass(what);
	}

	protected final void fail(String what) {
		checks.fail(what);
	}

	protected final void note(String what) {
		checks.note(what);
	}

	protected final Checks checks() {
		return checks;
	}
}
