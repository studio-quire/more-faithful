package org.studioquire.oldfaithful.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server's hello and the client's reply, on channel 0. Sections are length-prefixed so a newer core's can be skipped.
 * Every list is capped and every read bounded by the message; what's read here came from the other side.
 */
public final class Hello {
	public static final int PROTOCOL = 1;
	static final int KIND_HELLO = 1, KIND_REPLY = 2;
	private static final int SECTION_MODS = 1, SECTION_CHANNELS = 2, SECTION_MATCH = 3, SECTION_ACCEPTED = 4;
	private static final int MAX_ENTRIES = 4096;

	public int protocol = PROTOCOL;
	public int kind;
	public boolean accepted;
	public List<ModInfo> mods = new ArrayList<>();
	/** Channels by number, in the hello. */
	public Map<Integer, String> channels = new LinkedHashMap<>();
	public Map<Integer, Integer> channelProtocols = new LinkedHashMap<>();
	public boolean hasMatch;
	public boolean hasContent;
	public byte[] matchHash = new byte[0];
	/** Numbers the client also has, in the reply. */
	public Set<Integer> acceptedChannels = new HashSet<>();

	/** Hash of what must match, or null when nothing must: such a server admits everyone. */
	public static byte[] matchHash(List<ModInfo> mods, List<String> contentLines) {
		boolean any = !contentLines.isEmpty();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (ModInfo m : mods) {
				if (!m.mustMatch()) continue;
				any = true;
				digest.update(("mod:" + m.id + ":" + m.network + "\n").getBytes(StandardCharsets.UTF_8));
			}
			for (String line : contentLines) digest.update((line + "\n").getBytes(StandardCharsets.UTF_8));
			return any ? digest.digest() : null;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private interface Body {
		void write(DataOutputStream out) throws IOException;
	}

	private static void section(DataOutputStream out, int id, Body body) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		body.write(new DataOutputStream(bytes));
		out.writeByte(id);
		out.writeInt(bytes.size());
		bytes.writeTo(out);
	}

	public byte[] encode() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(kind);
			out.writeShort(protocol);
			out.writeBoolean(accepted);
			section(out, SECTION_MODS, o -> {
				o.writeShort(mods.size());
				for (ModInfo m : mods) {
					o.writeUTF(m.id);
					o.writeUTF(m.version);
					o.writeByte((m.content ? 1 : 0) | (m.match ? 2 : 0));
					o.writeInt(m.network);
				}
			});
			if (kind == KIND_HELLO) {
				section(out, SECTION_CHANNELS, o -> {
					o.writeShort(channels.size());
					for (Map.Entry<Integer, String> e : channels.entrySet()) {
						o.writeShort(e.getKey());
						o.writeUTF(e.getValue());
						o.writeInt(channelProtocols.get(e.getKey()));
					}
				});
				section(out, SECTION_MATCH, o -> {
					o.writeBoolean(hasMatch);
					o.writeBoolean(hasContent);
					o.writeShort(matchHash.length);
					o.write(matchHash);
				});
			} else {
				section(out, SECTION_ACCEPTED, o -> {
					o.writeShort(acceptedChannels.size());
					for (int n : acceptedChannels) o.writeShort(n);
				});
			}
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Hello decode(byte[] data) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		Hello h = new Hello();
		h.kind = in.readUnsignedByte();
		h.protocol = in.readUnsignedShort();
		h.accepted = in.readBoolean();
		while (true) {
			int id = in.read();
			if (id < 0) break;
			int length = in.readInt();
			if (length < 0 || length > in.available()) throw new EOFException("A hello section longer than the hello");
			byte[] body = new byte[length];
			in.readFully(body);
			DataInputStream s = new DataInputStream(new ByteArrayInputStream(body));
			if (id == SECTION_MODS) {
				int n = count(s);
				for (int i = 0; i < n; i++) {
					String mod = s.readUTF(), version = s.readUTF();
					int flags = s.readUnsignedByte();
					h.mods.add(new ModInfo(mod, version, (flags & 1) != 0, (flags & 2) != 0, s.readInt()));
				}
			} else if (id == SECTION_CHANNELS) {
				int n = count(s);
				for (int i = 0; i < n; i++) {
					int number = s.readUnsignedShort();
					h.channels.put(number, s.readUTF());
					h.channelProtocols.put(number, s.readInt());
				}
			} else if (id == SECTION_MATCH) {
				h.hasMatch = s.readBoolean();
				h.hasContent = s.readBoolean();
				int n = s.readUnsignedShort();
				if (n > 64) throw new IOException("A match hash of " + n + " bytes");
				h.matchHash = new byte[n];
				s.readFully(h.matchHash);
			} else if (id == SECTION_ACCEPTED) {
				int n = count(s);
				for (int i = 0; i < n; i++) h.acceptedChannels.add(s.readUnsignedShort());
			}
		}
		return h;
	}

	private static int count(DataInputStream in) throws IOException {
		int n = in.readUnsignedShort();
		if (n > MAX_ENTRIES) throw new IOException("A hello list of " + n + " entries");
		return n;
	}

	/**
	 * What differs between two mod lists, only what must match: as lines for a person, by the mods' names ({@code name} of an
	 * id), and as lines for the log, by id and version.
	 */
	public static List<String> differences(List<ModInfo> server, List<ModInfo> client, java.util.function.Function<String, String> name, List<String> log) {
		List<String> lines = new ArrayList<>();
		for (ModInfo s : server) {
			if (!s.mustMatch()) continue;
			ModInfo c = find(client, s.id);
			if (c == null) {
				lines.add("The server needs: " + name.apply(s.id));
				log.add("Missing: " + s.id + " " + s.version);
			} else if (c.network != s.network) {
				lines.add("Needs another version: " + name.apply(s.id) + " (yours " + c.version + ", the server's " + s.version + ")");
				log.add("Different: " + s.id + " " + c.version + " here, " + s.version + " on the server");
			}
		}
		for (ModInfo c : client) {
			if (c.mustMatch() && find(server, c.id) == null) {
				lines.add("Turn off to join: " + name.apply(c.id));
				log.add("Extra: " + c.id + " " + c.version);
			}
		}
		if (lines.isEmpty()) {
			lines.add("The mods look the same, but what they add differs");
			log.add("Same mods, different content hashes");
		}
		return lines;
	}

	/** {@code blue-flowers} as Blue Flowers, for a mod nothing here has a name for. */
	public static String titled(String id) {
		StringBuilder out = new StringBuilder();
		for (String word : id.split("[-_]")) {
			if (word.isEmpty()) continue;
			if (out.length() > 0) out.append(' ');
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.length() > 0 ? out.toString() : id;
	}

	private static ModInfo find(List<ModInfo> mods, String id) {
		for (ModInfo m : mods) {
			if (m.id.equals(id)) return m;
		}
		return null;
	}
}
