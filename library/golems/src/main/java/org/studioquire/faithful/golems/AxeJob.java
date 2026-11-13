package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.api.Tools;

/**
 * The axe: a woodcutter. It fells the trees whose feet are in its area ({@link Tree} says what's one, so nothing built of logs), each from its farthest log down to its foot, so if it stops
 * partway what's left still stands on the ground. It reaches the logs it needs from where it stands however high they are, standing as near the trunk as a person would where it can and
 * stretching out to a player's reach where low leaves keep it further off. It leaves the
 * leaves to fall as beta's do once their tree is gone, picks up what the trees dropped (logs, saplings, apples) as it comes to lie in its area, and plants a sapling of the same kind where each
 * tree it felled stood, keeping back the saplings it still has stumps for when it empties its load and fetching them from its chest when it has none (through {@link #wants}).
 * <p>
 * What it does next, by rank: the tree it's felling; a stump it has the sapling for; what the trees dropped; a tree it was called away from partway; then the nearest other tree, by its
 * farthest log (by any tip, if that's out of reach).
 * A log takes twice the ticks a player's hand with the same axe would.
 */
final class AxeJob extends Job {
	private static final int FELLING = 3000, PLANT = 2000, PICK_UP = 1500, UNFINISHED = 1100, NEXT_TREE = 1000, NEXT_TREE_ANY_TIP = 999;
	/** Trees it started and was called away from, to come back to: a tree cut partway may no longer look like one to start on. */
	private static final int MOST_UNFINISHED = 32;
	/** How often it looks over its whole area for trees, in its own ticks; the tree it's felling it looks at every time. */
	private static final int LOOK_EVERY = 200;
	/** How long a stump waits for a sapling of its own kind before it takes any, in world ticks: its tree's leaves are long down by then. */
	private static final int OWN_KIND_FOR = 6000;
	/** Beyond its area, how far it goes for what its trees dropped: their leaves overhang it. */
	private static final int DROPS_OVER = 2;
	private static final int MOST_STUMPS = 256;

	private enum Piece { CHOP, PLANT }

	/** Where a tree it felled stood, and what grows there. */
	private static final class Stump {
		final int x, y, z, saplingId, saplingMeta;
		final long since;

		Stump(int x, int y, int z, int saplingId, int saplingMeta, long since) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.saplingId = saplingId;
			this.saplingMeta = saplingMeta;
			this.since = since;
		}
	}

	private Tree felling;
	private final List<Tree> unfinished = new ArrayList<>();
	private final List<Tree> trees = new ArrayList<>();
	/** The other trees' logs it listed, by place, to know which tree it's starting on. */
	private final Map<Long, Tree> treeOf = new HashMap<>();
	/** Every log it listed, by place: the height of its tree's foot; and how near it stands to chop it ({@link #across}). */
	private final Map<Long, Integer> footOf = new HashMap<>();
	private final Map<Long, Double> across = new HashMap<>();
	private int lookedAt = Integer.MIN_VALUE / 2;
	private final List<Stump> stumps = new ArrayList<>();
	private int ticks;
	private float progress;
	private Piece piece = Piece.CHOP;

	AxeJob(GolemEntity golem) {
		super(golem);
	}

	@Override
	public String kind() {
		return "axe";
	}

	@Override
	protected void findWork(Area area, List<Target> found) {
		for (Iterator<Stump> it = stumps.iterator(); it.hasNext(); ) {
			Stump s = it.next();
			if (!world.isPosLoaded(s.x, 64, s.z)) continue;
			if (!Block.BLOCKS[s.saplingId].canPlaceAt(world, s.x, s.y, s.z)) {
				// Something else is there now, or the ground went: it's not the stump's any more.
				it.remove();
				continue;
			}
			if (area.contains(s.x, s.z) && plantable(s) && saplingFor(s) >= 0) found.add(new Target(s.x, s.y, s.z, PLANT));
		}
		Box box = Box.createCached(area.minX - DROPS_OVER, 0, area.minZ - DROPS_OVER, area.maxX + 1 + DROPS_OVER, 128, area.maxZ + 1 + DROPS_OVER);
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, box)) {
			ItemEntity item = (ItemEntity) o;
			if (wanted(item)) found.add(new Target(item, PICK_UP));
		}
		footOf.clear();
		across.clear();
		if (felling != null) {
			felling = Tree.of(world, felling.x, felling.y, felling.z, true);
			if (felling != null) {
				for (int[] l : felling.logs) {
					if (l[4] == 0) continue;
					found.add(new Target(l[0], l[1], l[2], FELLING + l[3]));
					footOf.put(Tree.key(l[0], l[1], l[2]), felling.y);
				}
			}
		}
		treeOf.clear();
		for (int i = 0; i < unfinished.size(); i++) {
			Tree t = Tree.of(world, unfinished.get(i).x, unfinished.get(i).y, unfinished.get(i).z, true);
			if (t == null || t.sameFoot(felling)) {
				unfinished.remove(i--);
				continue;
			}
			unfinished.set(i, t);
			for (int[] l : t.logs) {
				if (l[4] == 0) continue;
				found.add(new Target(l[0], l[1], l[2], UNFINISHED + l[3]));
				treeOf.put(Tree.key(l[0], l[1], l[2]), t);
				footOf.put(Tree.key(l[0], l[1], l[2]), t.y);
			}
		}
		if (golem.age - lookedAt >= LOOK_EVERY) look(area);
		for (Tree t : trees) {
			if (t.sameFoot(felling) || isUnfinished(t)) continue;
			int[] far = t.farthest();
			for (int[] l : t.logs) {
				if (l[4] == 0 || !Tree.isLog(world.getBlockId(l[0], l[1], l[2]))) continue;
				found.add(new Target(l[0], l[1], l[2], l == far ? NEXT_TREE : NEXT_TREE_ANY_TIP));
				treeOf.put(Tree.key(l[0], l[1], l[2]), t);
				footOf.put(Tree.key(l[0], l[1], l[2]), t.y);
			}
		}
	}

	/** Every tree with its foot in the area, from the top of each column down to the ground. */
	private void look(Area area) {
		lookedAt = golem.age;
		trees.clear();
		for (int x = area.minX; x <= area.maxX; x++) {
			for (int z = area.minZ; z <= area.maxZ; z++) {
				if (!world.isPosLoaded(x, 64, z)) continue;
				for (int y = Math.min(world.getTopY(x, z), 126); y > 0; y--) {
					int id = world.getBlockId(x, y, z);
					if (Tree.isLog(id)) {
						if (!Tree.rootsIn(world.getBlockId(x, y - 1, z))) continue;
						Tree t = Tree.of(world, x, y, z, false);
						if (t != null) trees.add(t);
						break;
					}
					if (id == 0 || Tree.isLeaves(id) || Block.BLOCKS[id] == null || !Block.BLOCKS[id].material.blocksMovement()) continue;
					break;
				}
			}
		}
	}

	@Override
	protected boolean stillWork(Target t) {
		if (t.entity != null) return wanted((ItemEntity) t.entity);
		if (t.rank == PLANT) {
			Stump s = stumpAt(t.x, t.y, t.z);
			return s != null && plantable(s) && saplingFor(s) >= 0;
		}
		return Tree.isLog(world.getBlockId(t.x, t.y, t.z));
	}

	@Override
	protected boolean work(Target t) {
		if (t.rank == PLANT) return plant(t);
		return chop(t);
	}

	private boolean chop(Target t) {
		piece = Piece.CHOP;
		if (ticks == 0 && t.rank < FELLING) {
			// Starting on another tree, or back to one it left: as it stands now, in case it's been built onto since the last look.
			boolean back = t.rank >= UNFINISHED;
			Tree tree = treeOf.get(Tree.key(t.x, t.y, t.z));
			tree = tree == null ? null : Tree.of(world, tree.x, tree.y, tree.z, back);
			if (tree == null || !hasTip(tree, t)) {
				lookedAt = Integer.MIN_VALUE / 2;
				return true;
			}
			if (felling != null && unfinished.size() < MOST_UNFINISHED && !isUnfinished(felling)) unfinished.add(Tree.foot(felling.x, felling.y, felling.z));
			unfinished.removeIf(tree::sameFoot);
			felling = tree;
		}
		Block block = Block.BLOCKS[world.getBlockId(t.x, t.y, t.z)];
		ticks++;
		if (ticks % GolemEntity.EXCHANGE == 0) progress += Tools.breakProgress(block, golem.tool(), golem.onGround, golem.inWater());
		if (ticks % (4 * GolemEntity.EXCHANGE) == 0) {
			Sounds.play(world, t.x + 0.5, t.y + 0.5, t.z + 0.5, block.soundGroup.getSound(), (block.soundGroup.getVolume() + 1.0F) / 8.0F, block.soundGroup.getPitch() * 0.5F);
		}
		if (progress < 1.0F) return false;
		Tree tree = felling;
		breakBlock(t.x, t.y, t.z);
		abandon(t);
		if (tree != null && tree.isFoot(t.x, t.y, t.z)) {
			felling = null;
			if (stumps.size() < MOST_STUMPS) stumps.add(new Stump(t.x, t.y, t.z, tree.saplingId, tree.saplingMeta, world.getTime()));
		}
		return true;
	}

	private boolean isUnfinished(Tree tree) {
		for (Tree u : unfinished) {
			if (u.sameFoot(tree)) return true;
		}
		return false;
	}

	private static boolean hasTip(Tree tree, Target t) {
		for (int[] l : tree.logs) {
			if (l[4] != 0 && l[0] == t.x && l[1] == t.y && l[2] == t.z) return true;
		}
		return false;
	}

	// Planting.

	private Stump stumpAt(int x, int y, int z) {
		for (Stump s : stumps) {
			if (s.x == x && s.y == y && s.z == z) return s;
		}
		return null;
	}

	/** Where a sapling would take now: as its block asks, and light enough that it wouldn't drop off again at once. */
	private boolean plantable(Stump s) {
		Block b = Block.BLOCKS[s.saplingId];
		return b.canPlaceAt(world, s.x, s.y, s.z) && (!(b instanceof PlantBlock) || ((PlantBlock) b).canGrow(world, s.x, s.y, s.z));
	}

	private static boolean isSapling(ItemStack stack) {
		return stack != null && stack.itemId < Block.BLOCKS.length && Block.BLOCKS[stack.itemId] instanceof SaplingBlock;
	}

	/** The slot of the sapling it would plant here: its own kind, or once that's waited long enough, any. */
	private int saplingFor(Stump s) {
		int any = -1;
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			ItemStack stack = golem.slot(i);
			if (stack == null) continue;
			if (stack.itemId == s.saplingId && stack.getDamage() == s.saplingMeta) return i;
			if (any < 0 && isSapling(stack)) any = i;
		}
		return world.getTime() - s.since >= OWN_KIND_FOR ? any : -1;
	}

	private boolean plant(Target t) {
		piece = Piece.PLANT;
		Stump s = stumpAt(t.x, t.y, t.z);
		int slot = s == null ? -1 : saplingFor(s);
		if (slot < 0) return true;
		ItemStack stack = golem.slot(slot);
		Block b = Block.BLOCKS[stack.itemId];
		if (!world.setBlock(s.x, s.y, s.z, stack.itemId, stack.getDamage())) return true;
		// As a player's hand places it.
		Sounds.play(world, s.x + 0.5, s.y + 0.5, s.z + 0.5, b.soundGroup.getSound(), (b.soundGroup.getVolume() + 1.0F) / 2.0F, b.soundGroup.getPitch() * 0.8F);
		stack.count--;
		golem.setSlot(slot, stack);
		stumps.remove(s);
		golem.worked++;
		return true;
	}

	/** Saplings it has stumps waiting for: its own kind for each, and any for one that's waited long enough. */
	@Override
	protected int wants(ItemStack stack) {
		if (!isSapling(stack)) return 0;
		int n = 0;
		long now = world.getTime();
		for (Stump s : stumps) {
			if (stack.itemId == s.saplingId && stack.getDamage() == s.saplingMeta || now - s.since >= OWN_KIND_FOR) n++;
		}
		return n;
	}

	// What the trees dropped.

	/** A tree's log, sapling or apple lying still in or by its area, out of the water and not on a tree, that it would take and has room for. */
	private boolean wanted(ItemEntity item) {
		if (item.dead || item.stack == null || !item.onGround || !golem.wouldTake(item) || !golem.hasRoomFor(item.stack)) return false;
		ItemStack s = item.stack;
		boolean tree = isSapling(s) || s.itemId < Block.BLOCKS.length && Tree.isLog(s.itemId) || Tags.has("c:fruits/apple", s);
		if (!tree) return false;
		int x = MathHelper.floor(item.x), y = MathHelper.floor(item.boundingBox.minY + 0.05), z = MathHelper.floor(item.z);
		int under = world.getBlockId(x, y - 1, z);
		return !Ground.isLiquid(world.getBlockId(x, y, z)) && !Tree.isLeaves(under) && !Tree.isLog(under);
	}

	// Reach.

	/**
	 * A log: above its eye at any height, at or below it as far as anything else (a player's reach), through leaves and logs but nothing else solid; and along the ground no further from
	 * its column than it must stand ({@link #across}). The loop walks it up within {@link GolemEntity#CLOSE} of what's that near its eye by itself; a log high up never is, so the axe
	 * says how near it stands to the trunk. A stump: as anything else. (What lies about is the loop's to reach.)
	 */
	@Override
	protected boolean reaches(double ex, double ey, double ez, Target t) {
		if (t.rank == PLANT) return super.reaches(ex, ey, ez, t);
		double nx = clamp(ex, t.x, t.x + 1), ny = clamp(ey, t.y, t.y + 1), nz = clamp(ez, t.z, t.z + 1);
		double dx = nx - ex, dy = ny - ey, dz = nz - ez, r = GolemEntity.REACH;
		double along = dx * dx + dz * dz, out = across(t);
		if (along > out * out) return false;
		if (dy <= 0 && along + dy * dy > r * r) return false;
		return clearTo(ex, ey, ez, t.x, t.y, t.z);
	}

	/**
	 * How near a log's column along the ground it stands to chop it: within {@link GolemEntity#CLOSE} where it can get that near and see the log, else within 3 where it can, else as far
	 * as it reaches. "Can get" is the loop's walk search, which is current when this is first asked for a log in a look for work: the loop searches, then weighs places to stand, and
	 * this is kept until the next look ({@link #findWork} forgets it). A first build asked only whether there was a floor that near, and the top of a trunk cut down to two logs was one,
	 * with no way up: it held two trees of nine in one wood two logs from the ground, their low leaves keeping it further off than that.
	 */
	private double across(Target t) {
		long k = Tree.key(t.x, t.y, t.z);
		Integer foot = footOf.get(k);
		if (foot == null) return GolemEntity.REACH;
		Double known = across.get(k);
		if (known != null) return known;
		WalkMap map = WalkMap.get();
		double nearest = GolemEntity.REACH, r = GolemEntity.REACH;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double ax = Math.max(0.0, Math.abs(dx) - 0.5), az = Math.max(0.0, Math.abs(dz) - 0.5), d = Math.sqrt(ax * ax + az * az);
				double tier = d <= GolemEntity.CLOSE ? GolemEntity.CLOSE : d <= 3.0 ? 3.0 : GolemEntity.REACH;
				int x = t.x + dx, z = t.z + dz;
				if (tier >= nearest) continue;
				for (int y = foot - 3; y <= foot + 3; y++) {
					if (map.node(x, y, z) < 0 || !mayStandAt(x, y, z, t)) continue;
					double ey = y + GolemEntity.EYE, dy = clamp(ey, t.y, t.y + 1) - ey;
					if (dy <= 0 && d * d + dy * dy > r * r || !clearTo(x + 0.5, ey, z + 0.5, t.x, t.y, t.z)) continue;
					nearest = tier;
					break;
				}
			}
		}
		across.put(k, nearest);
		return nearest;
	}

	private static double clamp(double v, double min, double max) {
		return v < min ? min : v > max ? max : v;
	}

	/** Nothing solid between its eye and the block's middle but leaves and logs: a quarter block at a time. */
	private boolean clearTo(double ex, double ey, double ez, int tx, int ty, int tz) {
		double dx = tx + 0.5 - ex, dy = ty + 0.5 - ey, dz = tz + 0.5 - ez;
		int n = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 4.0);
		int lx = Integer.MIN_VALUE, ly = 0, lz = 0;
		for (int i = 1; i < n; i++) {
			double f = i / (double) n;
			int x = MathHelper.floor(ex + dx * f), y = MathHelper.floor(ey + dy * f), z = MathHelper.floor(ez + dz * f);
			if (x == lx && y == ly && z == lz) continue;
			lx = x;
			ly = y;
			lz = z;
			if (x == tx && y == ty && z == tz) return true;
			int id = world.getBlockId(x, y, z);
			if (id == 0 || Tree.isLog(id) || Tree.isLeaves(id)) continue;
			Block b = Block.BLOCKS[id];
			if (b != null && b.getCollisionShape(world, x, y, z) != null) return false;
		}
		return true;
	}

	/** Not on the log it's cutting. */
	@Override
	protected boolean mayStandAt(int x, int y, int z, Target t) {
		return t.rank == PLANT || !(x == t.x && z == t.z && y - 1 == t.y);
	}

	/** Its feet may be as far below a log as the tallest tree. */
	@Override
	protected int standBelow() {
		return Tree.TALL + 2;
	}

	@Override
	protected void abandon(Target t) {
		ticks = 0;
		progress = 0.0F;
	}

	@Override
	protected int restAfter() {
		switch (piece) {
			case PLANT:
				// A held right-click places again every 5 ticks (beta's tps / 4), as the hoe sows.
				return 5 * GolemEntity.EXCHANGE;
			default:
				return super.restAfter();
		}
	}

	// Kept with the world: the tree it's felling and the stumps it has to plant.

	@Override
	protected void write(NbtCompound nbt) {
		if (felling != null) {
			nbt.putInt("FellingX", felling.x);
			nbt.putInt("FellingY", felling.y);
			nbt.putInt("FellingZ", felling.z);
		}
		NbtList left = new NbtList();
		for (Tree t : unfinished) {
			NbtCompound c = new NbtCompound();
			c.putInt("X", t.x);
			c.putInt("Y", t.y);
			c.putInt("Z", t.z);
			left.add(c);
		}
		nbt.put("Unfinished", left);
		NbtList list = new NbtList();
		for (Stump s : stumps) {
			NbtCompound c = new NbtCompound();
			c.putInt("X", s.x);
			c.putInt("Y", s.y);
			c.putInt("Z", s.z);
			c.putInt("Sapling", s.saplingId);
			c.putInt("Kind", s.saplingMeta);
			c.putLong("Since", s.since);
			list.add(c);
		}
		nbt.put("Stumps", list);
	}

	@Override
	protected void read(NbtCompound nbt) {
		// Only its foot: what stands on it is looked at as it looks for work, once the land is in.
		felling = nbt.contains("FellingX") ? Tree.foot(nbt.getInt("FellingX"), nbt.getInt("FellingY"), nbt.getInt("FellingZ")) : null;
		unfinished.clear();
		NbtList left = nbt.getList("Unfinished");
		for (int i = 0; i < left.size() && i < MOST_UNFINISHED; i++) {
			NbtCompound c = (NbtCompound) left.get(i);
			unfinished.add(Tree.foot(c.getInt("X"), c.getInt("Y"), c.getInt("Z")));
		}
		stumps.clear();
		NbtList list = nbt.getList("Stumps");
		for (int i = 0; i < list.size(); i++) {
			NbtCompound c = (NbtCompound) list.get(i);
			int id = c.getInt("Sapling");
			if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) continue;
			stumps.add(new Stump(c.getInt("X"), c.getInt("Y"), c.getInt("Z"), id, c.getInt("Kind"), c.getLong("Since")));
		}
	}

	@Override
	protected String describe() {
		String s = stumps.size() + " stumps to plant" + (felling != null ? ", felling the " + felling : "") + (unfinished.isEmpty() ? "" : ", " + unfinished.size() + " left unfinished");
		return ticks > 0 && piece == Piece.CHOP ? "chopping, " + Math.round(progress * 100) + "%, " + s : s;
	}
}
