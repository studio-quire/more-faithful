package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * Which boundary poles golems are using, so a pole in use glows and gives a lit redstone torch's light (the user's ask, 2026-11-10): each golem's rectangle by its own number,
 * kept with the world in beta's data folder as maps are ({@code data/golems_poles.dat}, the Nether's {@code golems_poles_-1.dat}), so a golem whose chunk isn't loaded still
 * holds its poles. A pole is in use while some golem's rectangle has a corner in its column, at any height, as {@link BoundaryPole#rectangleAround} found the rectangle: a
 * golem that has lost a pole still works the rectangle it was handed its tool in, so the others stay lit and a pole put back there lights. A golem holds its rectangle from
 * being handed a tool until it's taken back, it's handed one elsewhere or it dies; one that comes back from the disk says again what it holds, so a world from before this
 * file has its poles lit as its golems load.
 * <p>
 * Each change marks the corner columns, and poles are put right at the end of the tick where they're loaded, each switched only if it's wrong, so golems sharing a pole
 * never make it flicker and a big rectangle costs four poles' light. A pole whose chunk isn't loaded is put right when it loads, and one placed when it's placed.
 */
public final class PoleClaims extends PersistentState {
	private static final Map<World, PoleClaims> OPEN = new WeakHashMap<>();
	private static final Map<World, Set<Long>> PENDING = new WeakHashMap<>();

	/** Each golem's corners: the lower x and z, then the upper. */
	private final Map<Long, int[]> claims = new HashMap<>();

	/** Beta makes one by its name to read it from the disk. */
	public PoleClaims(String id) {
		super(id);
	}

	static void init() {
		Chunks.onLoaded(chunk -> {
			byte[] list = Golems.poles.get(chunk);
			if (list == null) return;
			for (int i = 0; i + 1 < list.length; i += 2) mark(chunk.world, chunk.x << 4 | (list[i] >> 4 & 15), chunk.z << 4 | (list[i] & 15));
		});
		Ticks.onWorld(PoleClaims::settle);
	}

	private static PoleClaims of(World world) {
		PoleClaims open = OPEN.get(world);
		if (open != null) return open;
		String id = world.dimension.id == 0 ? "golems_poles" : "golems_poles_" + world.dimension.id;
		open = (PoleClaims) world.getOrCreateState(PoleClaims.class, id);
		if (open == null) {
			open = new PoleClaims(id);
			world.setState(id, open);
		}
		OPEN.put(world, open);
		return open;
	}

	/** This golem's poles are now its area's corners, or none for an area round its post or none at all. Where the world runs. */
	static void hold(World world, long golem, Area area) {
		PoleClaims c = of(world);
		int[] was = c.claims.get(golem);
		int[] now = area != null && area.marked ? new int[] {area.minX - 1, area.minZ - 1, area.maxX + 1, area.maxZ + 1} : null;
		if (Arrays.equals(was, now)) return;
		if (now == null) c.claims.remove(golem);
		else c.claims.put(golem, now);
		c.markDirty();
		if (was != null) markCorners(world, was);
		if (now != null) markCorners(world, now);
	}

	static void release(World world, long golem) {
		hold(world, golem, null);
	}

	/** Whether some golem's rectangle has a corner in this column. */
	static boolean inUse(World world, int x, int z) {
		for (int[] r : of(world).claims.values()) {
			if ((x == r[0] || x == r[2]) && (z == r[1] || z == r[3])) return true;
		}
		return false;
	}

	/** A column to put right at the end of the tick. */
	static void mark(World world, int x, int z) {
		PENDING.computeIfAbsent(world, w -> new HashSet<>()).add((long) x << 32 | (z & 0xFFFFFFFFL));
	}

	private static void markCorners(World world, int[] r) {
		mark(world, r[0], r[1]);
		mark(world, r[2], r[1]);
		mark(world, r[0], r[3]);
		mark(world, r[2], r[3]);
	}

	/** Each marked column's poles lit or plain as they should be, where they're loaded; the rest wait for their chunk. Switching one marks its column again, which finds it right. */
	private static void settle(World world) {
		Set<Long> marked = PENDING.get(world);
		if (marked == null || marked.isEmpty()) return;
		List<Long> columns = new ArrayList<>(marked);
		marked.clear();
		int plain = Golems.pole.get().id, lit = Golems.poleLit.get().id;
		for (long column : columns) {
			int x = (int) (column >> 32), z = (int) column;
			if (!Chunks.isLoaded(world, x >> 4, z >> 4)) continue;
			Chunk chunk = world.getChunk(x >> 4, z >> 4);
			byte[] list = Golems.poles.get(chunk);
			if (list == null) continue;
			int want = inUse(world, x, z) ? lit : plain;
			byte xz = (byte) ((x & 15) << 4 | (z & 15));
			for (int i = 0; i + 1 < list.length; i += 2) {
				if (list[i] != xz) continue;
				int y = list[i + 1] & 0xFF, id = world.getBlockId(x, y, z);
				if (BoundaryPole.is(id) && id != want) world.setBlock(x, y, z, want);
			}
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		NbtList list = nbt.getList("Golems");
		for (int i = 0; i < list.size(); i++) {
			NbtCompound g = (NbtCompound) list.get(i);
			claims.put(g.getLong("Golem"), new int[] {g.getInt("X0"), g.getInt("Z0"), g.getInt("X1"), g.getInt("Z1")});
		}
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		NbtList list = new NbtList();
		for (Map.Entry<Long, int[]> e : claims.entrySet()) {
			NbtCompound g = new NbtCompound();
			g.putLong("Golem", e.getKey());
			g.putInt("X0", e.getValue()[0]);
			g.putInt("Z0", e.getValue()[1]);
			g.putInt("X1", e.getValue()[2]);
			g.putInt("Z1", e.getValue()[3]);
			list.add(g);
		}
		nbt.put("Golems", list);
	}
}
