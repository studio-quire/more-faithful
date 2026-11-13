package org.studioquire.oldfaithful.presets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LibrarySelectionTest {
	private static void jar(Path file, String id) throws IOException {
		jar(file, id, "");
	}

	/** {@code more}: the rest of the file, from a comma. */
	private static void jar(Path file, String id, String more) throws IOException {
		try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
			zip.putNextEntry(new ZipEntry("fabric.mod.json"));
			zip.write(("{\"id\": \"" + id + "\", \"name\": \"" + id + "\", \"description\": \"What it does. More.\"" + more + "}").getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
	}

	private static String needs(String... ids) {
		StringBuilder s = new StringBuilder(", \"depends\": {\"faithful-core\": \"*\"");
		for (String id : ids) s.append(", \"").append(id).append("\": \"*\"");
		return s + "}";
	}

	private static final String CONTENT = ", \"custom\": {\"faithful\": {\"content\": true, \"network\": 1}}";

	/** materials <- tools <- armoury, and a client-side map that needs materials' colours; a lone content mod; a lone map. */
	private static Path chain(Path game) throws IOException {
		Path folder = Files.createDirectories(game.resolve("mods-library"));
		jar(folder.resolve("faithful-core.jar"), "faithful-core");
		jar(folder.resolve("materials.jar"), "materials", needs() + CONTENT);
		jar(folder.resolve("tools.jar"), "tools", needs("materials") + CONTENT);
		jar(folder.resolve("armoury.jar"), "armoury", needs("tools") + CONTENT);
		jar(folder.resolve("ore-map.jar"), "ore-map", needs("materials"));
		jar(folder.resolve("farm.jar"), "farm", needs() + CONTENT);
		jar(folder.resolve("minimap.jar"), "minimap", needs());
		return folder;
	}

	private static LibrarySelection.Entry entry(LibrarySelection library, String id) {
		return library.entries().stream().filter(e -> e.id.equals(id)).findFirst().get();
	}

	private static List<String> on(LibrarySelection library) {
		return library.entries().stream().filter(library::isOn).map(e -> e.id).collect(java.util.stream.Collectors.toList());
	}

	private static List<String> list(Path game) throws IOException {
		return Files.readAllLines(game.resolve("mods-library").resolve("selection.txt"));
	}

	@Test
	void aLongFirstSentenceIsCutToAHintAtAWord() {
		assertEquals("What it does.", LibrarySelection.hint("What it does."));
		String cut = LibrarySelection.hint("Word, ".repeat(40).trim() + ".");
		assertTrue(cut.length() <= 96 && cut.endsWith("Word..."), cut);
	}

	@Test
	void theCoreFollowsTheRest(@TempDir Path game) throws IOException {
		Path folder = Files.createDirectories(game.resolve("mods-library"));
		jar(folder.resolve("faithful-core.jar"), "faithful-core");
		jar(folder.resolve("world-rules.jar"), "world-rules");
		LibrarySelection library = new LibrarySelection(game);
		assertEquals(1, library.entries().size());
		LibrarySelection.Entry rules = library.entries().get(0);
		assertEquals("What it does.", rules.description);
		assertFalse(library.isOn(rules));
		assertTrue(library.set(rules, true).saved);
		assertEquals(List.of("mods-library/world-rules.jar", "mods-library/faithful-core.jar"), list(game));
		assertTrue(library.set(rules, false).saved);
		assertEquals(List.of(), list(game));
	}

	@Test
	void aModComesOnWithWhatItDependsOn(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		LibrarySelection.Change change = library.set(entry(library, "armoury"), true);
		assertEquals(List.of("armoury", "materials", "tools"), on(library));
		assertEquals(List.of("materials", "tools"), change.on);
		assertEquals("Also on: materials, tools.", change.also());
		assertTrue(list(game).contains("mods-library/faithful-core.jar"));
		assertEquals(null, library.set(entry(library, "farm"), true).also());
	}

	@Test
	void aHeldModHoldsWhatItDependsOnButNotWhatDependsOnIt(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		java.util.function.Function<String, String> holdingTools = id -> id.equals("tools") ? "Held." : null;
		assertEquals("Held.", library.heldBy(entry(library, "tools"), holdingTools), "its own switch");
		assertEquals("Held.", library.heldBy(entry(library, "materials"), holdingTools), "going off would take it");
		assertEquals(null, library.heldBy(entry(library, "armoury"), holdingTools), "going off leaves it");
		assertEquals(null, library.heldBy(entry(library, "ore-map"), holdingTools), "a neighbour on the same dependency");
		assertEquals(null, library.heldBy(entry(library, "farm"), holdingTools), "nothing to do with it");
		assertEquals(null, library.heldBy(entry(library, "tools"), id -> null), "nothing held");
	}

	@Test
	void aDependencyGoingOffTakesWhatNeedsIt(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		library.set(entry(library, "armoury"), true);
		library.set(entry(library, "ore-map"), true);
		library.set(entry(library, "minimap"), true);
		LibrarySelection.Change change = library.set(entry(library, "materials"), false);
		assertEquals(List.of("minimap"), on(library));
		assertEquals(List.of("armoury", "ore-map", "tools"), change.off);
		assertEquals("Also off: armoury, ore-map, tools.", change.also());
		library.set(entry(library, "minimap"), false);
		assertEquals(List.of(), list(game));
	}

	@Test
	void aServersSetLeavesOneSidedModsAlone(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		library.set(entry(library, "farm"), true);
		library.set(entry(library, "minimap"), true);
		LibrarySelection.Change change = library.useServerSet(List.of("tools", "strange-mod"));
		assertEquals(List.of("materials", "minimap", "tools"), on(library));
		assertEquals(List.of("materials", "tools"), change.on);
		assertEquals(List.of("farm"), change.off);
		assertEquals(List.of("strange-mod"), change.absent);
	}

	@Test
	void aServersSetTakesAOneSidedModWhoseDependencyWent(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		library.set(entry(library, "ore-map"), true);
		library.set(entry(library, "minimap"), true);
		library.useServerSet(List.of("farm"));
		assertEquals(List.of("farm", "minimap"), on(library));
	}

	@Test
	void aLineWhoseJarWasRenamedFollowsItsModId(@TempDir Path game) throws IOException {
		Path folder = Files.createDirectories(game.resolve("mods-library"));
		jar(folder.resolve("faithful-core-0.2.0.jar"), "faithful-core");
		jar(folder.resolve("world-rules-0.2.0.jar"), "world-rules");
		Files.write(folder.resolve("selection.txt"), List.of("mods-library/world-rules-0.1.0.jar", "mods-library/faithful-core-0.1.0.jar", "mods-library/gone-1.0.jar"));
		new LibrarySelection(game);
		assertEquals(List.of("mods-library/world-rules-0.2.0.jar", "mods-library/faithful-core-0.2.0.jar"), list(game));
		assertTrue(LibrarySelection.notice().contains("world-rules"));
	}

	/** The chain with presets, as a pack's file has them: small (the map alone), tools (materials and tools), all; a fresh install starts on all. */
	private static void presets(Path game) throws IOException {
		chain(game);
		Files.write(game.resolve("mods-library").resolve("pack.properties"), List.of("name=Test Pack", "presets=small,tools,all", "preset.default=all",
				"preset.small.name=Small", "preset.small.hint=The map.", "preset.small.mods=minimap",
				"preset.tools.name=Tools", "preset.tools.mods=materials, tools",
				"preset.all.name=All", "preset.all.mods=materials,tools,armoury,ore-map,farm,minimap",
				"rule.world-rules.sleeping=false"));
	}

	private static final String ALL = "mods-library/armoury.jar\nmods-library/farm.jar\nmods-library/materials.jar\nmods-library/minimap.jar\n"
			+ "mods-library/ore-map.jar\nmods-library/tools.jar\nmods-library/faithful-core.jar\n";

	private static String record(Path game) throws IOException {
		return String.join("\n", Files.readAllLines(game.resolve("config").resolve(LibrarySelection.RECORD))).replaceAll("(?m)^#.*\n", "");
	}

	@Test
	void withoutAPackFileThereAreNoPresetsAndNoRecord(@TempDir Path game) throws IOException {
		chain(game);
		LibrarySelection library = new LibrarySelection(game);
		library.set(entry(library, "farm"), true);
		library.reconcile();
		assertTrue(library.presets().isEmpty());
		assertEquals(null, library.current());
		assertFalse(Files.exists(game.resolve("config")));
	}

	@Test
	void aFreshInstallRecordsTheDefaultPresetAtItsFirstStart(@TempDir Path game) throws IOException {
		presets(game);
		Files.writeString(game.resolve("mods-library").resolve("selection.txt"), ALL);
		LibrarySelection library = new LibrarySelection(game);
		assertEquals(List.of("Small", "Tools", "All"), library.presets().stream().map(p -> p.name).collect(java.util.stream.Collectors.toList()));
		library.reconcile();
		assertEquals("preset=all\non=\noff=", record(game));
		assertEquals("All", library.current().name);
		assertFalse(library.custom());
		assertEquals(ALL, Files.readString(game.resolve("mods-library").resolve("selection.txt")), "the list as it came");
	}

	@Test
	void choosingAPresetWritesItsListAndASwitchIsAChangeToIt(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection library = new LibrarySelection(game);
		library.choose(library.presets().get(1));
		assertEquals(List.of("materials", "tools"), on(library));
		assertTrue(list(game).contains("mods-library/faithful-core.jar"));
		assertEquals("preset=tools\non=\noff=", record(game));
		assertFalse(library.custom());
		library.set(entry(library, "armoury"), true);
		library.set(entry(library, "materials"), false);
		assertEquals(List.of(), on(library), "materials took tools and armoury with it");
		assertEquals("preset=tools\non=\noff=materials,tools", record(game));
		assertTrue(library.custom());
		library.set(entry(library, "farm"), true);
		assertEquals("preset=tools\non=farm\noff=materials,tools", record(game));
		library.choose(library.current());
		assertEquals(List.of("materials", "tools"), on(library));
		assertEquals("preset=tools\non=\noff=", record(game), "choosing it again clears the changes");
	}

	@Test
	void aClickGoesToTheNextPresetOrFromCustomBackToItsOwn(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection library = new LibrarySelection(game);
		library.choose(library.presets().get(2));
		assertEquals("small", library.next(id -> null, new String[1]).id, "after the last, the first");
		String[] why = new String[1];
		assertEquals("tools", library.next(id -> id.equals("tools") ? "Locked." : null, why).id, "past one that would turn off a held mod");
		assertEquals(null, library.next(id -> id.equals("farm") ? "Locked." : null, why), "every one would");
		assertEquals("Locked.", why[0]);
		library.set(entry(library, "farm"), false);
		assertEquals("all", library.next(id -> null, new String[1]).id, "custom: its own first");
	}

	@Test
	void anUpdateIsPutBackToTheRecordForTheNextLaunch(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection before = new LibrarySelection(game);
		before.choose(before.presets().get(1));
		before.set(entry(before, "minimap"), true);
		String record = record(game);
		// The new version: its list is its default preset's, every mod on, and config/ is carried over.
		Files.writeString(game.resolve("mods-library").resolve("selection.txt"), ALL);
		LibrarySelection after = new LibrarySelection(game);
		after.reconcile();
		assertEquals(List.of("materials", "minimap", "tools"), on(after));
		assertTrue(list(game).contains("mods-library/faithful-core.jar"));
		assertEquals(record, record(game));
		assertTrue(LibrarySelection.notice().contains("Restart"), LibrarySelection.notice());
		// And a start that finds it as the record says changes nothing.
		java.nio.file.attribute.FileTime written = Files.getLastModifiedTime(game.resolve("mods-library").resolve("selection.txt"));
		new LibrarySelection(game).reconcile();
		assertEquals(written, Files.getLastModifiedTime(game.resolve("mods-library").resolve("selection.txt")));
	}

	@Test
	void anOldListCarriedInIsKeptAsChangesToTheDefault(@TempDir Path game) throws IOException {
		presets(game);
		Files.writeString(game.resolve("mods-library").resolve("selection.txt"), "mods-library/farm.jar\nmods-library/faithful-core.jar\n");
		LibrarySelection library = new LibrarySelection(game);
		library.reconcile();
		assertEquals(List.of("farm"), on(library));
		assertEquals("preset=all\non=\noff=armoury,materials,minimap,ore-map,tools", record(game));
		assertTrue(library.custom());
	}

	@Test
	void aPresetTheRecordNamesThatsGoneIsTheDefault(@TempDir Path game) throws IOException {
		presets(game);
		Files.createDirectories(game.resolve("config"));
		Files.write(game.resolve("config").resolve(LibrarySelection.RECORD), List.of("preset=retired", "on=", "off=farm"));
		LibrarySelection library = new LibrarySelection(game);
		library.reconcile();
		assertEquals(List.of("armoury", "materials", "minimap", "ore-map", "tools"), on(library));
		assertEquals("all", library.current().id);
	}

	@Test
	void theRecordKeepsAPresetByIdSoARenameChangesNothing(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection before = new LibrarySelection(game);
		before.choose(before.presets().get(0));
		Path pack = game.resolve("mods-library").resolve("pack.properties");
		Files.writeString(pack, Files.readString(pack).replace("preset.small.name=Small", "preset.small.name=Tiny"));
		LibrarySelection after = new LibrarySelection(game);
		after.reconcile();
		assertEquals("Tiny", after.current().name);
		assertEquals(List.of("minimap"), on(after));
		assertEquals("preset=small\non=\noff=", record(game));
	}

	@Test
	void aChangeToAModWhoseJarIsGoneIsKeptForWhenItsBack(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection before = new LibrarySelection(game);
		before.choose(before.presets().get(0));
		before.set(entry(before, "farm"), true);
		// A new version without that jar in the folder: the player copies it in after the first start.
		Path farm = game.resolve("mods-library").resolve("farm.jar"), aside = game.resolve("farm.jar");
		Files.move(farm, aside);
		Files.writeString(game.resolve("mods-library").resolve("selection.txt"), ALL);
		new LibrarySelection(game).reconcile();
		assertEquals("preset=small\non=farm\noff=", record(game));
		Files.move(aside, farm);
		LibrarySelection back = new LibrarySelection(game);
		back.reconcile();
		assertEquals(List.of("farm", "minimap"), on(back));
	}

	@Test
	void aServersSetIsAChangeToThePreset(@TempDir Path game) throws IOException {
		presets(game);
		LibrarySelection library = new LibrarySelection(game);
		library.choose(library.presets().get(0));
		library.useServerSet(List.of("farm"));
		assertEquals(List.of("farm", "minimap"), on(library));
		assertEquals("preset=small\non=farm\noff=", record(game));
	}

	@Test
	void aNameOnlyLikeAModsIsntItsJar(@TempDir Path game) throws IOException {
		Path folder = Files.createDirectories(game.resolve("mods-library"));
		jar(folder.resolve("world.jar"), "world");
		Files.write(folder.resolve("selection.txt"), List.of("mods-library/world-rules-0.1.0.jar"));
		new LibrarySelection(game);
		assertEquals(List.of(), list(game));
		assertTrue(LibrarySelection.notice().contains("world-rules-0.1.0.jar"));
	}
}
