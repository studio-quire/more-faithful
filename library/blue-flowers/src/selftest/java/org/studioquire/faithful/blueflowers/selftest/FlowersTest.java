package org.studioquire.faithful.blueflowers.selftest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.blueflowers.BlueFlower;
import org.studioquire.faithful.blueflowers.BlueFlowers;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * The flower beside beta's two on the plot and its dye in lapis lazuli's place, then real generated land far off: a square of a spruce forest, one of tundra and one of Swampland's hills, none of
 * it holding lapis lazuli ore, lapis lazuli from before the mod going (ore to stone, blocks to blue wool, standing or kept in a chest), and bonemeal in and out of the cold.
 */
public final class FlowersTest extends WorldTest {
	private static final int SQUARE = 10, SKY = 115, BLUE = 4, LIGHT_BLUE = 12, WHITE = 15, USES = 40;

	/** Where {@code FlowersClientTest} goes to look, in single player: a few patches in each biome, each a name, an eye and the angles to the patch's middle. */
	public static final List<View> views = new ArrayList<>();

	public static final class View {
		public final String name;
		public final double x, y, z;
		public final float yaw, pitch;

		View(String name, double x, double y, double z, float yaw, float pitch) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	private BlueFlower flower;
	private final List<int[]> forestFlowers = new ArrayList<>(), tundraFlowers = new ArrayList<>(), hillFlowers = new ArrayList<>();
	private int lapis, diamond;
	private int fx, fz;

	private ItemStack crafted(int side, Object... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, side, side);
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof Block) grid.setStack(i, new ItemStack((Block) cells[i]));
			else if (cells[i] instanceof ItemStack) grid.setStack(i, ((ItemStack) cells[i]).copy());
		}
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private static boolean dye(ItemStack made, int colour, int count) {
		return made != null && made.getItem() == Item.DYE && made.getDamage() == colour && made.count == count;
	}

	private static ItemStack dye(int colour) {
		return new ItemStack(Item.DYE, 1, colour);
	}

	private static boolean stack(ItemStack stack, int id, int damage, int count) {
		return stack != null && stack.itemId == id && stack.getDamage() == damage && stack.count == count;
	}

	private static boolean wool(ItemStack stack, int count) {
		return stack(stack, Block.WOOL.id, BlueFlowers.BLUE_WOOL, count);
	}

	/** A stack written as the game saves one and read back as it loads one. */
	private static ItemStack saved(ItemStack stack) {
		NbtCompound nbt = new NbtCompound();
		stack.writeNbt(nbt);
		return new ItemStack(nbt);
	}

	private static Object[] nine(Object cell) {
		Object[] cells = new Object[9];
		java.util.Arrays.fill(cells, cell);
		return cells;
	}

	private void recipe() {
		check("a flower makes two blue dye in the inventory's grid", dye(crafted(2, flower), BLUE, 2));
		check("from any cell", dye(crafted(2, null, null, null, flower), BLUE, 2));
		check("and at the bench", dye(crafted(3, null, null, null, null, null, null, null, flower), BLUE, 2));
		check("two flowers make nothing", crafted(2, flower, flower) == null);
		ItemStack rose = crafted(2, Block.ROSE);
		check("a rose still makes red", rose != null && rose.getDamage() == 1 && rose.count == 2);
		// The dye is beta's own blue, so what beta makes of it stands: light blue with bone meal, for one, and blue wool.
		check("blue dye and bone meal make two light blue, by beta's recipe", dye(crafted(2, dye(BLUE), dye(WHITE)), LIGHT_BLUE, 2));
		ItemStack wool = crafted(2, dye(BLUE), new ItemStack(Block.WOOL));
		check("blue dye and wool make blue wool", wool != null && wool.itemId == Block.WOOL.id && wool.getDamage() == 11);
		check("nine blue dye no longer make a lapis lazuli block", crafted(3, nine(dye(BLUE))) == null);
		check("nor does a lapis lazuli block come apart into nine", crafted(2, Block.LAPIS_BLOCK) == null);
		check("a gold block still comes apart into nine ingots", crafted(2, Block.GOLD_BLOCK) != null && crafted(2, Block.GOLD_BLOCK).count == 9);
		// Held lapis lazuli, as any save gives a stack back: through the game's own writing and reading of one.
		check("a stack of lapis lazuli blocks read from a save is as many blue wool", wool(saved(new ItemStack(Block.LAPIS_BLOCK, 5)), 5));
		check("a stack of the ore is as many stone", stack(saved(new ItemStack(Block.LAPIS_ORE, 3)), Block.STONE.id, 0, 3));
		check("the dye is read as it was written", dye(saved(new ItemStack(Item.DYE, 7, BLUE)), BLUE, 7));
		check("and so is a diamond block", stack(saved(new ItemStack(Block.DIAMOND_BLOCK, 2)), Block.DIAMOND_BLOCK.id, 0, 2));
		ItemStack gold = crafted(3, nine(new ItemStack(Item.GOLD_INGOT)));
		check("nine gold ingots still make their block", gold != null && gold.itemId == Block.GOLD_BLOCK.id);
	}

	private void asARose() {
		Block rose = Block.ROSE;
		check("as hard as a rose", flower.getHardness() == rose.getHardness());
		check("of a rose's material", flower.material == rose.material);
		check("with a rose's sound", flower.soundGroup == rose.soundGroup);
		check("ticking as a rose does", Block.BLOCKS_RANDOM_TICK[flower.id] && Block.BLOCKS_RANDOM_TICK[rose.id]);
		check("letting light through as a rose does", Block.BLOCKS_LIGHT_OPACITY[flower.id] == Block.BLOCKS_LIGHT_OPACITY[rose.id] && !Block.BLOCKS_OPAQUE[flower.id]);
		check("not full, as a rose isn't", !flower.isFullCube() && !flower.isOpaque());
		check("the same outline", flower.minX == rose.minX && flower.maxX == rose.maxX && flower.maxY == rose.maxY);

		Block[] grounds = {Block.GRASS_BLOCK, Block.DIRT, Block.FARMLAND, Block.STONE, Block.SAND, Block.GRAVEL, Block.PLANKS, Block.LEAVES, Block.SNOW_BLOCK};
		for (int i = 0; i < grounds.length; i++) {
			set(i - 4, -1, 3, grounds[i]);
			boolean stands = i < 3;
			check((stands ? "stands on " : "doesn't stand on ") + grounds[i].getTranslationKey(), flower.canPlaceAt(world, x + i - 4, y, z + 3) == stands && rose.canPlaceAt(world, x + i - 4, y, z + 3) == stands);
			set(i - 4, -1, 3, Block.STONE);
		}
		check("nothing stops you walking through", flower.getCollisionShape(world, x + 1, y, z + 1) == null);
		check("what it drops is itself, one", flower.getDroppedItemId(0, world.random) == flower.id && flower.getDroppedItemCount(world.random) == 1);
	}

	private int groundItems(int dx, int dz, Block block) {
		int n = 0;
		List<?> items = world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x + dx - 1, y - 1, z + dz - 1, x + dx + 2, y + 2, z + dz + 2));
		for (Object o : items) {
			ItemStack stack = ((ItemEntity) o).stack;
			if (stack.getItem() == Item.ITEMS[block.id]) n += stack.count;
		}
		return n;
	}

	private void clearGroundItems(int dx, int dz) {
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, Box.createCached(x + dx - 1, y - 1, z + dz - 1, x + dx + 2, y + 2, z + dz + 2))) ((ItemEntity) o).markDead();
	}

	/** Both, side by side, lose their ground, by a block's change and so a neighbour's update. */
	private void groundGoes() {
		then(() -> {
			set(-3, -1, -4, Block.GRASS_BLOCK);
			set(-3, 0, -4, flower);
			set(-5, -1, -4, Block.GRASS_BLOCK);
			set(-5, 0, -4, Block.ROSE);
			check("the flower stands", block(-3, 0, -4) == flower.id);
			set(-3, -1, -4, Block.STONE);
			set(-5, -1, -4, Block.STONE);
			check("with its ground gone it's gone, as the rose is", block(-3, 0, -4) == 0 && block(-5, 0, -4) == 0);
		});
		wait(2);
		then(() -> {
			check("and lies there as one item: " + groundItems(-3, -4, flower), groundItems(-3, -4, flower) == 1);
			check("the rose too", groundItems(-5, -4, Block.ROSE) == 1);
			clearGroundItems(-3, -4);
			clearGroundItems(-5, -4);
		});
	}

	private void box(int dx, int dz, Block plant) {
		set(dx, -1, dz, Block.GRASS_BLOCK);
		set(dx, 0, dz, plant);
		set(dx, 1, dz, Block.STONE);
		set(dx + 1, 0, dz, Block.STONE);
		set(dx - 1, 0, dz, Block.STONE);
		set(dx, 0, dz + 1, Block.STONE);
		set(dx, 0, dz - 1, Block.STONE);
	}

	private void unbox(int dx, int dz) {
		set(dx, 1, dz, null);
		set(dx + 1, 0, dz, null);
		set(dx - 1, 0, dz, null);
		set(dx, 0, dz + 1, null);
		set(dx, 0, dz - 1, null);
		set(dx, 0, dz, null);
		set(dx, -1, dz, Block.STONE);
	}

	/** Shut in the dark, the next tick drops them both. */
	private void darkness() {
		then(() -> {
			box(5, 5, flower);
			box(-5, 5, Block.ROSE);
		});
		until("the boxes to go dark", 200, () -> world.getBrightness(x + 5, y, z + 5) < 8 && world.getBrightness(x - 5, y, z + 5) < 8);
		then(() -> {
			check("in the dark it won't live, as a rose won't", !flower.canGrow(world, x + 5, y, z + 5) && !Block.ROSE.canGrow(world, x - 5, y, z + 5));
			flower.onTick(world, x + 5, y, z + 5, world.random);
			Block.ROSE.onTick(world, x - 5, y, z + 5, world.random);
			check("and its tick drops it", block(5, 0, 5) == 0 && block(-5, 0, 5) == 0);
			unbox(5, 5);
			unbox(-5, 5);
		});
		wait(2);
		then(() -> {
			clearGroundItems(5, 5);
			clearGroundItems(-5, 5);
		});
	}

	/** A spot whose middle and corners are this biome, or just its middle for one that's mostly thin strips. Well away from the plot, where nothing ticks, so land stands as it was made. */
	private int[] find(Biome biome, boolean whole) {
		return find(biome, whole, 512);
	}

	/** From so far out; the third number is how far out it was found, for looking on from there. */
	private int[] find(Biome biome, boolean whole, int from) {
		int reach = SQUARE * 8;
		for (int r = from; r < 12000; r += 64) {
			for (int a = -r; a <= r; a += 64) {
				for (int[] p : new int[][] {{a, -r}, {a, r}, {-r, a}, {r, a}}) {
					int px = x + p[0], pz = z + p[1];
					if (Worldgen.biome(world, px, pz) != biome) continue;
					if (!whole) return new int[] {px, pz, r};
					boolean all = true;
					for (int[] c : new int[][] {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}) all &= Worldgen.biome(world, px + c[0] * reach, pz + c[1] * reach) == biome;
					if (all) return new int[] {px, pz, r};
				}
			}
		}
		return null;
	}

	/** Loads a square of chunks round a spot, one wider to the south and east, so its own are all decorated, and gives every one of a plant. */
	private List<int[]> survey(int px, int pz, Block plant) {
		int cx0 = (px >> 4) - SQUARE / 2, cz0 = (pz >> 4) - SQUARE / 2;
		for (int cx = cx0; cx <= cx0 + SQUARE + 1; cx++) {
			for (int cz = cz0; cz <= cz0 + SQUARE + 1; cz++) world.getChunkSource().loadChunk(cx, cz);
		}
		List<int[]> found = new ArrayList<>();
		for (int fx = cx0 * 16; fx < (cx0 + SQUARE) * 16; fx++) {
			for (int fz = cz0 * 16; fz < (cz0 + SQUARE) * 16; fz++) {
				for (int fy = 40; fy < 128; fy++) {
					if (world.getBlockId(fx, fy, fz) == plant.id) found.add(new int[] {fx, fy, fz});
				}
			}
		}
		return found;
	}

	/** How many cells of a block lie under a surveyed square, from the bottom up to a height: its chunks are loaded and decorated by then. */
	private int below(int px, int pz, Block block, int top) {
		int cx0 = (px >> 4) - SQUARE / 2, cz0 = (pz >> 4) - SQUARE / 2, n = 0;
		for (int fx = cx0 * 16; fx < (cx0 + SQUARE) * 16; fx++) {
			for (int fz = cz0 * 16; fz < (cz0 + SQUARE) * 16; fz++) {
				for (int fy = 1; fy < top; fy++) {
					if (world.getBlockId(fx, fy, fz) == block.id) n++;
				}
			}
		}
		return n;
	}

	/** Beta lays a vein of each in every chunk, lapis lazuli up to y 36 or so and diamond to 20. */
	private void ores(String where, int px, int pz) {
		int l = below(px, pz, Block.LAPIS_ORE, 48), d = below(px, pz, Block.DIAMOND_ORE, 48);
		lapis += l;
		diamond += d;
		note(where + ": " + l + " lapis lazuli ore and " + d + " diamond ore under " + SQUARE * SQUARE + " chunks");
	}

	/** The biome beta goes by for the square a flower was decorated in, as it does for its dandelions. */
	private Biome squareBiome(int fx, int fz) {
		return Worldgen.biome(world, ((fx - 8) >> 4) * 16 + 16, ((fz - 8) >> 4) * 16 + 16);
	}

	private void inspect(String where, List<int[]> flowers) {
		int open = 0, wrongGround = 0, elsewhere = 0;
		for (int[] f : flowers) {
			int ground = world.getBlockId(f[0], f[1] - 1, f[2]);
			if (ground != Block.GRASS_BLOCK.id && ground != Block.DIRT.id) wrongGround++;
			if (world.getTopSolidBlockY(f[0], f[2]) <= f[1]) open++;
			Biome biome = squareBiome(f[0], f[2]);
			if (biome != Biome.TAIGA && biome != Biome.TUNDRA && biome != Biome.SWAMPLAND) elsewhere++;
		}
		check(where + ": all on grass or dirt, " + wrongGround + " not", wrongGround == 0);
		check(where + ": all in squares beta reckons Taiga, Tundra or Swampland, " + elsewhere + " not", elsewhere == 0);
		note(where + ": " + open + " of " + flowers.size() + " with nothing solid overhead");
	}

	private String patches(List<int[]> flowers) {
		List<List<int[]>> groups = group(flowers);
		StringBuilder sizes = new StringBuilder();
		for (List<int[]> g : groups) sizes.append(g.size()).append(' ');
		return flowers.size() + " flowers in " + groups.size() + " patches over " + SQUARE * SQUARE + " chunks: " + sizes.toString().trim();
	}

	/** Flowers within a few cells of each other. */
	private static List<List<int[]>> group(List<int[]> flowers) {
		List<List<int[]>> groups = new ArrayList<>();
		for (int[] f : flowers) {
			List<int[]> home = null;
			for (List<int[]> g : groups) {
				for (int[] other : g) {
					if (Math.abs(other[0] - f[0]) <= 5 && Math.abs(other[2] - f[2]) <= 5) {
						home = g;
						break;
					}
				}
				if (home != null) break;
			}
			if (home == null) groups.add(home = new ArrayList<>());
			home.add(f);
		}
		return groups;
	}

	/** Beta's walk on a floor of grass in the sky here: how many of each grew over so many bonemeal. */
	private int[] bonemeal(int px, int pz, int uses) {
		int reach = 10;
		for (int bx = px - reach; bx <= px + reach; bx++) {
			for (int bz = pz - reach; bz <= pz + reach; bz++) world.getChunkSource().loadChunk(bx >> 4, bz >> 4);
		}
		for (int bx = px - reach; bx <= px + reach; bx++) {
			for (int bz = pz - reach; bz <= pz + reach; bz++) world.setBlockWithoutNotifyingNeighbors(bx, SKY, bz, Block.GRASS_BLOCK.id);
		}
		int[] grown = new int[2];
		ItemStack meal = new ItemStack(Item.DYE, uses, 15);
		for (int i = 0; i < uses; i++) {
			meal.useOnBlock(player, world, px, SKY, pz, 1);
			for (int bx = px - reach; bx <= px + reach; bx++) {
				for (int bz = pz - reach; bz <= pz + reach; bz++) {
					int id = world.getBlockId(bx, SKY + 1, bz);
					if (id == flower.id) grown[0]++;
					if (id == Block.ROSE.id) grown[1]++;
					if (id != 0) world.setBlockWithoutNotifyingNeighbors(bx, SKY + 1, bz, 0);
				}
			}
		}
		check("the bonemeal was used up", meal.count == 0);
		for (int bx = px - reach; bx <= px + reach; bx++) {
			for (int bz = pz - reach; bz <= pz + reach; bz++) world.setBlockWithoutNotifyingNeighbors(bx, SKY, bz, 0);
		}
		return grown;
	}

	/** For each of a few of the biggest patches, a place a few cells off that sees the most of it, none right underfoot, for the pictures. */
	private void chooseViews(String biome, List<int[]> flowers) {
		List<List<int[]>> groups = group(flowers);
		groups.sort((a, b) -> b.size() - a.size());
		int taken = 0;
		for (List<int[]> g : groups) {
			if (taken == 3) break;
			View view = null;
			int best = 0;
			double cx = 0, cy = 0, cz = 0;
			for (int[] f : g) {
				cx += f[0] + 0.5;
				cy += f[1] + 0.4;
				cz += f[2] + 0.5;
			}
			cx /= g.size();
			cy /= g.size();
			cz /= g.size();
			for (int d = 5; d <= 10; d++) {
				for (int a = 0; a < 16; a++) {
					int ex = (int) Math.floor(cx + d * Math.cos(a * Math.PI / 8)), ez = (int) Math.floor(cz + d * Math.sin(a * Math.PI / 8));
					int ey = Worldgen.surface(world, ex, ez);
					if (ey < cy || !(world.isAir(ex, ey, ez) || world.getBlockId(ex, ey, ez) == Block.SNOW.id) || !world.isAir(ex, ey + 1, ez) || !world.getMaterial(ex, ey - 1, ez).isSolid()) continue;
					double eye = ey + 1.62;
					int seen = 0;
					for (int[] f : g) {
						if (Math.abs(f[0] - ex) <= 2 && Math.abs(f[2] - ez) <= 2) {
							seen = -1;
							break;
						}
						// Seen to its foot, not just its petals over a ledge.
						HitResult hit = world.raycast(Vec3d.create(ex + 0.5, eye, ez + 0.5), Vec3d.create(f[0] + 0.5, f[1] + 0.25, f[2] + 0.5));
						if (hit == null || world.getMaterial(hit.blockX, hit.blockY, hit.blockZ) == Material.PLANT) seen++;
					}
					if (seen <= best || seen * 2 < g.size()) continue;
					best = seen;
					double tx = cx - (ex + 0.5), ty = cy - eye, tz = cz - (ez + 0.5);
					view = new View(biome + "-" + (taken + 1), ex + 0.5, eye, ez + 0.5, (float) Math.toDegrees(Math.atan2(-tx, tz)), (float) -Math.toDegrees(Math.atan2(ty, Math.hypot(tx, tz))));
				}
			}
			if (view == null) continue;
			views.add(view);
			taken++;
			note("view " + view.name + ": stand at " + (int) Math.floor(view.x) + " " + (int) Math.floor(view.y - 1.62) + " " + (int) Math.floor(view.z) + ", " + best + " of " + g.size() + " flowers seen, seed " + world.getSeed());
		}
	}

	private boolean underLeaves(int[] f) {
		for (int fy = f[1] + 1; fy < 128; fy++) {
			if (world.getBlockId(f[0], fy, f[2]) == Block.LEAVES.id) return true;
		}
		return false;
	}

	private void land() {
		then(() -> {
			int[] forest = find(Biome.TAIGA, true);
			if (!check("there's a spruce forest to look in", forest != null)) return;
			fx = forest[0];
			fz = forest[1];
			forestFlowers.addAll(survey(fx, fz, flower));
			note("taiga at " + fx + " " + fz + ": " + patches(forestFlowers));
			// Made as beta's dandelions are, at their rate in Taiga, and most of beta's patches start too far from the ground to make anything, so a square can have few of either.
			note("and beta's dandelions there: " + patches(survey(fx, fz, Block.DANDELION)));
			inspect("in the forest", forestFlowers);
			ores("under the forest", fx, fz);
		});
		then(() -> {
			int[] tundra = find(Biome.TUNDRA, true);
			if (tundra == null) tundra = find(Biome.TUNDRA, false);
			if (tundra == null) {
				note("no tundra near");
				return;
			}
			tundraFlowers.addAll(survey(tundra[0], tundra[1], flower));
			note("tundra at " + tundra[0] + " " + tundra[1] + ": " + patches(tundraFlowers));
			check("flowers grow in the two squares, " + (forestFlowers.size() + tundraFlowers.size()), forestFlowers.size() + tundraFlowers.size() >= 4);
			inspect("in the tundra", tundraFlowers);
			ores("under the tundra", tundra[0], tundra[1]);
		});
		then(() -> {
			// Swampland lies in strips a few hundred blocks across, so a whole square of it may not be near: then the square round a spot of it, and only its own chunks' flowers count. The
			// plot is somewhere else each run, and a square with only a corner of Swampland in it can have none, so it looks on to further spots, a few, before saying none grow.
			int[] hills = find(Biome.SWAMPLAND, true);
			if (hills == null) hills = find(Biome.SWAMPLAND, false);
			if (!check("there's Swampland to look in", hills != null)) return;
			for (int look = 0; hills != null; look++) {
				List<int[]> found = survey(hills[0], hills[1], flower);
				int own = 0;
				for (int[] f : found) {
					if (squareBiome(f[0], f[2]) != Biome.SWAMPLAND) continue;
					hillFlowers.add(f);
					own++;
				}
				note("swampland at " + hills[0] + " " + hills[1] + ": " + own + " flowers in its own chunks, " + found.size() + " in the square with its other biomes'");
				inspect("in swampland's square", found);
				ores("under swampland", hills[0], hills[1]);
				if (hillFlowers.size() >= 4 || look == 3) break;
				hills = find(Biome.SWAMPLAND, false, hills[2] + SQUARE * 32);
			}
			note("swampland: " + patches(hillFlowers).replace(" over " + SQUARE * SQUARE + " chunks", ""));
			check("flowers grow in Swampland's hills, " + hillFlowers.size(), hillFlowers.size() > 0);
			check("no lapis lazuli ore in new land, " + lapis + " cells", lapis == 0);
			check("and beta's other ores as ever: diamond, " + diamond + " cells", diamond > 0);
		});
		// Long enough for the land's own light to spread: it can only have added to what each flower saw.
		wait(200);
		then(() -> {
			StringBuilder dark = new StringBuilder();
			int shaded = 0;
			for (List<int[]> flowers : java.util.Arrays.asList(forestFlowers, tundraFlowers, hillFlowers)) {
				for (int[] f : flowers) {
					if (flower.canGrow(world, f[0], f[1], f[2])) continue;
					// Swampland grows big oaks, one tree in ten, whose crowns reach further than a flower keeps from its square's edge: one a neighbour grew later can stand over a
					// flower, as beta's trees come to stand over its own, and under leaves a flower lives by day and drops in the dark. Seen once, 2026-11-12, of 26 in a square.
					if (flowers == hillFlowers && underLeaves(f)) shaded++;
					else dark.append(' ').append(f[0]).append(',').append(f[1]).append(',').append(f[2]);
				}
			}
			if (shaded > 0) note(shaded + " in Swampland under a tree that grew over it, in too little light just now");
			check("every one lives, light settled; too dark:" + dark, dark.length() == 0);
			views.clear();
			chooseViews("taiga", forestFlowers);
			chooseViews("tundra", tundraFlowers);
			chooseViews("swampland", hillFlowers);
		});
	}

	/**
	 * Lapis lazuli ore and blocks put into a chunk of real land, then the mod's pass over the chunk as it runs when one loads: the ore is stone and the blocks blue wool, and every other cell of
	 * the chunk, its meta and each column's height are as they were. They go in deep in the rock, in a cave's air, as a column's top block and at the very top of the world.
	 */
	private void oldLapis() {
		then(() -> {
			Chunk chunk = world.getChunk(fx >> 4, fz >> 4);
			int ox = chunk.x * 16, oz = chunk.z * 16, ore = Block.LAPIS_ORE.id, block = Block.LAPIS_BLOCK.id, planted = 0;
			int[] before = cells(chunk);
			for (int i = 0; i < 40; i++) {
				int lx = i * 7 % 16, lz = i * 11 % 16, top = chunk.getHeight(lx, lz);
				int y = i % 4 == 0 ? Math.min(top, 126) : i == 1 ? 127 : 4 + i;
				if (world.setBlockWithoutNotifyingNeighbors(ox + lx, y, oz + lz, i % 3 == 0 ? block : ore)) planted++;
			}
			int[] want = cells(chunk);
			int ores = 0, blocks = 0;
			for (int i = 0; i < 32768; i++) {
				if ((want[i] & 0xFFF) == ore) {
					want[i] = Block.STONE.id;
					ores++;
				} else if ((want[i] & 0xFFF) == block) {
					want[i] = Block.WOOL.id | BlueFlowers.BLUE_WOOL << 12;
					blocks++;
				}
			}
			check("lapis lazuli stands in the chunk, " + ores + " cells of ore and " + blocks + " blocks", ores + blocks == planted && ores >= 20 && blocks >= 10);
			BlueFlowers.lapisGoes(chunk);
			int[] after = cells(chunk);
			int wrong = 0, left = 0;
			for (int i = 0; i < after.length; i++) {
				if (after[i] != want[i]) wrong++;
				if (i < 32768 && ((after[i] & 0xFFF) == ore || (after[i] & 0xFFF) == block)) left++;
			}
			check("the pass over a chunk leaves no lapis lazuli ore or block, " + left + " left", left == 0);
			check("the ore stone, the blocks blue wool, and every other cell, meta and column height as it was, " + wrong + " not", wrong == 0);
			// The land goes back as it was found, for the looks at it that follow.
			for (int i = 0; i < 32768; i++) {
				if (after[i] != before[i]) world.setBlockWithoutNotifyingNeighbors(ox + (i >> 11), i & 127, oz + (i >> 7 & 15), before[i] & 0xFFF, before[i] >> 12);
			}
		});
	}

	/** A chunk's cells as id and meta, then its columns' heights. */
	private static int[] cells(Chunk chunk) {
		int[] cells = new int[32768 + 256];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 128; y++) cells[x << 11 | z << 7 | y] = chunk.getBlockId(x, y, z) | chunk.getBlockMeta(x, y, z) << 12;
				cells[32768 + (x << 4 | z)] = chunk.getHeight(x, z);
			}
		}
		return cells;
	}

	private void bonemealing() {
		then(() -> {
			int[] cold = bonemeal(fx, fz, USES);
			note("bonemeal in the spruce forest, " + USES + " uses: " + cold[0] + " blue, " + cold[1] + " roses");
			check("bonemeal on cold grass grows blue flowers", cold[0] > 0);
			check("and roses still", cold[1] > 0);
			check("at about a rose's odds", cold[0] * 3 >= cold[1] && cold[1] * 3 >= cold[0]);
			// Where none grows wild, too: bonemeal brings beta's two flowers in every biome, and this one with them.
			int[] warm = find(Biome.FOREST, false);
			if (!check("a warm forest to try", warm != null)) return;
			int[] elsewhere = bonemeal(warm[0], warm[1], USES);
			note("in a warm forest: " + elsewhere[0] + " blue, " + elsewhere[1] + " roses");
			check("bonemeal grows them in a warm forest as well", elsewhere[0] > 0);
			check("roses as ever", elsewhere[1] > 0);
			check("at about a rose's odds there too", elsewhere[0] * 3 >= elsewhere[1] && elsewhere[1] * 3 >= elsewhere[0]);
		});
	}

	@Override
	protected void script() {
		flower = BlueFlowers.flower.get();
		scene("grass row", a -> {
			for (int dx = -2; dx <= 2; dx++) set(dx, -1, 2, Block.GRASS_BLOCK);
			set(1, -1, 1, Block.GRASS_BLOCK);
		});
		scene("kit", a -> {
			Item[] kit = {Item.ITEMS[Block.ROSE.id], Item.ITEMS[Block.DANDELION.id], Item.ITEMS[flower.id]};
			for (int i = 0; i < kit.length; i++) player.inventory.main[i] = new ItemStack(kit[i], 4);
			// Beside them in the hotbar for the row's picture: the flower's dye, and beta's two for its company.
			int[] dyes = {1, 11, BLUE};
			for (int i = 0; i < dyes.length; i++) player.inventory.main[kit.length + i] = new ItemStack(Item.DYE, 4, dyes[i]);
			player.currentScreenHandler.sendContentUpdates();
		});
		scene("standing", a -> check("where the world runs, the placed flower stands", block(1, 0, 1) == flower.id));
		scene("broken", a -> check("where the world runs, it's gone", block(1, 0, 1) == 0));
		scene("carrying", a -> {
			int n = 0;
			for (ItemStack s : player.inventory.main) if (s != null && s.getItem() == Item.ITEMS[flower.id]) n += s.count;
			check("it was picked up, four again: " + n, n == 4);
		});
		scene("clear", a -> {
			for (int dx = -2; dx <= 2; dx++) {
				set(dx, 0, 2, null);
				set(dx, -1, 2, Block.STONE);
			}
			set(1, -1, 1, Block.STONE);
			java.util.Arrays.fill(player.inventory.main, null);
			player.currentScreenHandler.sendContentUpdates();
		});
		if (restarted()) {
			check("a flower came back with its name", block(0, 0, -2) == flower.id);
			// Its chunk has been saved and loaded since: ore that a world held from before the mod.
			check("lapis lazuli ore saved in the floor came back as stone", block(4, -1, -4) == Block.STONE.id);
			check("and ore saved standing on it too", block(4, 0, -4) == Block.STONE.id);
			check("the diamond ore beside it is diamond ore still", block(5, -1, -4) == Block.DIAMOND_ORE.id);
			check("a lapis lazuli block somebody placed came back as blue wool", block(6, 0, -4) == Block.WOOL.id && meta(6, 0, -4) == BlueFlowers.BLUE_WOOL);
			check("and the white wool beside it is white still", block(7, 0, -4) == Block.WOOL.id && meta(7, 0, -4) == 0);
			BlockEntity chest = world.getBlockEntity(x + 6, y, z - 6);
			if (check("the chest is where it was left", chest instanceof ChestBlockEntity)) {
				ChestBlockEntity kept = (ChestBlockEntity) chest;
				check("lapis lazuli blocks kept in a chest came back as blue wool, as many", wool(kept.getStack(0), 7));
				check("lapis lazuli ore kept there as stone", stack(kept.getStack(1), Block.STONE.id, 0, 3));
				check("the dye kept there is the dye", dye(kept.getStack(2), BLUE, 5));
				check("and the gold block a gold block", stack(kept.getStack(3), Block.GOLD_BLOCK.id, 0, 1));
			}
			return;
		}
		then(() -> {
			recipe();
			asARose();
			set(0, -1, -2, Block.GRASS_BLOCK);
			set(0, 0, -2, flower);
			set(4, -1, -4, Block.LAPIS_ORE);
			set(4, 0, -4, Block.LAPIS_ORE);
			set(5, -1, -4, Block.DIAMOND_ORE);
			set(6, 0, -4, Block.LAPIS_BLOCK);
			set(7, 0, -4, Block.WOOL);
			set(6, 0, -6, Block.CHEST);
			BlockEntity chest = world.getBlockEntity(x + 6, y, z - 6);
			if (check("a chest to keep things in", chest instanceof ChestBlockEntity)) {
				ChestBlockEntity keeping = (ChestBlockEntity) chest;
				keeping.setStack(0, new ItemStack(Block.LAPIS_BLOCK, 7));
				keeping.setStack(1, new ItemStack(Block.LAPIS_ORE, 3));
				keeping.setStack(2, new ItemStack(Item.DYE, 5, BLUE));
				keeping.setStack(3, new ItemStack(Block.GOLD_BLOCK, 1));
			}
			check("ore put into a chunk that's already in the world stands until the chunk is loaded again", block(4, -1, -4) == Block.LAPIS_ORE.id);
		});
		groundGoes();
		darkness();
		land();
		oldLapis();
		bonemealing();
	}
}
