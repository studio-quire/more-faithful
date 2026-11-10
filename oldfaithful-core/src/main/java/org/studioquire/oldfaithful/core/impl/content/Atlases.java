package org.studioquire.oldfaithful.core.impl.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where mods' tiles go: a texture index is {@code atlas << 8 | tile}, atlas 0 being vanilla's sheet, so vanilla's masks split it
 * as they stand. Pure ints: {@code getTexture} runs on the server too. A block's tiles all land in one atlas, so one bind draws it.
 */
public final class Atlases {
	public static final class Tile {
		public final String mod, kind, name; // kind "vanilla": name holds the terrain tile
		public final int index;
		Tile(String mod, String kind, String name, int index) {
			this.mod = mod;
			this.kind = kind;
			this.name = name;
			this.index = index;
		}
	}

	private static final int MAX_BLOCK_TILES = 16;
	private static final Map<String, Tile> TILES = new LinkedHashMap<>();
	private static int atlas = 1;
	private static int next;

	private Atlases() {
	}

	/** A block's tiles stay together: start a new atlas if this one hasn't room for the most a block may have. */
	static void beginBlock() {
		if (next + MAX_BLOCK_TILES > 256) {
			atlas++;
			next = 0;
		}
	}

	private static int allocate() {
		if (next >= 256) {
			atlas++;
			next = 0;
		}
		return atlas << 8 | next++;
	}

	static int tile(String mod, String kind, String name) {
		String key = mod + "/" + kind + "/" + name;
		Tile t = TILES.get(key);
		if (t != null && t.index >> 8 == atlas) return t.index;
		t = new Tile(mod, kind, name, allocate());
		TILES.put(key + "#" + (t.index >> 8), t);
		TILES.putIfAbsent(key, t);
		return t.index;
	}

	static int vanillaTile(int tile) {
		String key = "vanilla/" + tile + "#" + atlas;
		Tile t = TILES.get(key);
		if (t == null) {
			t = new Tile(null, "vanilla", String.valueOf(tile), allocate());
			TILES.put(key, t);
		}
		return t.index;
	}

	static void finish() {
	}

	public static int count() {
		return next == 0 ? atlas - 1 : atlas;
	}

	/** Every tile, each once. */
	public static List<Tile> tiles() {
		List<Tile> tiles = new ArrayList<>();
		for (Tile t : TILES.values()) {
			if (!tiles.contains(t)) tiles.add(t);
		}
		return tiles;
	}
}
