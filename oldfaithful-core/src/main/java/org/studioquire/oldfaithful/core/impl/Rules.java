package org.studioquire.oldfaithful.core.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.ChoiceRule;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Rule;

/**
 * World rules: declared by mods, valued per world. A rule declared {@link Rule#atCreation at creation} is set only while there's no world yet, as
 * a new world's starting value or in its file before it first opens, and is never changed inside one. Whoever runs the world owns them: a player in single player, the file or
 * {@code /rule} on a server; a client on a server only hears the server's values. The file holds only what was changed from a
 * rule's default, and a world nobody touched has no file. The install keeps one thing: what a new world starts with.
 * <p>
 * A world's rules can be locked, every mod's at once: then nothing in the game changes them, {@code /rule} included, and nothing in the
 * game unlocks them. The lock is a plain line in the world's file, so taking it out with the world closed is the way back, and
 * nothing here notices or minds.
 */
public final class Rules {
	private static final int MAX_RULES = 4096;
	private static final String DEFAULTS_FILE = "faithful-rules.properties";
	/** The world file's line. */
	public static final String LOCK_KEY = "rules-locked";
	/** Sent among the values, where no rule's id can be, since every one has a colon; a core that doesn't know it keeps it unread. */
	private static final String LOCKED_ON_WIRE = "locked";

	private abstract static class Declared implements Rule {
		final String id;
		final List<String> values;
		final String defaultText;
		final boolean creation;

		Declared(String id, List<String> values, String defaultText, boolean creation) {
			this.id = id;
			this.values = values;
			this.defaultText = defaultText;
			this.creation = creation;
		}

		@Override
		public boolean atCreation() {
			return creation;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public List<String> values() {
			return values;
		}

		@Override
		public String defaultText() {
			return defaultText;
		}

		/** A value as read from a file or heard from a server: one of {@link #values}. */
		abstract String read(String text);

		final String text() {
			String v = current(id);
			return v == null ? defaultText : read(v);
		}
	}

	private static final class Flag extends Declared implements BoolRule {
		Flag(String id, boolean defaultValue, boolean creation) {
			super(id, Arrays.asList("true", "false"), String.valueOf(defaultValue), creation);
		}

		@Override
		public boolean defaultValue() {
			return Boolean.parseBoolean(defaultText);
		}

		@Override
		public boolean get() {
			return Boolean.parseBoolean(text());
		}

		@Override
		String read(String text) {
			return String.valueOf(Boolean.parseBoolean(text));
		}
	}

	private static final class Choice extends Declared implements ChoiceRule {
		Choice(String id, List<String> values, boolean creation) {
			super(id, values, values.get(0), creation);
		}

		@Override
		public String get() {
			return text();
		}

		/** A value it doesn't know, from an edited file or a newer server's mod, is beta's. */
		@Override
		String read(String text) {
			for (String v : values) {
				if (v.equalsIgnoreCase(text)) return v;
			}
			return defaultText;
		}
	}

	private static final Map<String, Declared> RULES = new LinkedHashMap<>();
	private static Channel channel;
	private static WorldFile world;
	/** Values while a world is open here. */
	private static final Map<String, String> worldValues = new HashMap<>();
	/** Values heard from the server, on a client on one; null when not on a server. */
	private static Map<String, String> serverValues;
	/** What a new world starts with, this install's. */
	private static WorldFile newWorld;
	/** The open world's lock, or what the server said of its own. */
	private static boolean locked;

	private Rules() {
	}

	static String key(String id) {
		return "rule." + id.replace(':', '.');
	}

	static BoolRule declare(String id, boolean creation, boolean defaultValue) {
		return add(new Flag(id, defaultValue, creation));
	}

	static ChoiceRule declare(String id, boolean creation, String... values) {
		if (values.length < 2) throw new IllegalArgumentException("The rule " + id + " has nothing to choose between");
		return add(new Choice(id, Arrays.asList(values.clone()), creation));
	}

	private static <R extends Declared> R add(R rule) {
		if (RULES.putIfAbsent(rule.id, rule) != null) throw new IllegalStateException("The rule " + rule.id + " is declared twice");
		return rule;
	}

	/** The rules, in the order declared. */
	public static Collection<? extends Rule> all() {
		return RULES.values();
	}

	static void start() {
		newWorld = new WorldFile(FabricLoader.getInstance().getConfigDir().resolve(DEFAULTS_FILE));
		channel = CoreNet.channel(Core.ID + ":rules", 1).onClient(Rules::heard);
		Commands.register("rule", "rule [name] [value] | lock  shows, sets or locks this world's rules", Rules::command);
	}

	private static String current(String id) {
		if (serverValues != null) return serverValues.get(id);
		return worldValues.get(id);
	}

	/** A world opens here, in single player or on a server. A new one starts from this install's choices. */
	public static void open(Path dir) {
		boolean fresh = !java.nio.file.Files.exists(dir.resolve("level.dat"));
		SeenMods.note(dir);
		world = new WorldFile(dir.resolve(WorldFile.NAME));
		worldValues.clear();
		serverValues = null;
		Properties p = world.values();
		locked = Boolean.parseBoolean(p.getProperty(LOCK_KEY, "false").trim());
		for (Declared r : RULES.values()) {
			String v = p.getProperty(key(r.id));
			if (v != null) worldValues.put(r.id, v.trim());
		}
		if (fresh && worldValues.isEmpty() && !locked) {
			Properties defaults = newWorld.values();
			for (Declared r : RULES.values()) {
				String v = defaults.getProperty(key(r.id));
				if (v != null && !r.read(v.trim()).equals(r.defaultText)) set(r.id, r.read(v.trim()));
			}
		}
		for (Map.Entry<String, String> e : worldValues.entrySet()) System.out.println("[faithful-core] Rule " + e.getKey() + " = " + e.getValue());
		if (locked) System.out.println("[faithful-core] This world's rules are locked");
	}

	/** On a server, before anything of the world loads. */
	public static void openOnDedicated(Path dir) {
		SeenMods.checkServer(dir);
		open(dir);
	}

	/** The world closes, or the client left a server. */
	public static void close() {
		world = null;
		worldValues.clear();
		serverValues = null;
		locked = false;
	}

	/** On a client: a server's world starts; nothing is known of its rules until it says. */
	public static void openOnServer() {
		world = null;
		worldValues.clear();
		serverValues = new HashMap<>();
		locked = false;
	}

	public static boolean onServer() {
		return serverValues != null;
	}

	/** Whether a world is open here, whose rules can be changed. */
	public static boolean worldOpen() {
		return world != null;
	}

	/** Whether the rules here can't be changed: this world's, locked, or the server's, which it said it locked. */
	public static boolean locked() {
		return locked;
	}

	/**
	 * Locks the open world's rules, for good as far as the game goes: written to its file first, so a lock that couldn't be saved
	 * isn't one. False if there's no world open here, or it couldn't be saved.
	 */
	public static boolean lock() {
		if (world == null) return false;
		if (locked) return true;
		if (!world.set(LOCK_KEY, "true")) return false;
		locked = true;
		System.out.println("[faithful-core] This world's rules are now locked");
		if (channel != null) sendTo(null);
		return true;
	}

	/** Whether this server told us its rules. */
	public static boolean serverHasRules() {
		return channel.available();
	}

	/** What the settings screen shows: this world's, the server's, or what new worlds start with. Null: nothing to show. */
	public static String shown(Rule rule) {
		Declared r = (Declared) rule;
		String v;
		if (serverValues != null) {
			if (!channel.available()) return null;
			v = serverValues.get(r.id);
		} else {
			v = world != null ? worldValues.get(r.id) : newWorld.values().getProperty(key(r.id));
		}
		return v != null ? r.read(v.trim()) : r.defaultText;
	}

	/** A change from this game: to the open world, or with none open to what new worlds start with. A rule chosen at creation only takes the second. */
	public static boolean change(Rule rule, String value) {
		if (serverValues != null) return false;
		if (world != null && rule.atCreation()) {
			System.out.println("[faithful-core] " + rule.id() + " is as it was: a world chooses it when it's created");
			return false;
		}
		if (world != null && locked) {
			System.out.println("[faithful-core] " + rule.id() + " is as it was: this world's rules are locked");
			return false;
		}
		return world != null ? set(rule.id(), value) : newWorld.set(key(rule.id()), value);
	}

	private static boolean set(String id, String value) {
		if (locked || !world.set(key(id), value)) return false;
		worldValues.put(id, value);
		if (channel != null) sendTo(null);
		return true;
	}

	/** To one player, or everyone with the channel. */
	public static void sendTo(net.minecraft.entity.player.PlayerEntity player) {
		Channel.Writer w = out -> {
			out.writeShort(worldValues.size() + (locked ? 1 : 0));
			for (Map.Entry<String, String> e : worldValues.entrySet()) {
				out.writeUTF(e.getKey());
				out.writeUTF(e.getValue());
			}
			if (locked) {
				out.writeUTF(LOCKED_ON_WIRE);
				out.writeUTF("true");
			}
		};
		if (player != null) channel.toPlayer(player, w);
		else channel.toAll(w);
	}

	private static void heard(net.minecraft.entity.player.PlayerEntity player, DataInputStream in) throws IOException {
		if (serverValues == null) return; // in single player the world's own values are the truth
		int n = in.readUnsignedShort();
		if (n > MAX_RULES) throw new IOException(n + " rules in one message");
		Map<String, String> heard = new HashMap<>();
		for (int i = 0; i < n; i++) {
			String id = in.readUTF();
			heard.put(id, in.readUTF());
		}
		locked = "true".equals(heard.remove(LOCKED_ON_WIRE));
		serverValues = heard;
	}

	private static Declared find(String name) {
		Declared exact = RULES.get(name);
		if (exact != null) return exact;
		List<Declared> found = new ArrayList<>();
		for (Declared r : RULES.values()) {
			if (r.id.endsWith(":" + name)) found.add(r);
		}
		return found.size() == 1 ? found.get(0) : null;
	}

	private static void command(String[] args, Commands.Reply reply) {
		if (args.length > 0 && args[0].equalsIgnoreCase("lock")) {
			lockCommand(args, reply);
			return;
		}
		if (args.length == 0) {
			if (RULES.isEmpty()) reply.say("No rules here.");
			if (locked) reply.say("This world's rules are locked.");
			for (Declared r : RULES.values()) reply.say(r.id + " = " + r.text());
			return;
		}
		Declared r = find(args[0]);
		if (r == null) {
			reply.say("No rule called " + args[0] + " (or more than one: use its whole name)");
		} else if (args.length == 1) {
			reply.say(r.id + " = " + r.text());
		} else if (!r.values.contains(args[1].toLowerCase(java.util.Locale.ROOT))) {
			reply.say(r.id + " is one of: " + String.join(", ", r.values));
		} else if (world == null) {
			reply.say("No world is open");
		} else if (r.creation) {
			reply.say(r.id + " was chosen when this world was created, and stays.");
		} else if (locked) {
			reply.say("This world's rules are locked, so they can't be changed.");
		} else if (set(r.id, args[1].toLowerCase(java.util.Locale.ROOT))) {
			reply.say(r.id + " = " + r.text());
		} else {
			reply.say("Could not save that; see the log");
		}
	}

	/** {@code rule lock}, which asks to be sure, as the game's button does; {@code rule lock confirm} locks. Lines short enough for beta's chat. */
	private static void lockCommand(String[] args, Commands.Reply reply) {
		String slash = reply.player() == null ? "" : "/";
		if (world == null) {
			reply.say("No world is open");
		} else if (locked) {
			reply.say("This world's rules are already locked.");
		} else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
			if (lock()) {
				reply.say("This world's rules are locked. Nothing in the game can change them now, " + slash + "rule included.");
			} else {
				reply.say("Could not save that, so nothing is locked; see the log");
			}
		} else {
			reply.say("Locking this world's rules can't be undone in game or with " + slash + "rule: nobody can change them after.");
			reply.say("To lock them, type " + slash + "rule lock confirm");
		}
	}
}
