package org.studioquire.faithful.buriedtowers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.buriedtowers.plan.Blocks;
import org.studioquire.faithful.buriedtowers.plan.Climate;
import org.studioquire.faithful.buriedtowers.plan.Ground;
import org.studioquire.faithful.buriedtowers.plan.Plan;
import org.studioquire.faithful.buriedtowers.plan.Style;
import org.studioquire.faithful.buriedtowers.plan.Tower;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * One world's towers. Where they are is a pure function of the seed and the land: a square of {@link #REGION} chunks has a site if the land there suits, and a tower's cells are a pure function of its
 * site, so each chunk writes its own share as it's made, reading no neighbour, and the tower's there in full the moment each piece of it is. Whether a site stands at all is the one thing that can't
 * come from the seed alone, because of chunks that were made before the mod. Each chunk in a site's square says what it knows of that with a mark: see {@link #stands}.
 */
final class Keeper {
	/** One tower to a square of this many chunks each way. */
	static final int REGION = 24;
	/**
	 * How far a site keeps from its square's edges, so towers in neighbouring squares stand at least twice this apart, and how many places in the square are tried for land that suits. At
	 * {@code REACH + 1} and three tries, as first, 2% of towers had a neighbour within 100 blocks and 23% within 200 (user, 2026-11-07: two close together should be very rare); now none can be nearer
	 * than 224 and the median is about 320, with the fifth try keeping as many squares with a tower as before (measured over three seeds, `buriedTowersTest -Ptowers.mode=spacing`).
	 */
	private static final int TRIES = 5;
	static final int MARGIN = 112;
	/** Towers worked out and kept: a plan is about 900 KB, and a player only comes near a few at a time. */
	private static final int CACHED = 6;
	/**
	 * A standing tower's chunks also say if it's simple, so a tower is finished in the style it was started in whatever the world's file says now. Towers from before there were two styles were
	 * all complex, and their marks say so by not saying simple.
	 */
	private static final int MARK_NO = 0, MARK_STANDS = 1, MARK_SEALED = 2, MARK_SIMPLE = 4;

	private static final Map<World, Keeper> KEEPERS = new HashMap<>();

	private final World world;
	private final long seed;
	private final BetaGround ground;
	private final Map<Long, Site> sites = new HashMap<>();
	private final Map<Long, Boolean> decided = new HashMap<>();
	/** The style of a site whose chunks have said: one already standing keeps its own. */
	private final Map<Long, Style> styles = new HashMap<>();
	private final Map<Long, Tower> towers = new LinkedHashMap<Long, Tower>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Tower> eldest) {
			return size() > CACHED;
		}
	};

	/** Where a tower would go, once the land's been looked at. */
	static final class Site {
		final int x, z;
		final Climate climate;

		Site(int x, int z, Climate climate) {
			this.x = x;
			this.z = z;
			this.climate = climate;
		}

		/** Whether a chunk lies within what the tower could reach. */
		boolean covers(int chunkX, int chunkZ) {
			return chunkX >= x - Tower.REACH >> 4 && chunkX <= x + Tower.REACH >> 4 && chunkZ >= z - Tower.REACH >> 4 && chunkZ <= z + Tower.REACH >> 4;
		}
	}

	private Keeper(World world) {
		this.world = world;
		seed = world.getSeed();
		ground = new BetaGround(world);
	}

	static Keeper of(World world) {
		return KEEPERS.computeIfAbsent(world, Keeper::new);
	}

	static void forget(World world) {
		KEEPERS.remove(world);
	}

	private static long key(int a, int b) {
		return (long) a << 32 | b & 0xFFFFFFFFL;
	}

	private static int mix(long seed, int a, int b, int c) {
		return (int) (Tower.mix(seed ^ c * 0x2545F4914F6CDD1DL, a, b) >>> 16);
	}

	/** The site of the square a chunk's in, or null if there's none there. */
	Site site(int chunkX, int chunkZ) {
		int rx = Math.floorDiv(chunkX, REGION), rz = Math.floorDiv(chunkZ, REGION);
		long key = key(rx, rz);
		if (sites.containsKey(key)) return sites.get(key);
		Site site = find(rx, rz);
		sites.put(key, site);
		return site;
	}

	private Site find(int rx, int rz) {
		return find(rx, rz, MARGIN);
	}

	/** The site a square would have with its towers kept {@code margin} from its edges. */
	Site find(int rx, int rz, int margin) {
		int span = REGION * 16, room = span - margin * 2;
		for (int attempt = 0; attempt < TRIES; attempt++) {
			int x = rx * span + margin + Math.floorMod(mix(seed, rx, rz, attempt * 2 + 1), room);
			int z = rz * span + margin + Math.floorMod(mix(seed, rx, rz, attempt * 2 + 2), room);
			if (suits(x, z)) return new Site(x, z, ground.climate(x, z));
		}
		return null;
	}

	/**
	 * Land, mostly above the sea, not so steep the tower would hang off it, and with room above, from nine columns. Each test stays failed once failed, so it stops at the first that does, reading
	 * land already worked out first: a place that won't do costs fewer chunks of terrain, and the answer is the same as from all nine.
	 */
	private boolean suits(int x, int z) {
		int r = Tower.MAX_RADIUS, samples = 9, low = 999, high = 0, wet = 0;
		boolean[] read = new boolean[samples];
		for (int pass = 0; pass < 2; pass++) {
			int i = 0;
			for (int ax = x - r; ax <= x + r; ax += r) {
				for (int az = z - r; az <= z + r; az += r, i++) {
					if (read[i] || pass == 0 && !ground.has(ax, az)) continue;
					read[i] = true;
					int top = ground.top(ax, az);
					low = Math.min(low, top);
					high = Math.max(high, top);
					if (top < Ground.SEA) wet++;
					if (wet * 3 > samples || high - low > 16 || high > 104) return false;
				}
			}
		}
		return true;
	}

	/** The tower of a site, worked out if it isn't kept. */
	Tower tower(Site site) {
		long key = key(site.x, site.z);
		Tower t = towers.get(key);
		if (t == null) {
			t = new Tower(seed, site.x, site.z, site.climate, ground, style(site));
			towers.put(key, t);
		}
		return t;
	}

	/**
	 * Whether a site's tower is there, which has to come out the same for every chunk of it, whatever order they come in and however long between. A chunk that holds a mark was made with the mod and
	 * knows; a chunk that's there with no mark was made without it, and the tower would cut through it. So: any chunk of it standing says the site stands, then no chunk at all means it's free to, and
	 * anything else means it can't. Marks are read from memory first and then disk. A lost chunk of a standing tower comes back as part of it, and a chunk from before the mod stops it for good.
	 */
	private boolean stands(Site site) {
		long key = key(site.x, site.z);
		Boolean known = decided.get(key);
		if (known != null) return known;
		boolean stands = true, standing = false;
		int cx0 = site.x - Tower.REACH >> 4, cx1 = site.x + Tower.REACH >> 4, cz0 = site.z - Tower.REACH >> 4, cz1 = site.z + Tower.REACH >> 4;
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				Integer mark;
				boolean there;
				if (Chunks.isLoaded(world, cx, cz)) {
					mark = BuriedTowers.mark.get(world.getChunk(cx, cz));
					there = true;
				} else {
					mark = BuriedTowers.mark.saved(world, cx, cz);
					there = mark != null || Chunks.isSaved(world, cx, cz);
				}
				if (mark != null && (mark & MARK_STANDS) != 0) {
					standing = true;
					styles.put(key, (mark & MARK_SIMPLE) != 0 ? Style.SIMPLE : Style.COMPLEX);
				} else if (there) stands = false;
			}
		}
		boolean answer = standing || stands;
		decided.put(key, answer);
		return answer;
	}

	/** How a site's tower is built: as its chunks say if it's started, otherwise as this world chose when it was made. */
	private Style style(Site site) {
		stands(site);
		Style known = styles.get(key(site.x, site.z));
		return known != null ? known : BuriedTowers.style.get().equals("complex") ? Style.COMPLEX : Style.SIMPLE;
	}

	/** For the test driver: for the site of a square, whether it stands, and how its finished chunks hold up against its plan. */
	String report(Site site) {
		boolean stands = stands(site);
		Tower tower = tower(site);
		Plan plan = tower.plan;
		int right = 0, wrong = 0, chunks = 0, trees = 0, fallen = 0;
		String sample = "";
		for (int cx = site.x - Tower.REACH >> 4; cx <= site.x + Tower.REACH >> 4; cx++) {
			for (int cz = site.z - Tower.REACH >> 4; cz <= site.z + Tower.REACH >> 4; cz++) {
				if (!Chunks.isLoaded(world, cx, cz) || !lastTouch(cx, cz)) continue;
				chunks++;
				Chunk chunk = world.getChunk(cx, cz);
				for (Map.Entry<Long, String> e : plan.fixtures.entrySet()) {
					int fx = Plan.keyX(e.getKey()), fy = Plan.keyY(e.getKey()), fz = Plan.keyZ(e.getKey());
					if (fx >> 4 != cx || fz >> 4 != cz || Blocks.id(plan.get(fx, fy, fz)) != Blocks.CHEST && Blocks.id(plan.get(fx, fy, fz)) != Blocks.SPAWNER) continue;
					BlockEntity entity = world.getBlockEntity(fx, fy, fz);
					boolean good = e.getValue().startsWith("spawner:") ? entity instanceof MobSpawnerBlockEntity && spawning((MobSpawnerBlockEntity) entity).equals(e.getValue().substring(8)) : !e.getValue().startsWith("chest:") || filled(entity);
					if (!good) {
						wrong++;
						if (sample.length() < 120) sample += " fixture " + fx + "," + fy + "," + fz + ":" + e.getValue();
					}
				}
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = 0; y < Plan.HEIGHT; y++) {
							int c = plan.get(cx * 16 + x, y, cz * 16 + z);
							if (c == Plan.KEEP) continue;
							int want = Blocks.id(c), have = chunk.getBlockId(x, y, z);
							if (want == have || want == 0 && grows(have) && !later(plan.id(cx * 16 + x, y - 1, cz * 16 + z))) {
								right++;
							} else if (fallenIn(plan, chunk, x, y, z)) {
								right++;
								fallen++;
							} else if (tree(have) && tower.treeMayKeep(cx * 16 + x, y, cz * 16 + z)) {
								right++;
								trees++;
							} else {
								wrong++;
								if (sample.length() < 120) sample += " " + (cx * 16 + x) + "," + y + "," + (cz * 16 + z) + ":" + have + "!=" + want;
							}
						}
					}
				}
			}
		}
		return site.x + " " + site.z + " stands=" + stands + " chunks=" + chunks + " right=" + right + " wrong=" + wrong + " trees=" + trees + " fallen=" + fallen + sample;
	}

	private static String spawning(MobSpawnerBlockEntity spawner) {
		NbtCompound nbt = new NbtCompound();
		spawner.writeNbt(nbt);
		return nbt.getString("EntityId");
	}

	private static boolean filled(BlockEntity entity) {
		if (!(entity instanceof ChestBlockEntity)) return false;
		ChestBlockEntity chest = (ChestBlockEntity) entity;
		for (int i = 0; i < chest.size(); i++) if (chest.getStack(i) != null) return true;
		return false;
	}

	/** Worldgen's chunk pass: this chunk's share of the tower, and its mark. */
	void built(Worldgen.Cells cells) {
		int cx = cells.chunkX(), cz = cells.chunkZ();
		Site site = site(cx, cz);
		if (site == null || !site.covers(cx, cz)) return;
		boolean stands = stands(site);
		if (!stands) {
			BuriedTowers.mark.set(cells.chunk(), MARK_NO);
			return;
		}
		Tower tower = tower(site);
		BuriedTowers.mark.set(cells.chunk(), tower.style == Style.SIMPLE ? MARK_STANDS | MARK_SIMPLE : MARK_STANDS);
		Plan plan = tower.plan;
		int ox = cx * 16, oz = cz * 16;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < Plan.HEIGHT; y++) {
					int c = plan.get(ox + x, y, oz + z);
					if (c == Plan.KEEP) continue;
					int id = Blocks.id(c);
					if (later(id)) cells.set(x, y, z, Blocks.COBBLE);
					else cells.set(x, y, z, id, Blocks.meta(c));
				}
			}
		}
	}

	/**
	 * What can't go in a chunk that's still being made: nothing gives it a block entity there, and light it gives wouldn't spread. Cobble holds its place until the decoration pass puts it in, as air
	 * there would let a tree grow into it, to be cut when the chest or the lava went back. No vanilla feature takes cobble for its own (ores want stone), and a tree won't grow through it.
	 */
	private static boolean later(int id) {
		return Block.BLOCKS_WITH_ENTITY[id] || Block.BLOCKS_LIGHT_LUMINANCE[id] > 0;
	}

	/**
	 * Worldgen's decoration pass: this decoration has just finished, and it and the ones before it may have written into four chunks. In each of those belonging to a tower, the cells go back as drawn,
	 * and the block entities and lights go in that couldn't before. Only until each chunk's seen the last decoration that can touch it, marked as sealed: from then on it's the player's, so a chunk lost
	 * and made again beside it, decorating again, leaves it alone. Water and lava go back after everything else in all four, as lava put back under a spring's water that's still there turns to obsidian.
	 */
	void decorated(int originX, int originZ) {
		int dx = originX - 8 >> 4, dz = originZ - 8 >> 4;
		for (int pass = 0; pass < 2; pass++) {
			for (int cx = dx; cx <= dx + 1; cx++) {
				for (int cz = dz; cz <= dz + 1; cz++) {
					Site site = site(cx, cz);
					if (site == null || !site.covers(cx, cz)) continue;
					Chunk chunk = world.getChunk(cx, cz);
					Integer mark = BuriedTowers.mark.get(chunk);
					if (mark == null || (mark & MARK_STANDS) == 0 || (mark & MARK_SEALED) != 0) continue;
					restore(tower(site), chunk, pass == 1);
					if (pass == 1 && lastTouch(cx, cz)) BuriedTowers.mark.set(chunk, mark | MARK_SEALED);
				}
			}
		}
	}

	/** Whether all four decorations that can reach a chunk have happened. */
	private boolean lastTouch(int cx, int cz) {
		for (int ax = cx - 1; ax <= cx; ax++) {
			for (int az = cz - 1; az <= cz; az++) {
				if (!Chunks.isLoaded(world, ax, az) || !world.getChunk(ax, az).terrainPopulated) return false;
			}
		}
		return true;
	}

	private static boolean fluid(int id) {
		return id == Blocks.WATER || id == Blocks.FLOWING_WATER || id == Blocks.LAVA || id == Blocks.FLOWING_LAVA;
	}

	/** The chunk's cells back as drawn: the fluids among them, or everything else. */
	private void restore(Tower tower, Chunk chunk, boolean fluids) {
		Plan plan = tower.plan;
		int ox = chunk.x * 16, oz = chunk.z * 16;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < Plan.HEIGHT; y++) {
					int c = plan.get(ox + x, y, oz + z);
					if (c == Plan.KEEP) continue;
					int want = Blocks.id(c), have = chunk.getBlockId(x, y, z);
					if (fluid(want) != fluids) continue;
					if (fallenIn(plan, chunk, x, y, z)) {
						if (have == want) write(tower, ox + x, y, oz + z, 0, 0);
						continue;
					}
					if (want == have && (later(want) || Blocks.meta(c) == chunk.getBlockMeta(x, y, z))) continue;
					// What a decoration leaves on an open floor can stay: it's only what grows on anything left open to the sky. Not on what goes in after it, a chest say, where it would float.
					if (want == 0 && grows(have) && !later(plan.id(ox + x, y - 1, oz + z))) continue;
					// A tree grown into the open air round the tower stays whole.
					if (tree(have) && tower.treeMayKeep(ox + x, y, oz + z)) continue;
					write(tower, ox + x, y, oz + z, want, Blocks.meta(c));
				}
			}
		}
	}

	/**
	 * Whether a loose stone's land has gone from under it: a lake's dug in decoration, after the tower was written, and can take the ground a fallen stone was laid on. Then it isn't put back, and
	 * goes if it's still there, whatever the lake left in its cell staying. Cells are restored from the bottom up, so a stone on one that went goes too. Only what's under it decides, so it comes out
	 * the same whichever decoration dug the lake and whenever.
	 */
	private static boolean fallenIn(Plan plan, Chunk chunk, int x, int y, int z) {
		return y > 0 && plan.isLoose(chunk.x * 16 + x, y, chunk.z * 16 + z) && !Blocks.solid(chunk.getBlockId(x, y - 1, z));
	}

	private static boolean tree(int id) {
		return id == Blocks.LOG || id == Blocks.LEAVES;
	}

	private static boolean grows(int id) {
		return id == Blocks.SNOW || id == Blocks.TALL_GRASS || id == Blocks.DEAD_BUSH || id == Blocks.DANDELION || id == Blocks.ROSE || id == Blocks.BROWN_MUSHROOM || id == Blocks.RED_MUSHROOM || id == Blocks.SAPLING;
	}

	private void write(Tower tower, int x, int y, int z, int id, int meta) {
		Plan plan = tower.plan;
		world.setBlockWithoutNotifyingNeighbors(x, y, z, id, meta);
		world.blockUpdateEvent(x, y, z);
		String fixture = plan.fixture(x, y, z);
		if (fixture == null) return;
		BlockEntity entity = world.getBlockEntity(x, y, z);
		int colon = fixture.indexOf(':');
		String value = fixture.substring(colon + 1);
		if (entity instanceof ChestBlockEntity) {
			Loot.fill((ChestBlockEntity) entity, value, tower.floorAt(y).depth, new Random(Tower.mix(seed ^ y * 0x7FFFFFFFL, x, z)));
		} else if (entity instanceof MobSpawnerBlockEntity) {
			((MobSpawnerBlockEntity) entity).setSpawnedEntityId(value);
		} else if (entity instanceof JukeboxBlockEntity) {
			// The record left in it: right-clicked, the jukebox gives it up, as beta's do.
			((JukeboxBlockEntity) entity).recordId = (value.equals("cat") ? Item.RECORD_CAT : Item.RECORD_THIRTEEN).id;
			entity.markDirty();
		} else if (entity instanceof SignBlockEntity) {
			String[] lines = value.split("\\|", 4);
			System.arraycopy(lines, 0, ((SignBlockEntity) entity).texts, 0, lines.length);
		}
	}
}
