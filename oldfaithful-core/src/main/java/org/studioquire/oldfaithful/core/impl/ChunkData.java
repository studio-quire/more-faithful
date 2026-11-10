package org.studioquire.oldfaithful.core.impl;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.chunk.Chunk;

/**
 * What a chunk keeps under {@code Level.Faithful}. The compound stays on the chunk from load to save, so what nobody here
 * understands (a removed mod's blocks, a later version's keys) goes back out as it came in. Sections read and write their own
 * keys in it, in order of name: at load straight after the block arrays are read, before vanilla repairs the height map or
 * adds block entities, and at save after vanilla's own fields. A chunk with nothing to say writes no key at all.
 */
public final class ChunkData {
	public static final String KEY = "Faithful";

	public interface Section {
		void load(Chunk chunk, NbtCompound faithful);

		void save(Chunk chunk, NbtCompound faithful);
	}

	/** Implemented by the game's Chunk. */
	public interface Holder {
		NbtElement faithful$data();

		void faithful$setData(NbtElement data);
	}

	private static final Map<String, Section> SECTIONS = new TreeMap<>();

	private ChunkData() {
	}

	public static synchronized void section(String name, Section section) {
		if (SECTIONS.putIfAbsent(name, section) != null) throw new IllegalStateException("The chunk section " + name + " is registered twice");
	}

	/** Takes a key out of a compound: beta's compounds have no call for it. */
	public static void remove(NbtCompound nbt, String key) {
		nbt.values().removeIf(e -> ((NbtElement) e).getKey().equals(key));
	}

	static NbtElement find(NbtCompound nbt, String key) {
		for (Object e : nbt.values()) {
			if (((NbtElement) e).getKey().equals(key)) return (NbtElement) e;
		}
		return null;
	}

	/** The core's: inside {@code loadChunkFromNbt}. */
	public static void load(Chunk chunk, NbtCompound level) {
		NbtElement data = find(level, KEY);
		if (data != null) ((Holder) chunk).faithful$setData(data);
		if (data != null && !(data instanceof NbtCompound)) {
			// A compound's reader would throw, and vanilla answers that by regenerating the chunk.
			System.out.println("[faithful-core] WARN The chunk at " + chunk.x + ", " + chunk.z + " has a Faithful entry that isn't a compound: kept as it is, and not read");
			return;
		}
		if (SECTIONS.isEmpty()) return;
		NbtCompound faithful = data != null ? (NbtCompound) data : new NbtCompound();
		for (Map.Entry<String, Section> s : SECTIONS.entrySet()) {
			try {
				s.getValue().load(chunk, faithful);
			} catch (RuntimeException e) {
				System.out.println("[faithful-core] WARN The chunk at " + chunk.x + ", " + chunk.z + ": its section " + s.getKey() + " wouldn't load, and is kept as it is: " + e);
			}
		}
	}

	/** The core's: at the end of {@code saveChunkToNbt}. */
	public static void save(Chunk chunk, NbtCompound level) {
		Holder holder = (Holder) chunk;
		NbtElement data = holder.faithful$data();
		if (data != null && !(data instanceof NbtCompound)) {
			level.put(KEY, data);
			return;
		}
		NbtCompound faithful = data != null ? (NbtCompound) data : new NbtCompound();
		for (Map.Entry<String, Section> s : SECTIONS.entrySet()) {
			try {
				s.getValue().save(chunk, faithful);
			} catch (RuntimeException e) {
				System.out.println("[faithful-core] WARN The chunk at " + chunk.x + ", " + chunk.z + ": its section " + s.getKey() + " wouldn't save: " + e);
			}
		}
		if (faithful.values().isEmpty()) {
			holder.faithful$setData(null);
			return;
		}
		holder.faithful$setData(faithful);
		level.put(KEY, faithful);
	}
}
