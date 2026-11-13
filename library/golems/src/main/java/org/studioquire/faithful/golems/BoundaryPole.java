package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.BlockShapes;
import org.studioquire.oldfaithful.core.api.Chunks;

/**
 * A thin iron pole banded with redstone, standing on the ground (in place of the first build's cobblestone stake: the golem's own kit of iron and redstone). Four
 * at a rectangle's corners mark the space a golem works in: what's inside, the poles' own rows left out. Each chunk keeps where its poles are ({@link Golems#poles}), added as one is
 * placed and taken off as it goes, so a golem finds them without reading the land.
 * <p>
 * Two blocks, plain and lit, as beta's redstone torch is two: beta gives a block its light by its number, not its meta. The lit one is a pole a golem is using
 * ({@link PoleClaims}), with a lit redstone torch's light and its own tiles; it has no item and drops the plain one's, and both count as the same pole everywhere.
 */
public final class BoundaryPole extends Block {
	/** A rod 2 pixels square and 10 high, a torch's size, as Fable drew it (its side tile has the rod in its lower ten rows, as beta maps a short box onto a tile); its ring the top two. */
	public static final float LOW = 7.0F / 16.0F, HIGH = 9.0F / 16.0F, TOP = 10.0F / 16.0F, RING = 8.0F / 16.0F;
	/** How a lit pole is drawn: as beta draws a lit torch (client/LitPoleShape). */
	public static final String LIT_SHAPE = "golems:lit_pole";
	/** Iron to look at and hear, but punched loose by hand: it's a marker that gets moved about, and mustn't need a pick to pick up. */
	private static final Material MARKER = new Material(MapColor.SILVER);

	private final int sideTexture, topTexture, bitsTexture;
	private final boolean lit;

	BoundaryPole(int id, int side, int top, int bits, boolean lit) {
		super(id, side, MARKER);
		sideTexture = side;
		topTexture = top;
		bitsTexture = bits;
		this.lit = lit;
		setBoundingBox(LOW, 0.0F, LOW, HIGH, TOP, HIGH);
		setHardness(0.5F);
		setSoundGroup(METAL_SOUND_GROUP);
		// A lit redstone torch's, 7.
		if (lit) setLuminance(0.5F);
	}

	/** Either pole, plain or lit. */
	public static boolean is(int id) {
		return id != 0 && (id == Golems.pole.get().id || id == Golems.poleLit.get().id);
	}

	@Override
	public int getDroppedItemId(int meta, java.util.Random random) {
		return Golems.pole.get().id;
	}

	/**
	 * The bottom's is its bits' tile, mostly iron with a little red: beta cuts the pieces of a block being hit or broken out of its bottom tile, and the bottom itself is never seen,
	 * as a pole stands on a solid block.
	 */
	@Override
	public int getTexture(int side) {
		return side == 0 ? bitsTexture : side == 1 ? topTexture : sideTexture;
	}

	@Override
	public int getRenderType() {
		// Beta's 0 is any block's box.
		return lit ? BlockShapes.type(LIT_SHAPE) : 0;
	}

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	public boolean isFullCube() {
		return false;
	}

	@Override
	public boolean canPlaceAt(World world, int x, int y, int z) {
		return world.shouldSuffocate(x, y - 1, z);
	}

	@Override
	public void neighborUpdate(World world, int x, int y, int z, int id) {
		if (world.shouldSuffocate(x, y - 1, z)) return;
		dropStacks(world, x, y, z, world.getBlockMeta(x, y, z));
		world.setBlock(x, y, z, 0);
	}

	@Override
	public void onPlaced(World world, int x, int y, int z) {
		if (world.isRemote) return;
		Chunk chunk = world.getChunkFromPos(x, z);
		byte[] had = Golems.poles.get(chunk);
		if (had != null && indexOf(had, x, y, z) >= 0) {
			PoleClaims.mark(world, x, z);
			return;
		}
		byte[] now = had == null ? new byte[2] : Arrays.copyOf(had, had.length + 2);
		now[now.length - 2] = (byte) ((x & 15) << 4 | (z & 15));
		now[now.length - 1] = (byte) y;
		Golems.poles.set(chunk, now);
		PoleClaims.mark(world, x, z);
	}

	@Override
	public void onBreak(World world, int x, int y, int z) {
		if (world.isRemote) return;
		Chunk chunk = world.getChunkFromPos(x, z);
		byte[] had = Golems.poles.get(chunk);
		int at = had == null ? -1 : indexOf(had, x, y, z);
		if (at < 0) return;
		byte[] now = new byte[had.length - 2];
		System.arraycopy(had, 0, now, 0, at);
		System.arraycopy(had, at + 2, now, at, had.length - at - 2);
		Golems.poles.set(chunk, now.length == 0 ? null : now);
	}

	private static int indexOf(byte[] list, int x, int y, int z) {
		byte xz = (byte) ((x & 15) << 4 | (z & 15));
		for (int i = 0; i + 1 < list.length; i += 2) {
			if (list[i] == xz && (list[i + 1] & 0xFF) == y) return i;
		}
		return -1;
	}

	/** Every pole within {@code range} of a place, each way across the land, at any height: x, y, z each. Only from loaded chunks, and checked against the world. */
	static List<int[]> near(World world, int x, int z, int range) {
		List<int[]> found = new ArrayList<>();
		for (int cx = (x - range) >> 4; cx <= (x + range) >> 4; cx++) {
			for (int cz = (z - range) >> 4; cz <= (z + range) >> 4; cz++) {
				if (!Chunks.isLoaded(world, cx, cz)) continue;
				byte[] list = Golems.poles.get(world.getChunk(cx, cz));
				if (list == null) continue;
				for (int i = 0; i + 1 < list.length; i += 2) {
					int sx = cx << 4 | (list[i] >> 4 & 15), sz = cz << 4 | (list[i] & 15), sy = list[i + 1] & 0xFF;
					if (Math.abs(sx - x) > range || Math.abs(sz - z) > range || !is(world.getBlockId(sx, sy, sz))) continue;
					found.add(new int[] {sx, sy, sz});
				}
			}
		}
		return found;
	}

	/**
	 * The smallest rectangle of four poles, one at each corner, with this place strictly inside it, or null if there's none. Its corners: the pole of each, in {@code corners} if given.
	 * Poles of neighbouring rectangles can be shared, and one rectangle inside another works too: a golem takes the smallest round its post.
	 */
	static Area rectangleAround(World world, int x, int z, int range, List<int[]> corners) {
		List<int[]> all = near(world, x, z, range);
		Set<Long> columns = new HashSet<>();
		for (int[] s : all) columns.add(column(s[0], s[2]));
		int[] best = null;
		long bestSize = Long.MAX_VALUE;
		for (int[] a : all) {
			for (int[] b : all) {
				// a the low corner, b the high one.
				if (!(a[0] < x && x < b[0] && a[2] < z && z < b[2])) continue;
				if (!columns.contains(column(a[0], b[2])) || !columns.contains(column(b[0], a[2]))) continue;
				long size = (long) (b[0] - a[0]) * (b[2] - a[2]);
				if (size >= bestSize) continue;
				bestSize = size;
				best = new int[] {a[0], a[2], b[0], b[2]};
			}
		}
		if (best == null) return null;
		if (corners != null) {
			for (int[] s : all) {
				if ((s[0] == best[0] || s[0] == best[2]) && (s[2] == best[1] || s[2] == best[3])) corners.add(s);
			}
		}
		return new Area(best[0] + 1, best[1] + 1, best[2] - 1, best[3] - 1, true);
	}

	private static long column(int x, int z) {
		return (long) x << 32 | (z & 0xFFFFFFFFL);
	}
}
