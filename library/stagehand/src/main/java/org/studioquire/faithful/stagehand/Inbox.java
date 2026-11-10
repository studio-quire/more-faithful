package org.studioquire.faithful.stagehand;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The folder: {@code stagehand/in/<id>.txt} are scripts, {@code stagehand/out/<id>.json} what became of them, each replaced whole so a reader never sees half of one. A writer does the same, or stops
 * writing within a moment. Only the helper and whoever drops scripts make the folders.
 */
final class Inbox {
	/** A file touched more recently than this may still be being written. */
	private static final long QUIET_MS = 250;
	/** Whoever dropped a script older than this has given up on it, and it shouldn't move someone out of the blue. */
	private static final long EXPIRES_MS = 10 * 60 * 1000;
	private static final long RESULTS_KEPT_MS = 3 * 24 * 60 * 60 * 1000L;
	private static final int MOST_BYTES = 64 * 1024;
	private static final String RUNNING_STATE = "\"state\": \"running\"";

	private static boolean tidied;

	private Inbox() {
	}

	private static Path folder() {
		return FabricLoader.getInstance().getGameDir().resolve("stagehand");
	}

	static void look() {
		File in = folder().resolve("in").toFile();
		String[] names = in.list();
		if (names == null || names.length == 0) return;
		tidy();
		Arrays.sort(names);
		long now = System.currentTimeMillis();
		for (String name : names) {
			if (!name.endsWith(".txt")) continue;
			File file = new File(in, name);
			if (!file.isFile() || now - file.lastModified() < QUIET_MS) continue;
			take(file, name.substring(0, name.length() - ".txt".length()).replaceAll("[^A-Za-z0-9._-]", "_"), now);
		}
	}

	private static void take(File file, String id, long now) {
		byte[] bytes = null;
		boolean tooBig = file.length() > MOST_BYTES, expired = now - file.lastModified() > EXPIRES_MS;
		try {
			if (!tooBig) bytes = Files.readAllBytes(file.toPath());
			Files.delete(file.toPath());
		} catch (IOException e) {
			return; // Someone has it open: next look.
		}
		Side.Refusal refusal = Stagehand.side().refusal();
		if (expired) answer(id, "expired", "expired", "it lay here over " + EXPIRES_MS / 60000 + " minutes, so it wasn't run");
		else if (refusal != null) answer(id, "refused", refusal.code, refusal.message);
		else if (tooBig) answer(id, "failed", "too_big", "a script is at most " + MOST_BYTES + " bytes");
		else start(id, bytes, now);
	}

	private static void start(String id, byte[] bytes, long now) {
		String text;
		try {
			text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException e) {
			answer(id, "failed", "not_text", "a script is UTF-8 text");
			return;
		}
		Script script = Script.parse(text);
		Stagehand.start(new Run(id, script, now));
	}

	/** An answer with no script run. */
	private static void answer(String id, String state, String code, String message) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("code", code);
		error.put("message", message);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", id);
		result.put("state", state);
		result.put("ok", false);
		result.put("error", error);
		result.put("steps", new java.util.ArrayList<>());
		write(id, result);
	}

	/** True when it's done. A reader holding the old file open can make this fail on Windows, so the caller tries again. */
	static boolean write(String id, Map<String, Object> result) {
		try {
			Path out = folder().resolve("out");
			Files.createDirectories(out);
			Path temp = out.resolve(id + ".json.tmp");
			Files.write(temp, Json.write(result).getBytes(StandardCharsets.UTF_8));
			Path target = out.resolve(id + ".json");
			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Once a run of the game, when there's a script at all. A result still saying it's running is from a game that ended without saying so (closing the window ends single player that way, and so does a
	 * crash), and old results are nobody's now.
	 */
	private static void tidy() {
		if (tidied) return;
		tidied = true;
		File[] results = folder().resolve("out").toFile().listFiles((dir, name) -> name.endsWith(".json"));
		if (results == null) return;
		long now = System.currentTimeMillis();
		for (File result : results) {
			try {
				if (now - result.lastModified() > RESULTS_KEPT_MS) {
					Files.deleteIfExists(result.toPath());
					continue;
				}
				String text = new String(Files.readAllBytes(result.toPath()), StandardCharsets.UTF_8);
				if (text.contains(RUNNING_STATE)) Files.write(result.toPath(), text.replace(RUNNING_STATE, "\"state\": \"lost\"").getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				// It stays as it is: nothing depends on this.
			}
		}
	}
}
