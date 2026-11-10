package org.studioquire.faithful.buriedtowers.plan;

/**
 * Beta's blocks by number, and the meta each wants for a direction. A plan is plain numbers so it can be drawn and looked at with no game running; vanilla's numbers are fixed in every save, which a
 * mod's aren't, and a tower only ever uses vanilla's. A cell packs an id and a meta as {@code id | meta << 8}.
 */
public final class Blocks {
	public static final int AIR = 0, STONE = 1, GRASS = 2, DIRT = 3, COBBLE = 4, PLANKS = 5, SAPLING = 6, BEDROCK = 7, FLOWING_WATER = 8, WATER = 9, FLOWING_LAVA = 10, LAVA = 11, SAND = 12, GRAVEL = 13,
			GOLD_ORE = 14, IRON_ORE = 15, COAL_ORE = 16, LOG = 17, LEAVES = 18, SPONGE = 19, GLASS = 20, LAPIS_ORE = 21, LAPIS_BLOCK = 22, DISPENSER = 23, SANDSTONE = 24, NOTE_BLOCK = 25, BED = 26,
			POWERED_RAIL = 27, DETECTOR_RAIL = 28, STICKY_PISTON = 29, WEB = 30, TALL_GRASS = 31, DEAD_BUSH = 32, PISTON = 33, WOOL = 35, DANDELION = 37, ROSE = 38, BROWN_MUSHROOM = 39, RED_MUSHROOM = 40,
			GOLD_BLOCK = 41, IRON_BLOCK = 42, DOUBLE_SLAB = 43, SLAB = 44, BRICK = 45, TNT = 46, BOOKSHELF = 47, MOSSY = 48, OBSIDIAN = 49, TORCH = 50, FIRE = 51, SPAWNER = 52, WOOD_STAIRS = 53, CHEST = 54,
			WIRE = 55, DIAMOND_ORE = 56, DIAMOND_BLOCK = 57, WORKBENCH = 58, WHEAT = 59, FARMLAND = 60, FURNACE = 61, LIT_FURNACE = 62, SIGN = 63, WOOD_DOOR = 64, LADDER = 65, RAIL = 66, COBBLE_STAIRS = 67,
			WALL_SIGN = 68, LEVER = 69, STONE_PLATE = 70, IRON_DOOR = 71, WOOD_PLATE = 72, REDSTONE_ORE = 73, REDSTONE_TORCH = 76, BUTTON = 77, SNOW = 78, ICE = 79, SNOW_BLOCK = 80, CACTUS = 81, CLAY = 82,
			CANE = 83, JUKEBOX = 84, FENCE = 85, PUMPKIN = 86, NETHERRACK = 87, SOUL_SAND = 88, GLOWSTONE = 89, PORTAL = 90, JACK_O_LANTERN = 91, CAKE = 92, REPEATER = 93, TRAPDOOR = 96;

	/** Meta of a slab, or a double slab, by material. */
	public static final int STONE_SLAB = 0, SAND_SLAB = 1, WOOD_SLAB = 2, COBBLE_SLAB = 3;
	/** Water falling down a column: what a settled stream's cells hold in beta. */
	public static final int FALLING = 8;

	/** Directions, in the order a {@link Frame} turns them. */
	public static final int EAST = 0, SOUTH = 1, WEST = 2, NORTH = 3;
	public static final int[] DX = {1, 0, -1, 0}, DZ = {0, 1, 0, -1};

	private Blocks() {
	}

	public static int cell(int id, int meta) {
		return id | meta << 8;
	}

	public static int id(int cell) {
		return cell & 0xFF;
	}

	public static int meta(int cell) {
		return cell >> 8 & 15;
	}

	public static int opposite(int dir) {
		return dir + 2 & 3;
	}

	/** A stair whose high side is towards {@code up}, so going that way climbs. */
	public static int stairMeta(int up) {
		return new int[] {0, 2, 1, 3}[up];
	}

	/** A torch on the face of the block lying towards {@code wall}. */
	public static int torchOn(int wall) {
		return new int[] {2, 4, 1, 3}[wall];
	}

	/** A ladder, wall sign, furnace or chest whose back is on the block lying towards {@code wall}. */
	public static int backTo(int wall) {
		return new int[] {4, 2, 5, 3}[wall];
	}

	/** A lever on the block lying towards {@code wall}. */
	public static int leverOn(int wall) {
		return new int[] {2, 4, 1, 3}[wall];
	}

	/** A trapdoor hung on the block lying towards {@code wall}. */
	public static int trapdoorOn(int wall) {
		return new int[] {2, 0, 3, 1}[wall];
	}

	/** A closed door whose leaf stands along the side of its cell towards {@code side}. Beta draws a closed door's leaf on the side one quarter back from its meta's ({@code DoorBlock.rotate}). */
	public static int doorOn(int side) {
		return new int[] {2, 3, 0, 1}[side];
	}

	/** The foot of a bed lying with its head towards {@code head}; add 8 for the head's cell. */
	public static int bed(int head) {
		return new int[] {3, 0, 1, 2}[head];
	}

	/** A rail running along {@code dir}, flat. */
	public static int rail(int dir) {
		return dir == EAST || dir == WEST ? 1 : 0;
	}

	public static boolean solid(int id) {
		switch (id) {
			case AIR: case FLOWING_WATER: case WATER: case FLOWING_LAVA: case LAVA: case SAPLING: case BED: case POWERED_RAIL: case DETECTOR_RAIL: case WEB: case TALL_GRASS: case DEAD_BUSH: case DANDELION:
			case ROSE: case BROWN_MUSHROOM: case RED_MUSHROOM: case SLAB: case TORCH: case FIRE: case WOOD_STAIRS: case WIRE: case WHEAT: case FARMLAND: case SIGN: case WOOD_DOOR: case LADDER: case RAIL:
			case COBBLE_STAIRS: case WALL_SIGN: case LEVER: case STONE_PLATE: case IRON_DOOR: case WOOD_PLATE: case REDSTONE_TORCH: case BUTTON: case SNOW: case CACTUS: case CANE: case FENCE: case PORTAL:
			case CAKE: case REPEATER: case TRAPDOOR: case LEAVES: case GLASS:
				return false;
			default:
				return true;
		}
	}

	/** What a player can stand on top of. */
	public static boolean carries(int id) {
		return solid(id) || id == SLAB || id == WOOD_STAIRS || id == COBBLE_STAIRS || id == FENCE || id == LEAVES || id == GLASS || id == TRAPDOOR || id == FARMLAND || id == BED || id == CACTUS;
	}

	/** What a player walks through. */
	public static boolean passable(int id) {
		switch (id) {
			case AIR: case FLOWING_WATER: case WATER: case SAPLING: case POWERED_RAIL: case DETECTOR_RAIL: case WEB: case TALL_GRASS: case DEAD_BUSH: case DANDELION: case ROSE: case BROWN_MUSHROOM:
			case RED_MUSHROOM: case TORCH: case WIRE: case WHEAT: case SIGN: case LADDER: case RAIL: case WALL_SIGN: case LEVER: case STONE_PLATE: case WOOD_PLATE: case REDSTONE_TORCH: case BUTTON:
			case SNOW: case CANE: case REPEATER: case WOOD_DOOR: case IRON_DOOR: case TRAPDOOR:
				return true;
			default:
				return false;
		}
	}
}
