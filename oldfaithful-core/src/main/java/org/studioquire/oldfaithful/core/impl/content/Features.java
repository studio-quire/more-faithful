package org.studioquire.oldfaithful.core.impl.content;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.block.SandBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** The registered features, in order of name, so what one mod adds doesn't change the order anything else runs in. */
public final class Features {
	private static final class Placed<T> {
		final int dimension;
		final T feature;

		Placed(int dimension, T feature) {
			this.dimension = dimension;
			this.feature = feature;
		}
	}

	private static final Map<String, Placed<Worldgen.ChunkFeature>> BUILT = new TreeMap<>();
	private static final Map<String, Placed<Worldgen.Decoration>> DECORATIONS = new TreeMap<>();

	private Features() {
	}

	private static void check(String name) {
		if (name.indexOf(':') <= 0) throw new IllegalArgumentException("A feature's name is a full one: " + name);
		if (!ContentMixins.active()) throw new IllegalStateException("Worldgen needs the content layer: say \"custom\": {\"faithful\": {\"content\": true}} in fabric.mod.json");
	}

	public static void chunkBuilt(String name, int dimension, Worldgen.ChunkFeature feature) {
		check(name);
		if (BUILT.putIfAbsent(name, new Placed<>(dimension, feature)) != null) throw new IllegalStateException("The feature " + name + " is registered twice");
	}

	public static void decorate(String name, int dimension, Worldgen.Decoration decoration) {
		check(name);
		if (DECORATIONS.putIfAbsent(name, new Placed<>(dimension, decoration)) != null) throw new IllegalStateException("The feature " + name + " is registered twice");
	}

	/** A feature's own stream for a chunk: the mixing keeps neighbouring chunks and similar names from starting alike. */
	static Random random(long seed, int chunkX, int chunkZ, String name) {
		long h = seed;
		for (int i = 0; i < name.length(); i++) h = h * 1099511628211L + name.charAt(i);
		h += chunkX * 341873128712L + chunkZ * 132897987541L;
		h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
		h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
		return new Random(h ^ h >>> 31);
	}

	/** A chunk just made, by any generator: its cells are final but for this, and its height map and light are still to come. */
	public static void built(World world, Chunk chunk) {
		if (BUILT.isEmpty()) return;
		Cells cells = new Cells(world, chunk);
		for (Map.Entry<String, Placed<Worldgen.ChunkFeature>> e : BUILT.entrySet()) {
			if (e.getValue().dimension != world.dimension.id) continue;
			try {
				e.getValue().feature.build(cells, random(world.getSeed(), chunk.x, chunk.z, e.getKey()));
			} catch (RuntimeException t) {
				failed(e.getKey(), chunk.x, chunk.z, t);
			}
		}
	}

	/** At the end of a generator's {@code decorate}. Falling blocks land at once here, as they do in vanilla's. */
	public static void decorate(World world, int chunkX, int chunkZ) {
		if (DECORATIONS.isEmpty()) return;
		SandBlock.fallInstantly = true;
		for (Map.Entry<String, Placed<Worldgen.Decoration>> e : DECORATIONS.entrySet()) {
			if (e.getValue().dimension != world.dimension.id) continue;
			try {
				e.getValue().feature.decorate(world, random(world.getSeed(), chunkX, chunkZ, e.getKey()), chunkX * 16 + 8, chunkZ * 16 + 8);
			} catch (RuntimeException t) {
				failed(e.getKey(), chunkX, chunkZ, t);
			}
		}
		SandBlock.fallInstantly = false;
	}

	/** A chunk with one feature missing beats a server down, and a world losing the chunk. */
	private static void failed(String feature, int chunkX, int chunkZ, RuntimeException t) {
		System.out.println("[faithful-core] WARN The feature " + feature + " failed in the chunk at " + chunkX + ", " + chunkZ + ", and was skipped: " + t);
		t.printStackTrace(System.out);
	}

	/** A vein: a short wander of small blobs, kept inside what decoration may touch. */
	public static void vein(World world, Random random, int originX, int originZ, int x, int y, int z, int ore, int replaces, int size) {
		double px = x, py = y, pz = z;
		double dx = random.nextDouble() - 0.5, dy = (random.nextDouble() - 0.5) / 2, dz = random.nextDouble() - 0.5;
		int placed = 0;
		for (int step = 0; step < size && placed < size; step++) {
			double radius = 0.7 + random.nextDouble();
			int reach = (int) Math.ceil(radius);
			for (int ox = -reach; ox <= reach; ox++) {
				for (int oy = -reach; oy <= reach; oy++) {
					for (int oz = -reach; oz <= reach; oz++) {
						if (placed >= size || ox * ox + oy * oy + oz * oz > radius * radius) continue;
						int cx = (int) Math.floor(px) + ox, cy = (int) Math.floor(py) + oy, cz = (int) Math.floor(pz) + oz;
						if (cx < originX - 8 || cx >= originX + 24 || cz < originZ - 8 || cz >= originZ + 24 || cy < 1 || cy > 126) continue;
						if (world.getBlockId(cx, cy, cz) == replaces) {
							world.setBlockWithoutNotifyingNeighbors(cx, cy, cz, ore);
							placed++;
						}
					}
				}
			}
			px += dx * 2;
			py += dy * 2;
			pz += dz * 2;
		}
	}

	private static final class Cells implements Worldgen.Cells {
		private final World world;
		private final Chunk chunk;

		Cells(World world, Chunk chunk) {
			this.world = world;
			this.chunk = chunk;
		}

		@Override
		public World world() {
			return world;
		}

		@Override
		public Chunk chunk() {
			return chunk;
		}

		@Override
		public int chunkX() {
			return chunk.x;
		}

		@Override
		public int chunkZ() {
			return chunk.z;
		}

		@Override
		public int get(int x, int y, int z) {
			return chunk.getBlockId(x, y, z);
		}

		@Override
		public int meta(int x, int y, int z) {
			return chunk.getBlockMeta(x, y, z);
		}

		@Override
		public void set(int x, int y, int z, int block) {
			set(x, y, z, block, 0);
		}

		@Override
		public void set(int x, int y, int z, int block, int meta) {
			Overlay.Holder holder = Overlay.of(chunk);
			int cell = x << 11 | z << 7 | y;
			char[] ext = holder.faithful$ext();
			if (Overlay.modded(block)) {
				if (ext == null) {
					ext = new char[Overlay.CELLS];
					holder.faithful$setExt(ext);
				}
				ext[cell] = (char) (block | (meta & 15) << 12);
			} else if (ext != null) {
				ext[cell] = 0;
			}
			chunk.blocks[cell] = (byte) Overlay.standId(block);
			chunk.meta.set(x, y, z, Overlay.standMeta(block, meta));
		}
	}
}
