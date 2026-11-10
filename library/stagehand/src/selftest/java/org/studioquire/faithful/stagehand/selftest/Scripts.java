package org.studioquire.faithful.stagehand.selftest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

/** A script's way there and its result's way back, by the real folder, as the helper does it. */
public final class Scripts {
	private Scripts() {
	}

	public static Path folder() {
		return FabricLoader.getInstance().getGameDir().resolve("stagehand");
	}

	public static void send(String id, String script) {
		sendBytes(id, script.getBytes(StandardCharsets.UTF_8), 0);
	}

	public static void sendBytes(String id, byte[] bytes, long ageMs) {
		try {
			Path in = folder().resolve("in"), temp = in.resolve(id + ".tmp");
			Files.createDirectories(in);
			Files.deleteIfExists(folder().resolve("out").resolve(id + ".json"));
			Files.write(temp, bytes);
			if (ageMs != 0) Files.setLastModifiedTime(temp, FileTime.fromMillis(System.currentTimeMillis() - ageMs));
			Files.move(temp, in.resolve(id + ".txt"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The result as it stands, or null. */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> result(String id) {
		Path path = folder().resolve("out").resolve(id + ".json");
		try {
			if (!Files.isRegularFile(path)) return null;
			return (Map<String, Object>) new Reader(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).value();
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean over(String id) {
		Map<String, Object> result = result(id);
		return result != null && !"running".equals(result.get("state"));
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> steps(Map<String, Object> result) {
		return (List<Map<String, Object>>) result.get("steps");
	}

	/** What this step of the result said, by its place among those run. */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> data(Map<String, Object> result, int step) {
		List<Map<String, Object>> steps = steps(result);
		if (step >= steps.size()) return new LinkedHashMap<>();
		Object data = steps.get(step).get("data");
		return data == null ? new LinkedHashMap<>() : (Map<String, Object>) data;
	}

	@SuppressWarnings("unchecked")
	public static String errorCode(Map<String, Object> result) {
		Object error = result.get("error");
		return error == null ? null : (String) ((Map<String, Object>) error).get("code");
	}

	/** Enough JSON to read what the mod writes. */
	private static final class Reader {
		private final String s;
		private int at;

		Reader(String s) {
			this.s = s;
		}

		Object value() {
			space();
			char c = s.charAt(at);
			if (c == '{') {
				at++;
				Map<String, Object> map = new LinkedHashMap<>();
				while (true) {
					space();
					if (s.charAt(at) == '}') {
						at++;
						return map;
					}
					if (s.charAt(at) == ',') {
						at++;
						continue;
					}
					String key = string();
					space();
					at++;
					map.put(key, value());
				}
			}
			if (c == '[') {
				at++;
				List<Object> list = new ArrayList<>();
				while (true) {
					space();
					if (s.charAt(at) == ']') {
						at++;
						return list;
					}
					if (s.charAt(at) == ',') {
						at++;
						continue;
					}
					list.add(value());
				}
			}
			if (c == '"') return string();
			int start = at;
			while (at < s.length() && ",]} \n\r\t".indexOf(s.charAt(at)) < 0) at++;
			String word = s.substring(start, at);
			if (word.equals("true")) return Boolean.TRUE;
			if (word.equals("false")) return Boolean.FALSE;
			if (word.equals("null")) return null;
			return Double.valueOf(word);
		}

		private void space() {
			while (Character.isWhitespace(s.charAt(at))) at++;
		}

		private String string() {
			StringBuilder out = new StringBuilder();
			at++;
			while (s.charAt(at) != '"') {
				char c = s.charAt(at++);
				if (c == '\\') {
					char e = s.charAt(at++);
					switch (e) {
						case 'n': out.append('\n'); break;
						case 'r': out.append('\r'); break;
						case 't': out.append('\t'); break;
						case 'u':
							out.append((char) Integer.parseInt(s.substring(at, at + 4), 16));
							at += 4;
							break;
						default: out.append(e);
					}
				} else {
					out.append(c);
				}
			}
			at++;
			return out.toString();
		}
	}
}
