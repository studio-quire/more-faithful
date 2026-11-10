package org.studioquire.oldfaithful.core.api;

import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.LogBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.DungeonFeature;
import org.studioquire.oldfaithful.core.impl.DungeonFeatureInvoker;
import org.studioquire.oldfaithful.core.impl.content.Features;

/**
 * Adding to new terrain. Each feature has a full name and a {@code Random} of its own, seeded from the world's seed, the chunk and that name, so adding a mod moves neither
 * vanilla's ores nor another mod's features. They run in order of name, after vanilla's. Register from {@code init}. The two passes need the content layer. Biomes and dimensions of a mod's own
 * aren't here yet. {@link #onDungeon} and the dungeon lists below are the base layer's, so a mod that only changes what's there needs nothing of a client.
 * <p>
 * Two passes. {@link #chunkBuilt} writes straight into a chunk that has just been made, before its height map and light: fast and simple, and it can't see its neighbours. {@link
 * #decorate} runs when vanilla decorates, with the whole world's calls to hand: trees, ores, anything that crosses a chunk's edge. It comes after vanilla's decorations, so tall grass and flowers are
 * already standing: {@link #surface} looks past them, and a tree should replace them.
 */
public final class Worldgen {
	public static final int OVERWORLD = 0, NETHER = -1, SKY = 1;

	/**
	 * A chunk that's just been made. Cells are in it, 0-15 and 0-127. No block entities yet, and nothing here lights up its surroundings, as vanilla's cave lava doesn't: a block
	 * that needs either comes from a decoration.
	 */
	public interface Cells {
		World world();

		/** The chunk itself, for what goes with its cells, like an attachment. It isn't in the world yet: nothing here can reach it through the world, and writing cells goes through this, not it. */
		Chunk chunk();

		int chunkX();

		int chunkZ();

		int get(int x, int y, int z);

		int meta(int x, int y, int z);

		void set(int x, int y, int z, int block);

		void set(int x, int y, int z, int block, int meta);
	}

	public interface ChunkFeature {
		void build(Cells cells, Random random);
	}

	public interface Decoration {
		/**
		 * Stay in {@code x..x+15}, {@code z..z+15}, plus up to 8 either side: past that, a neighbour may not exist yet, and asking makes it. A new world's spawn search decorates too, so on a server this
		 * can run before the world is finished: {@link Server#world} doesn't have it yet, and an entity added then dies at the first tick, as nothing could be tracking it.
		 */
		void decorate(World world, Random random, int x, int z);
	}

	/** Beta's own dungeon, as it was placed: its room is {@code x-halfX..x+halfX}, {@code y..y+3}, {@code z-halfZ..z+halfZ}, with its spawner at {@code x, y, z}. */
	public interface Dungeon {
		World world();

		int x();

		int y();

		int z();

		int halfX();

		int halfZ();
	}

	/** Asked before vanilla's generator tries for a dungeon somewhere. */
	public interface DungeonPlacing {
		World world();

		int x();

		int y();

		int z();

		void cancel();
	}

	private static final Listeners<Consumer<Dungeon>> DUNGEONS = new Listeners<>("Worldgen.onDungeon");
	private static final Listeners<Consumer<DungeonPlacing>> DUNGEON_PLACING = new Listeners<>("Worldgen.onDungeonPlacing");

	private Worldgen() {
	}

	/**
	 * Notice: vanilla's generator has just placed a dungeon, in either generator that makes them. This is the middle of a chunk's decoration: stay in what was just read out, and change no cell, as
	 * vanilla's next features will read them and their random stream goes by what they find. Note where it is and do the work later.
	 */
	public static void onDungeon(Consumer<Dungeon> listener) {
		DUNGEONS.add(listener);
	}

	/**
	 * Veto: vanilla's generator is about to try for a dungeon, which it does eight times a chunk, most finding no place. Cancelled, it still goes through the whole of it and makes all the same random
	 * draws, as though the room had been made, but no cell changes and {@link #onDungeon} isn't heard: so the ores and plants after it in that chunk's stream stay where they'd have been.
	 */
	public static void onDungeonPlacing(Consumer<DungeonPlacing> listener) {
		DUNGEON_PLACING.add(listener);
	}

	/** The core's. */
	public static boolean dungeonVetoed(World world, int x, int y, int z) {
		if (DUNGEON_PLACING.isEmpty()) return false;
		boolean[] cancelled = new boolean[1];
		DungeonPlacing placing = new DungeonPlacing() {
			@Override
			public World world() {
				return world;
			}

			@Override
			public int x() {
				return x;
			}

			@Override
			public int y() {
				return y;
			}

			@Override
			public int z() {
				return z;
			}

			@Override
			public void cancel() {
				cancelled[0] = true;
			}
		};
		DUNGEON_PLACING.each(l -> l.accept(placing));
		return cancelled[0];
	}

	/** The core's. */
	public static void dungeonPlaced(World world, int x, int y, int z, int halfX, int halfZ) {
		if (DUNGEONS.isEmpty()) return;
		Dungeon dungeon = new Dungeon() {
			@Override
			public World world() {
				return world;
			}

			@Override
			public int x() {
				return x;
			}

			@Override
			public int y() {
				return y;
			}

			@Override
			public int z() {
				return z;
			}

			@Override
			public int halfX() {
				return halfX;
			}

			@Override
			public int halfZ() {
				return halfZ;
			}
		};
		DUNGEONS.each(l -> l.accept(dungeon));
	}

	/** One draw from vanilla's dungeon chest list, which may come up empty, so null is an answer: beta rolls eight times a chest. */
	public static ItemStack dungeonChestItem(Random random) {
		return ((DungeonFeatureInvoker) new DungeonFeature()).faithful$chestItem(random);
	}

	/** One draw from vanilla's dungeon monsters: a name for {@code MobSpawnerBlockEntity.setSpawnedEntityId}. */
	public static String dungeonMonster(Random random) {
		return ((DungeonFeatureInvoker) new DungeonFeature()).faithful$monster(random);
	}

	/**
	 * For a decoration: the height of the cell just above the ground here, looking down past air, plants, leaves, logs and snow, where {@code getTopSolidBlockY} stops at the first leaf. Water and
	 * lava count as ground, so see what's below before planting. 0 if the column is empty.
	 */
	public static int surface(World world, int x, int z) {
		for (int y = 127; y > 0; y--) {
			int id = world.getBlockId(x, y, z);
			if (id == 0) continue;
			Block block = Block.BLOCKS[id];
			Material material = block.material;
			if (material == Material.PLANT || material == Material.LEAVES || material == Material.SNOW_LAYER || block instanceof LogBlock) continue;
			return y + 1;
		}
		return 0;
	}

	/** The biome of a column. Compare with {@code Biome.FOREST} and the rest; the same answer on both sides of a seed. */
	public static Biome biome(World world, int x, int z) {
		return world.method_1781().getBiome(x, z);
	}

	/** In the overworld. */
	public static void chunkBuilt(String name, ChunkFeature feature) {
		chunkBuilt(name, OVERWORLD, feature);
	}

	public static void chunkBuilt(String name, int dimension, ChunkFeature feature) {
		Features.chunkBuilt(name, dimension, feature);
	}

	/** In the overworld. */
	public static void decorate(String name, Decoration decoration) {
		decorate(name, OVERWORLD, decoration);
	}

	public static void decorate(String name, int dimension, Decoration decoration) {
		Features.decorate(name, dimension, decoration);
	}

	/**
	 * Veins of an ore in the overworld's stone. Vanilla's, as cells a vein, veins a chunk, below what height: coal 16, 20, 128; iron 8, 20, 64; gold 8, 2, 32; redstone 7, 8, 16; diamond 7, 1, 16;
	 * lapis 6, 1, around 16.
	 * @param veinsPerChunk may be a fraction: 0.25 is a vein in one chunk of four, and 2.5 is two and sometimes three
	 */
	public static void ore(String name, Block ore, int veinSize, float veinsPerChunk, int minY, int maxY) {
		ore(name, OVERWORLD, ore, Block.STONE, veinSize, veinsPerChunk, minY, maxY);
	}

	/** Veins in any dimension, replacing one block: netherrack, say. */
	public static void ore(String name, int dimension, Block ore, Block replaces, int veinSize, float veinsPerChunk, int minY, int maxY) {
		int whole = (int) veinsPerChunk;
		float part = veinsPerChunk - whole;
		decorate(name, dimension, (world, random, x, z) -> {
			int veins = part > 0.0F && random.nextFloat() < part ? whole + 1 : whole;
			for (int i = 0; i < veins; i++) {
				Features.vein(world, random, x, z, x + random.nextInt(16), minY + random.nextInt(Math.max(1, maxY - minY)), z + random.nextInt(16), ore.id, replaces.id, veinSize);
			}
		});
	}
}
