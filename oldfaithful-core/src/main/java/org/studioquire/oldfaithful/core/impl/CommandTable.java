package org.studioquire.oldfaithful.core.impl;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Listeners;

/** The commands mods added, looked up ahead of the server's own. */
public final class CommandTable {
	private static final class Entry {
		final String mod = Core.currentMod();
		final String help;
		final boolean everyone;
		final Listeners<Commands.Handler> handler;

		Entry(String name, String help, boolean everyone, Commands.Handler handler) {
			this.help = help;
			this.everyone = everyone;
			this.handler = new Listeners<>("the command " + name);
			this.handler.add(handler);
		}

		void run(String[] words, Commands.Reply reply) {
			handler.each(h -> {
				try {
					h.run(Arrays.copyOfRange(words, 1, words.length), reply);
				} catch (RuntimeException e) {
					reply.say("That command failed: " + e);
					throw e;
				}
			});
		}
	}

	private static final Map<String, Entry> COMMANDS = new TreeMap<>();

	private CommandTable() {
	}

	public static void add(String name, String help, boolean everyone, Commands.Handler handler) {
		if (COMMANDS.containsKey(name)) throw new IllegalStateException("The server command " + name + " is registered twice, first by " + COMMANDS.get(name).mod);
		COMMANDS.put(name, new Entry(name, help, everyone, handler));
	}

	private static String[] words(String line) {
		return line.trim().split("\\s+");
	}

	/** Runs the line if it's one of ours: from the console or an op. */
	public static boolean run(String line, Commands.Reply reply) {
		String[] words = words(line);
		Entry entry = COMMANDS.get(words[0].toLowerCase(Locale.ROOT));
		if (entry == null) return false;
		entry.run(words, reply);
		return true;
	}

	/** Runs the line if anyone may. */
	public static boolean runForAnyone(String line, Commands.Reply reply) {
		String[] words = words(line);
		Entry entry = COMMANDS.get(words[0].toLowerCase(Locale.ROOT));
		if (entry == null || !entry.everyone) return false;
		entry.run(words, reply);
		return true;
	}

	/** For the harness. */
	public static java.util.Set<String> names() {
		return COMMANDS.keySet();
	}

	public static String mod(String name) {
		return COMMANDS.get(name).mod;
	}

	public static void help(Commands.Reply reply) {
		for (Entry entry : COMMANDS.values()) reply.say("   " + entry.help);
	}
}
