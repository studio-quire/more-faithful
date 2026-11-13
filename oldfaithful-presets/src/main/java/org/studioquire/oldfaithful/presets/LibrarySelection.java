package org.studioquire.oldfaithful.presets;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Which of a library pack's mods are switched on: every jar sits in {@code mods-library/}, and the launch reads
 * {@code mods-library/selection.txt} ({@code -Dfabric.addMods=@mods-library/selection.txt}; a path is relative to the game
 * folder, where Prism starts the game). Jars in the folder have no version in their names, so a pack update that replaces one leaves
 * the list right. A line whose jar has gone anyway (replaced by hand under a versioned name) can't be mended for the launch that finds it, which the loader
 * has already read the list for: it's repointed for the next one, and {@link #notice} tells the player. A change takes effect at the next launch.
 * Faithful Core follows the rest: on while anything is. A list the loader would stop on is never written: a mod comes on with what it
 * {@code depends} on, and a mod going off takes what depends on it. Only names ever come from elsewhere; nothing here fetches anything.
 * <p>
 * A pack may offer presets (More Faithful; its {@code pack.properties} beside the jars): which of its mods are on, one for the whole game. The install's
 * preset, and the mods turned on or off beyond it, are kept in {@code config/faithful-preset.properties}, which players carry into a new version of the
 * pack with the rest of {@code config/}, where the list itself is the new pack's. Every change to the list here records itself there, and at each start
 * ({@link #reconcile}) the list is put back to what the record says, for the next launch. Without presets none of that happens.
 */
public final class LibrarySelection {
	public static final String FOLDER = "mods-library";
	private static final String LIST = "selection.txt";
	/** The pack's own file, which the game never writes: its name, the rules its new worlds start with, and its presets. */
	private static final String PACK_FILE = "pack.properties";
	/** The install's, in {@code config/}: its preset and what it turned on or off beyond it. */
	public static final String RECORD = "faithful-preset.properties";
	private static final String CORE = "faithful-core";
	private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
	/** What fits the settings screen's two lines of hint, in the game's font, with room for how the words fall. */
	private static final int HINT_LETTERS = 96;
	private static final Pattern DESCRIPTION = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+?)\\.");
	private static final Pattern DEPENDS = Pattern.compile("\"depends\"\\s*:\\s*\\{([^}]*)\\}");
	private static final Pattern KEY = Pattern.compile("\"([^\"]+)\"\\s*:");
	private static final Pattern MUST_MATCH = Pattern.compile("\"(content|match)\"\\s*:\\s*true");

	/** Said on the title screen of the launch that found a stale list; null if there's nothing to say. */
	private static volatile String notice;

	/** One jar in the folder. */
	public static final class Entry {
		public final String id;
		public final String name;
		public final String description;
		final String path;
		final Set<String> depends;
		/** Content, or {@code "match"}: a server decides whether this one is on. */
		final boolean mustMatch;

		Entry(String id, String name, String description, String path, Set<String> depends, boolean mustMatch) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.path = path;
			this.depends = depends;
			this.mustMatch = mustMatch;
		}
	}

	/** What a change did, beyond what was asked. */
	public static final class Change {
		/** False if the list couldn't be saved, and nothing changed. */
		public final boolean saved;
		public final List<String> on = new ArrayList<>(), off = new ArrayList<>();
		/** Mods a server has that the folder doesn't. */
		public final List<String> absent = new ArrayList<>();

		Change(boolean saved) {
			this.saved = saved;
		}

		/** For a screen: what else moved. Null if nothing did. */
		public String also() {
			String turned = on.isEmpty() ? "" : "Also on: " + String.join(", ", on) + ". ";
			if (!off.isEmpty()) turned += "Also off: " + String.join(", ", off) + ".";
			return turned.isEmpty() ? null : turned.trim();
		}
	}

	/** One of the pack's presets: the mods it has on, by id. */
	public static final class Preset {
		/** What an install's record keeps, so it never changes; the name is free to. */
		public final String id;
		public final String name;
		public final String hint;
		final Set<String> mods;

		Preset(String id, String name, String hint, Set<String> mods) {
			this.id = id;
			this.name = name;
			this.hint = hint;
			this.mods = mods;
		}
	}

	/** What the install's record says: its preset's id (maybe one this pack no longer has) and the mods turned on and off beyond it. */
	private static final class Record {
		final String preset;
		final Set<String> on, off;

		Record(String preset, Set<String> on, Set<String> off) {
			this.preset = preset;
			this.on = on;
			this.off = off;
		}
	}

	private final Path folder;
	private final Path record;
	private final List<Entry> entries = new ArrayList<>();
	private final List<Preset> presets = new ArrayList<>();
	private Preset firstChoice;
	private Entry core;
	/**
	 * The list and the record as last read, since the settings screen asks for them every frame: read again after a write here, and after {@link #forget}.
	 * Null {@code listed}, or {@code recordRead} false, until then.
	 */
	private Set<String> listed;
	private Record kept;
	private boolean recordRead;

	public LibrarySelection(Path gameDir) {
		folder = gameDir.resolve(FOLDER);
		record = gameDir.resolve("config").resolve(RECORD);
		if (!Files.isDirectory(folder)) return;
		try (DirectoryStream<Path> jars = Files.newDirectoryStream(folder, "*.jar")) {
			for (Path jar : jars) {
				Entry e = read(jar);
				if (e == null) continue;
				if (e.id.equals(CORE)) core = e;
				else entries.add(e);
			}
		} catch (IOException e) {
			System.out.println("[morefaithful-presets] WARN Could not read " + folder + ": " + e);
		}
		entries.sort(Comparator.comparing(e -> e.name));
		readPresets();
		repoint();
	}

	/**
	 * The pack's presets from its file: {@code presets=a,b,c} in the order a click steps through them, {@code preset.default=b} for a fresh install, and for
	 * each {@code preset.<id>.name}, {@code .hint} and {@code .mods} (ids, with a comma between). The build writes them, closed under what the mods depend
	 * on. Room for more lines under {@code preset.<id>.} later (rules of a preset's own, say), which this ignores.
	 */
	private void readPresets() {
		Path file = folder.resolve(PACK_FILE);
		if (!Files.isRegularFile(file)) return;
		Properties pack = new Properties();
		try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			pack.load(in);
		} catch (IOException | IllegalArgumentException e) {
			System.out.println("[morefaithful-presets] WARN Could not read " + file + ", so there are no presets: " + e);
			return;
		}
		for (String id : ids(pack.getProperty("presets"))) {
			String name = pack.getProperty("preset." + id + ".name", id).trim();
			presets.add(new Preset(id, name, pack.getProperty("preset." + id + ".hint", "").trim(), new LinkedHashSet<>(ids(pack.getProperty("preset." + id + ".mods")))));
		}
		firstChoice = preset(pack.getProperty("preset.default", "").trim());
		if (firstChoice == null && !presets.isEmpty()) firstChoice = presets.get(0);
	}

	private static List<String> ids(String line) {
		List<String> ids = new ArrayList<>();
		if (line == null) return ids;
		for (String id : line.split(",")) {
			if (!id.trim().isEmpty()) ids.add(id.trim());
		}
		return ids;
	}

	private Preset preset(String id) {
		for (Preset p : presets) {
			if (p.id.equals(id)) return p;
		}
		return null;
	}

	/** A line with no jar goes to the jar whose mod it's named for: {@code mods-library/world-rules-0.1.0.jar}, the id and a version, as the build names them. */
	private void repoint() {
		Set<String> lines = selected(), mended = new LinkedHashSet<>();
		List<Entry> all = new ArrayList<>(entries);
		if (core != null) all.add(core);
		boolean changed = false;
		List<String> gone = new ArrayList<>(), moved = new ArrayList<>();
		for (String line : lines) {
			String kept = line;
			if (!Files.exists(folder.getParent().resolve(line))) {
				String file = line.substring(line.lastIndexOf('/') + 1);
				Entry match = null;
				for (Entry e : all) {
					if (file.matches(Pattern.quote(e.id) + "(-\\d.*)?\\.jar")) match = e;
				}
				if (match != null) {
					kept = match.path;
					changed = true;
					moved.add(match.name);
					System.out.println("[morefaithful-presets] " + line + " is gone: the library's list now names " + kept + ", from the next launch");
				} else {
					gone.add(file);
					changed = true;
					System.out.println("[morefaithful-presets] WARN The library's list named " + line + ", which isn't there: it's off the list");
					continue;
				}
			}
			mended.add(kept);
		}
		if (changed) save(mended);
		if (!moved.isEmpty()) notice = "Not loaded, its jar renamed: " + brief(moved) + ". Restart the game to load it.";
		else if (!gone.isEmpty()) notice = "Not loaded, its jar gone: " + brief(gone) + ".";
	}

	private static String brief(List<String> names) {
		return names.size() <= 3 ? String.join(", ", names) : names.get(0) + " and " + (names.size() - 1) + " more";
	}

	/** For the title screen. */
	public static String notice() {
		return notice;
	}

	private static Entry read(Path jar) {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			ZipEntry json = zip.getEntry("fabric.mod.json");
			if (json == null) return null;
			String text;
			try (InputStream in = zip.getInputStream(json)) {
				text = new String(readAll(in), StandardCharsets.UTF_8);
			}
			Matcher id = ID.matcher(text), name = NAME.matcher(text), description = DESCRIPTION.matcher(text), depends = DEPENDS.matcher(text);
			if (!id.find()) return null;
			Set<String> needs = new LinkedHashSet<>();
			if (depends.find()) {
				for (Matcher key = KEY.matcher(depends.group(1)); key.find(); ) needs.add(key.group(1));
			}
			return new Entry(id.group(1), name.find() ? name.group(1) : id.group(1), description.find() ? hint(description.group(1) + ".") : "", FOLDER + "/" + jar.getFileName(),
					needs, MUST_MATCH.matcher(text).find());
		} catch (IOException e) {
			System.out.println("[morefaithful-presets] WARN Could not read " + jar + ": " + e);
			return null;
		}
	}

	/** A mod's first sentence, cut at a word if it would run past the hint's two lines. */
	static String hint(String sentence) {
		if (sentence.length() <= HINT_LETTERS) return sentence;
		String cut = sentence.substring(0, sentence.lastIndexOf(' ', HINT_LETTERS - 3));
		return cut.replaceAll("[,;:]$", "") + "...";
	}

	private static byte[] readAll(InputStream in) throws IOException {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) out.write(buffer, 0, n);
		return out.toByteArray();
	}

	/** The mods to choose from; the core isn't one, it follows them. */
	public List<Entry> entries() {
		return entries;
	}

	/** Reads the list and the record again at the next ask, so edits by hand show: for the settings screen, each time a page is built. */
	public void forget() {
		listed = null;
		recordRead = false;
	}

	private Set<String> selected() {
		if (listed == null) {
			listed = new LinkedHashSet<>();
			try {
				Path list = folder.resolve(LIST);
				if (Files.isRegularFile(list)) {
					for (String line : Files.readAllLines(list, StandardCharsets.UTF_8)) {
						if (!line.trim().isEmpty()) listed.add(line.trim());
					}
				}
			} catch (IOException e) {
				System.out.println("[morefaithful-presets] WARN Could not read the library's selection: " + e);
			}
		}
		return new LinkedHashSet<>(listed);
	}

	public boolean isOn(Entry entry) {
		return selected().contains(entry.path);
	}

	private Entry find(String id) {
		for (Entry e : entries) {
			if (e.id.equals(id)) return e;
		}
		return null;
	}

	private Set<Entry> chosen() {
		Set<String> lines = selected();
		Set<Entry> chosen = new LinkedHashSet<>();
		for (Entry e : entries) {
			if (lines.contains(e.path)) chosen.add(e);
		}
		return chosen;
	}

	/** {@code entry} and everything it depends on in the folder. */
	private Set<Entry> withNeeds(Entry entry) {
		Set<Entry> found = new LinkedHashSet<>();
		List<Entry> waiting = new ArrayList<>();
		waiting.add(entry);
		while (!waiting.isEmpty()) {
			Entry e = waiting.remove(waiting.size() - 1);
			if (!found.add(e)) continue;
			for (String id : e.depends) {
				Entry needed = find(id);
				if (needed != null) waiting.add(needed);
			}
		}
		return found;
	}

	/** {@code entry} and everything that depends on it, near or far. */
	private Set<Entry> withDependents(Entry entry) {
		Set<Entry> found = new LinkedHashSet<>();
		for (Entry e : entries) {
			if (withNeeds(e).contains(entry)) found.add(e);
		}
		return found;
	}

	/** Why {@code entry}'s switch can't be pressed: what {@code held} says of it, or of anything its going off would take. Null if nothing's held. */
	public String heldBy(Entry entry, Function<String, String> held) {
		for (Entry e : withDependents(entry)) {
			String reason = held.apply(e.id);
			if (reason != null) return reason;
		}
		return null;
	}

	/** Chooses a mod or not, for the next launch, along with whatever that takes. */
	public Change set(Entry entry, boolean on) {
		Set<Entry> wanted = chosen();
		if (on) wanted.addAll(withNeeds(entry));
		else wanted.removeAll(withDependents(entry));
		Change change = write(wanted, current());
		change.on.remove(entry.name);
		change.off.remove(entry.name);
		return change;
	}

	// ---- presets ------------------------------------------------------------------------------------------------------

	/** The pack's presets, in order; none without a pack file that has them. */
	public List<Preset> presets() {
		return presets;
	}

	/** The install's preset: the one its record names, else what a fresh install starts on. Null without presets. */
	public Preset current() {
		Record r = readRecord();
		Preset p = r == null ? null : preset(r.preset);
		return p != null ? p : firstChoice;
	}

	/** Whether the list has mods on or off beyond the install's preset. */
	public boolean custom() {
		Preset p = current();
		return p != null && !chosen().equals(entriesOf(p));
	}

	/** Whether the list differs from what this launch loaded, so a change waits for a restart. */
	public boolean waiting(java.util.function.Predicate<String> loaded) {
		for (Entry e : entries) {
			if (isOn(e) != loaded.test(e.id)) return true;
		}
		return false;
	}

	/** The preset's mods for the next launch, and nothing beyond it in the record. */
	public Change choose(Preset preset) {
		return write(entriesOf(preset), preset);
	}

	/**
	 * The preset a click on the screen goes to: the install's own from Custom, else the next in order, passing over any that would switch off a mod
	 * {@code held} holds (inside a world's locked rules). Null if every one would, with {@code held}'s reason in {@code why}.
	 */
	public Preset next(Function<String, String> held, String[] why) {
		Preset at = current();
		List<Preset> order = new ArrayList<>();
		if (custom()) order.add(at);
		int from = presets.indexOf(at);
		for (int i = 1; i < presets.size(); i++) order.add(presets.get((from + i) % presets.size()));
		Set<Entry> on = chosen();
		for (Preset p : order) {
			String reason = null;
			Set<Entry> keeps = entriesOf(p);
			for (Entry e : on) {
				if (!keeps.contains(e) && (reason = held.apply(e.id)) != null) break;
			}
			if (reason == null) return p;
			why[0] = reason;
		}
		return null;
	}

	/** The preset's mods that are in the folder, less any whose needs aren't. */
	private Set<Entry> entriesOf(Preset preset) {
		Set<Entry> found = new LinkedHashSet<>();
		for (Entry e : entries) {
			if (preset.mods.contains(e.id)) found.add(e);
		}
		return complete(found);
	}

	/** What the record says the list should be: its preset's mods, those it turned on with what they need, less those it turned off with what needs them. */
	private Set<Entry> wanted(Record r) {
		Preset base = preset(r.preset);
		Set<Entry> wanted = entriesOf(base != null ? base : firstChoice);
		for (String id : r.on) {
			Entry e = find(id);
			if (e != null) wanted.addAll(withNeeds(e));
		}
		for (String id : r.off) {
			Entry e = find(id);
			if (e != null) wanted.removeAll(withDependents(e));
		}
		return complete(wanted);
	}

	/** Without any mod whose needs in the folder aren't all there too. */
	private Set<Entry> complete(Set<Entry> mods) {
		boolean dropped = true;
		while (dropped) dropped = mods.removeIf(e -> !mods.containsAll(withNeeds(e)));
		return mods;
	}

	/**
	 * At each start, where the pack has presets: the list put back to what the install's record says, for the next launch, since this one has already
	 * loaded it. That's how a new version of the pack, whose list is its default preset's, becomes the install's own again once its {@code config/} is
	 * copied in; then the title screen says to restart. The first start with presets has no record: the list as it came is the start, so a fresh install
	 * records the default preset, and an install whose old list was copied in keeps it as its changes.
	 */
	public void reconcile() {
		if (presets.isEmpty()) return;
		Record r = readRecord();
		Set<Entry> loaded = chosen();
		if (r == null) {
			remember(loaded, firstChoice);
			return;
		}
		Set<Entry> wanted = wanted(r);
		if (wanted.equals(loaded)) return;
		Preset base = current();
		Change change = write(wanted, base);
		if (!change.saved) return;
		System.out.println("[morefaithful-presets] The library's list wasn't what config/" + RECORD + " says (" + base.id + ", on " + r.on + ", off " + r.off
				+ "): written for the next launch. On: " + change.on + ", off: " + change.off);
		notice = wanted.equals(entriesOf(base)) ? "Mods set back to your preset, " + base.name + ". Restart the game to play with them."
				: "Mods set back to your preset and your changes to it. Restart the game to play with them.";
	}

	private Record readRecord() {
		if (!recordRead) {
			kept = loadRecord();
			recordRead = true;
		}
		return kept;
	}

	private Record loadRecord() {
		if (presets.isEmpty() || !Files.isRegularFile(record)) return null;
		Properties p = new Properties();
		try (Reader in = Files.newBufferedReader(record, StandardCharsets.UTF_8)) {
			p.load(in);
		} catch (IOException | IllegalArgumentException e) {
			System.out.println("[morefaithful-presets] WARN Could not read " + record + ", so the list is taken as it is: " + e);
			return null;
		}
		return new Record(p.getProperty("preset", "").trim(), new LinkedHashSet<>(ids(p.getProperty("on"))), new LinkedHashSet<>(ids(p.getProperty("off"))));
	}

	/**
	 * The record of {@code base} with what {@code wanted} has beyond it, written whole (the file is the game's). A change the old record had to a mod whose
	 * jar isn't in the folder stays, so a jar the player adds by hand after an update (Wary Creepers) is on again once it's there.
	 */
	private void remember(Set<Entry> wanted, Preset base) {
		Set<Entry> preset = entriesOf(base);
		Set<String> on = new LinkedHashSet<>(), off = new LinkedHashSet<>();
		Record old = readRecord();
		if (old != null) {
			for (String id : old.on) if (find(id) == null) on.add(id);
			for (String id : old.off) if (find(id) == null) off.add(id);
		}
		for (Entry e : entries) {
			if (wanted.contains(e) && !preset.contains(e)) on.add(e.id);
			if (!wanted.contains(e) && preset.contains(e)) off.add(e.id);
		}
		List<String> lines = new ArrayList<>();
		lines.add("# Which preset of the pack's mods this game uses, and the mods turned on or off beyond it, as chosen in the settings' Mods");
		lines.add("# menu. Kept here so it comes with the rest of config/ into a new version of the pack: at each start the game sets");
		lines.add("# mods-library/selection.txt to what this says, for the next launch.");
		lines.add("preset=" + base.id);
		lines.add("on=" + String.join(",", on));
		lines.add("off=" + String.join(",", off));
		Path temporary = record.resolveSibling(RECORD + ".tmp");
		try {
			Files.createDirectories(record.getParent());
			Files.write(temporary, lines, StandardCharsets.UTF_8);
			Files.move(temporary, record, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("[morefaithful-presets] WARN Could not save " + record + ": " + e);
		}
		recordRead = false;
	}

	/**
	 * A server's set, by the ids in its hello that must match: those on, every other mod that must match off. Mods that needn't match stay as the
	 * player had them, unless what one depends on just went.
	 */
	public Change useServerSet(Iterable<String> mustMatch) {
		Set<Entry> wanted = chosen();
		wanted.removeIf(e -> e.mustMatch);
		List<String> absent = new ArrayList<>();
		for (String id : mustMatch) {
			Entry e = find(id);
			if (e != null) wanted.addAll(withNeeds(e));
			else absent.add(id);
		}
		for (Entry e : new ArrayList<>(wanted)) {
			if (!wanted.containsAll(withNeeds(e))) wanted.remove(e);
		}
		Change change = write(wanted, current());
		change.absent.addAll(absent);
		return change;
	}

	/** The list with these mods on, recorded, where the pack has presets, as {@code base} and what's on or off beyond it. */
	private Change write(Set<Entry> wanted, Preset base) {
		Set<String> lines = selected();
		Set<Entry> before = chosen();
		if (core != null) lines.remove(core.path);
		for (Entry e : entries) {
			if (wanted.contains(e)) lines.add(e.path);
			else lines.remove(e.path);
		}
		if (core != null && !lines.isEmpty()) lines.add(core.path);
		Change change = new Change(save(lines));
		if (!change.saved) return change;
		if (base != null) remember(wanted, base);
		for (Entry e : entries) {
			if (wanted.contains(e) && !before.contains(e)) change.on.add(e.name);
			if (!wanted.contains(e) && before.contains(e)) change.off.add(e.name);
		}
		return change;
	}

	private boolean save(Set<String> lines) {
		Path temporary = folder.resolve(LIST + ".tmp");
		listed = null;
		try {
			Files.write(temporary, lines, StandardCharsets.UTF_8);
			Files.move(temporary, folder.resolve(LIST), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			System.out.println("[morefaithful-presets] WARN Could not save the library's selection: " + e);
			return false;
		}
	}
}
