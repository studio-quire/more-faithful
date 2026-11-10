package org.studioquire.faithful.buriedtowers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.carver.CaveCarver;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import org.studioquire.faithful.buriedtowers.plan.Climate;
import org.studioquire.faithful.buriedtowers.plan.Ground;

/**
 * Beta's terrain worked out on the side: a generator of our own, seeded as the world's is, asked only for the land's shape. It makes no chunk and never calls the generator's {@code getChunk}, so no
 * mod's feature hears of it, and a site's ground is known from any chunk, in any order, before its neighbours exist. The one thing it shares with the world is the biome source, whose fields are
 * scratch space between one call and the next: they're put back as they were. For {@link #surface} it goes on as {@code getChunk} does, the surface and then the caves, with the generator's random
 * seeded for the chunk as there: the surface's sand and gravel aren't cut by a cave, and its water stops one. That second part waits until a surface is asked for, as finding a site needs only
 * {@link #top}: it reads nothing but the chunk's own terrain, its biomes and the random seeded afresh, so it comes out the same whenever it's done.
 */
final class BetaGround implements Ground {
	/** Enough for a tower's whole square, so a tower reads no chunk twice; chunks are dropped when it's full. */
	private static final int KEPT = 64;

	private final World world;
	private final OverworldChunkGenerator generator;
	private final Random random;
	private final CaveCarver caves = new CaveCarver();
	private final Map<Long, Land> lands = new HashMap<>();
	/** The chunk asked about last: a tower reads a column many times over, and its neighbours next. */
	private long lastKey = Long.MIN_VALUE;
	private Land last;

	/** A chunk's land: each column's {@link #top}, and its {@link #surface} once asked for, until then the terrain and biomes to work it out from. */
	private static final class Land {
		final byte[] tops = new byte[256];
		byte[] surfaces;
		byte[] terrain;
		Biome[] biomes;
	}

	BetaGround(World world) {
		this.world = world;
		generator = new OverworldChunkGenerator(world, world.getSeed());
		random = generatorRandom(generator);
	}

	/** The generator's own random, its only one, which the surface draws from. */
	private static Random generatorRandom(OverworldChunkGenerator generator) {
		try {
			for (Field f : OverworldChunkGenerator.class.getDeclaredFields()) {
				if (f.getType() != Random.class) continue;
				f.setAccessible(true);
				return (Random) f.get(generator);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Buried Towers can't reach the terrain generator's random", e);
		}
		throw new IllegalStateException("Buried Towers found no random in the terrain generator");
	}

	@Override
	public int top(int x, int z) {
		return land(x, z).tops[(x & 15) * 16 + (z & 15)] & 0xFF;
	}

	@Override
	public int surface(int x, int z) {
		Land land = land(x, z);
		if (land.surfaces == null) surface(land, x >> 4, z >> 4);
		return land.surfaces[(x & 15) * 16 + (z & 15)] & 0xFF;
	}

	/** Whether a column's chunk is worked out already, so reading it costs nothing. */
	boolean has(int x, int z) {
		long key = (long) (x >> 4) << 32 | z >> 4 & 0xFFFFFFFFL;
		return key == lastKey || lands.containsKey(key);
	}

	private Land land(int x, int z) {
		long key = (long) (x >> 4) << 32 | z >> 4 & 0xFFFFFFFFL;
		if (key == lastKey) return last;
		Land land = lands.get(key);
		if (land == null) {
			if (lands.size() >= KEPT) lands.clear();
			land = terrain(x >> 4, z >> 4);
			lands.put(key, land);
		}
		lastKey = key;
		last = land;
		return land;
	}

	private Land terrain(int chunkX, int chunkZ) {
		BiomeSource source = world.method_1781();
		double[] temperature = source.temperatureMap, downfall = source.downfallMap, weirdness = source.weirdnessMap;
		Biome[] biomes = source.biomes;
		try {
			source.temperatureMap = null;
			source.downfallMap = null;
			source.weirdnessMap = null;
			Land land = new Land();
			land.biomes = source.getBiomesInArea(null, chunkX * 16, chunkZ * 16, 16, 16);
			land.terrain = new byte[32768];
			generator.buildTerrain(chunkX, chunkZ, land.terrain, land.biomes, source.temperatureMap);
			int stone = Block.STONE.id;
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					int y = 127;
					int base = x << 11 | z << 7;
					while (y > 0 && land.terrain[base + y] != stone) y--;
					land.tops[x * 16 + z] = (byte) (y + 1);
				}
			}
			return land;
		} finally {
			source.temperatureMap = temperature;
			source.downfallMap = downfall;
			source.weirdnessMap = weirdness;
			source.biomes = biomes;
		}
	}

	private void surface(Land land, int chunkX, int chunkZ) {
		byte[] blocks = land.terrain;
		random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
		generator.buildSurfaces(chunkX, chunkZ, blocks, land.biomes);
		caves.carve(generator, world, chunkX, chunkZ, blocks);
		byte[] surfaces = new byte[256];
		int water = Block.WATER.id, flowing = Block.FLOWING_WATER.id;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int y = 127;
				int base = x << 11 | z << 7;
				while (y > 0 && (blocks[base + y] == 0 || blocks[base + y] == water || blocks[base + y] == flowing)) y--;
				surfaces[x * 16 + z] = (byte) (y + 1);
			}
		}
		land.surfaces = surfaces;
		land.terrain = null;
		land.biomes = null;
	}

	Climate climate(int x, int z) {
		BiomeSource source = world.method_1781();
		double[] temperature = source.temperatureMap, downfall = source.downfallMap, weirdness = source.weirdnessMap;
		Biome[] biomes = source.biomes;
		try {
			source.temperatureMap = null;
			source.downfallMap = null;
			source.weirdnessMap = null;
			Biome biome = source.getBiomesInArea(null, x, z, 1, 1)[0];
			if (biome == Biome.DESERT) return Climate.DESERT;
			if (biome == Biome.TAIGA || biome == Biome.TUNDRA || biome == Biome.ICE_DESERT) return Climate.COLD;
			if (biome == Biome.SWAMPLAND || biome == Biome.RAINFOREST) return Climate.WET;
			if (biome == Biome.FOREST || biome == Biome.SEASONAL_FOREST) return Climate.WOODED;
			return Climate.OPEN;
		} finally {
			source.temperatureMap = temperature;
			source.downfallMap = downfall;
			source.weirdnessMap = weirdness;
			source.biomes = biomes;
		}
	}
}
