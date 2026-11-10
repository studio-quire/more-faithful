package org.studioquire.faithful.buriedtowers.plan;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/** A tower's cells, whole: what each of them should end up as, in world coordinates, with nothing read from the world. A cell it doesn't speak for is {@link #KEEP}. */
public final class Plan {
	public static final int KEEP = -1;
	public static final int HEIGHT = 128;

	/** The square of columns covered. */
	public final int ox, oz, size;
	private final short[] cells;
	private final BitSet reserved;
	private final BitSet rock;
	private final BitSet loose;
	/** What a chest, spawner, sign and the like hold, by cell. */
	public final Map<Long, String> fixtures = new TreeMap<>();

	Plan(int ox, int oz, int size) {
		this.ox = ox;
		this.oz = oz;
		this.size = size;
		cells = new short[size * size * HEIGHT];
		Arrays.fill(cells, (short) KEEP);
		reserved = new BitSet(cells.length);
		rock = new BitSet(cells.length);
		loose = new BitSet(cells.length);
	}

	public boolean covers(int x, int y, int z) {
		return x >= ox && z >= oz && x < ox + size && z < oz + size && y >= 0 && y < HEIGHT;
	}

	private int index(int x, int y, int z) {
		return ((x - ox) * size + (z - oz)) * HEIGHT + y;
	}

	public static long key(int x, int y, int z) {
		return (long) (x + (1 << 25)) << 34 | (long) (z + (1 << 25)) << 8 | y;
	}

	public static int keyX(long key) {
		return (int) (key >>> 34) - (1 << 25);
	}

	public static int keyZ(long key) {
		return (int) (key >>> 8 & (1L << 26) - 1) - (1 << 25);
	}

	public static int keyY(long key) {
		return (int) (key & 0xFF);
	}

	/** The packed cell, or {@link #KEEP}, as it is for a cell past the plan. */
	public int get(int x, int y, int z) {
		return covers(x, y, z) ? cells[index(x, y, z)] : KEEP;
	}

	public int id(int x, int y, int z) {
		int c = get(x, y, z);
		return c == KEEP ? Blocks.AIR : Blocks.id(c);
	}

	public int meta(int x, int y, int z) {
		int c = get(x, y, z);
		return c == KEEP ? 0 : Blocks.meta(c);
	}

	/** Unless the cell is held for a way down. */
	public void set(int x, int y, int z, int cell) {
		if (covers(x, y, z) && !reserved.get(index(x, y, z))) {
			cells[index(x, y, z)] = (short) cell;
			rock.clear(index(x, y, z));
			loose.clear(index(x, y, z));
		}
	}

	/** Whatever the cell is held for. */
	public void force(int x, int y, int z, int cell) {
		if (covers(x, y, z)) {
			cells[index(x, y, z)] = (short) cell;
			rock.clear(index(x, y, z));
			loose.clear(index(x, y, z));
		}
	}

	/**
	 * A stone lying loose on the land outside, held up by nothing but what's under it: where the land under it is gone when the chunk's decorated (a lake dug after the tower was written), the
	 * keeper leaves it out, as it would have fallen in.
	 */
	public void loose(int x, int y, int z, int cell) {
		if (covers(x, y, z)) {
			force(x, y, z, cell);
			loose.set(index(x, y, z));
		}
	}

	public boolean isLoose(int x, int y, int z) {
		return covers(x, y, z) && loose.get(index(x, y, z));
	}

	/** Filling, on a floor that's solid until it's cut: it goes in a way's cells too, and the way, or a walk to it, can cut it again. */
	public void rock(int x, int y, int z, int cell) {
		if (covers(x, y, z)) {
			cells[index(x, y, z)] = (short) cell;
			rock.set(index(x, y, z));
			loose.clear(index(x, y, z));
		}
	}

	public boolean isRock(int x, int y, int z) {
		return covers(x, y, z) && rock.get(index(x, y, z));
	}

	public void hold(int x, int y, int z) {
		if (covers(x, y, z)) reserved.set(index(x, y, z));
	}

	public boolean held(int x, int y, int z) {
		return covers(x, y, z) && reserved.get(index(x, y, z));
	}

	public void fixture(int x, int y, int z, String value) {
		fixtures.put(key(x, y, z), value);
	}

	public String fixture(int x, int y, int z) {
		return fixtures.get(key(x, y, z));
	}
}
