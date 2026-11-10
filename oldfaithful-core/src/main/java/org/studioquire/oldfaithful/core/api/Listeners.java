package org.studioquire.oldfaithful.core.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.studioquire.oldfaithful.core.impl.Core;

/**
 * The listeners of one hook, kept in a stable order (by mod id, then as added). Each runs in its own try/catch: a throw is
 * logged with the mod and the hook, the event goes on as if that listener hadn't spoken, and after five throws in a
 * session the listener is turned off and says so. One mod's fault doesn't take down a server.
 */
public final class Listeners<T> {
	private static final int MAX_THROWS = 5;

	private static final class Entry<T> {
		final String mod;
		final T listener;
		int throwsSoFar;

		Entry(String mod, T listener) {
			this.mod = mod;
			this.listener = listener;
		}
	}

	private static final List<Listeners<?>> ALL = new ArrayList<>();

	private final String hook;
	private final List<Entry<T>> entries = new ArrayList<>();
	private String speaking;

	public Listeners(String hook) {
		this.hook = hook;
		synchronized (ALL) {
			ALL.add(this);
		}
	}

	/** For the harness: each hook with mods on it, a line each: its name, a tab, the mods. */
	public static List<String> describeAll() {
		List<String> lines = new ArrayList<>();
		synchronized (ALL) {
			for (Listeners<?> l : ALL) {
				java.util.Set<String> mods = new java.util.TreeSet<>();
				for (Entry<?> e : l.entries) mods.add(e.mod);
				if (!mods.isEmpty()) lines.add(l.hook + "\t" + String.join(",", mods));
			}
		}
		return lines;
	}

	public synchronized void add(T listener) {
		String mod = Core.currentMod();
		int at = entries.size();
		while (at > 0 && Core.orderKey(entries.get(at - 1).mod).compareTo(Core.orderKey(mod)) > 0) at--;
		entries.add(at, new Entry<>(mod, listener));
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/** While a listener runs, its mod: for a hook whose rule goes by mod id, as the order listeners run in can be salted. */
	public String speaking() {
		return speaking;
	}

	public void each(Consumer<? super T> call) {
		for (int i = 0; i < entries.size(); i++) {
			Entry<T> e = entries.get(i);
			if (e.throwsSoFar >= MAX_THROWS) continue;
			speaking = e.mod;
			try {
				call.accept(e.listener);
			} catch (RuntimeException | LinkageError | StackOverflowError t) {
				e.throwsSoFar++;
				System.out.println("[faithful-core] WARN " + e.mod + "'s listener on " + hook + " threw, and was skipped: " + t);
				t.printStackTrace(System.out);
				if (e.throwsSoFar == MAX_THROWS) {
					System.out.println("[faithful-core] WARN " + e.mod + "'s listener on " + hook + " is turned off for this session");
				}
			}
		}
	}
}
