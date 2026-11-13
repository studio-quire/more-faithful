package org.studioquire.faithful.golems;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SeedsItem;
import net.minecraft.nbt.NbtCompound;
import org.studioquire.oldfaithful.core.api.Crop;
import org.studioquire.oldfaithful.core.api.Sounds;

/**
 * The hoe: a farmer. In its area it reaps ripe wheat and sows empty farmland from the seeds it carries, one cell at a time: a cell it has just reaped is sown before it reaps
 * another, whenever it has a seed. It keeps its field as it found it: farmland it has seen that has gone back to dirt (trampled, as anything walking on it does in beta, or dried
 * out with nothing growing) is tilled again, once it has a seed to sow there. It breaks no new ground: the field is the player's to lay out. It keeps back as many seeds as it has gaps
 * when it puts its load away, and out of other work with gaps and no seeds, it fetches them from its chest (the loop's, through {@link #wants}).
 * <p>
 * Any crop by wheat's rules is reaped when ripe (vanilla's wheat, and a mod's {@link Crop}), and any {@code SeedsItem} is sown, through the seed's own use. Its field lies within
 * {@value #FIELD_RANGE} blocks up or down of the floor under its post. Each piece takes twice a player's ticks: 6 after breaking a crop, 5 after a right-click.
 */
final class HoeJob extends Job {
	/**
	 * Uses of its hoe a crop reaped (the user's call, 2026-11-09: the hoe wears from harvesting, one a crop). A player reaps by hand and a hoe wears only from tilling, so 0 here would
	 * be that rule exactly, and the farm would cost nothing to run. Tilling wears one, as a player's.
	 */
	static final int WEAR_PER_HARVEST = 1;
	/** How far up or down from the floor under its post its field may lie. */
	static final int FIELD_RANGE = 8;
	/** Ranks, which also say what kind of work a target is. Gaps are filled before more are made: sowing first, then tilling to sow, then reaping. */
	private static final int SOW = 2, TILL = 1, REAP = 0;
	/** The working part of a piece, the arm's stroke: half a player's 8-tick swing, at the exchange. The strike lands at its end. */
	private static final int STROKE = 4 * GolemEntity.EXCHANGE;
	/** A player's gap between two: 6 ticks after breaking a block (5, and one to start the next), 5 between right-clicks held down. */
	private static final int AFTER_BREAK = 6 * GolemEntity.EXCHANGE, AFTER_USE = 5 * GolemEntity.EXCHANGE;

	/**
	 * Its field: farmland it has seen in its area, by place. Forgotten when a cell turns into anything but farmland, dirt or grass, and when its hoe is taken back; another hoe handed
	 * to it keeps it.
	 */
	private final Set<Long> field = new HashSet<>();
	private int stroke, last = REAP, gaps;

	HoeJob(GolemEntity golem) {
		super(golem);
	}

	@Override
	public String kind() {
		return "hoe";
	}

	/** Every target is the cell above the ground: the crop, or the air it's sown into. */
	@Override
	protected void findWork(Area area, List<Target> found) {
		boolean seeds = seedSlot() >= 0;
		int floor = golem.postY() - 1, low = Math.max(1, floor - FIELD_RANGE);
		gaps = 0;
		for (int x = area.minX; x <= area.maxX; x++) {
			for (int z = area.minZ; z <= area.maxZ; z++) {
				if (!world.isPosLoaded(x, 64, z)) continue;
				// Farmland blocks light, so none lies above the column's height.
				for (int y = Math.min(world.getTopY(x, z) - 1, Math.min(floor + FIELD_RANGE, 125)); y >= low; y--) {
					if (world.getBlockId(x, y, z) != Block.FARMLAND.id) continue;
					remember(x, y, z);
					int above = world.getBlockId(x, y + 1, z);
					if (above == 0) {
						gaps++;
						if (seeds) found.add(new Target(x, y + 1, z, SOW));
					} else if (ripe(x, y + 1, z)) {
						found.add(new Target(x, y + 1, z, REAP));
					}
				}
			}
		}
		for (Iterator<Long> i = field.iterator(); i.hasNext(); ) {
			long c = i.next();
			int x = cellX(c), y = cellY(c), z = cellZ(c);
			// Its area moves with its post when it's handed another hoe: a field left outside it is kept, but not worked.
			if (!area.contains(x, z) || !world.isPosLoaded(x, y, z)) continue;
			int id = world.getBlockId(x, y, z);
			if (id == Block.FARMLAND.id) continue;
			if (id != Block.DIRT.id && id != Block.GRASS_BLOCK.id) {
				i.remove();
				continue;
			}
			if (world.getBlockId(x, y + 1, z) != 0) continue;
			gaps++;
			if (seeds) found.add(new Target(x, y + 1, z, TILL));
		}
	}

	/** Seeds for every gap in its field. */
	@Override
	protected int wants(ItemStack stack) {
		return isSeed(stack) ? gaps : 0;
	}

	@Override
	protected boolean stillWork(Target t) {
		switch (t.rank) {
			case REAP:
				return ripe(t.x, t.y, t.z);
			case SOW:
				return world.getBlockId(t.x, t.y, t.z) == 0 && world.getBlockId(t.x, t.y - 1, t.z) == Block.FARMLAND.id && seedSlot() >= 0;
			default:
				int id = world.getBlockId(t.x, t.y - 1, t.z);
				return (id == Block.DIRT.id || id == Block.GRASS_BLOCK.id) && world.getBlockId(t.x, t.y, t.z) == 0 && seedSlot() >= 0;
		}
	}

	/** It works the cell beside it, not the one it stands in. */
	@Override
	protected boolean mayStandAt(int x, int y, int z, Target t) {
		return x != t.x || z != t.z;
	}

	@Override
	protected boolean work(Target t) {
		if (++stroke < STROKE) return false;
		stroke = 0;
		switch (t.rank) {
			case REAP:
				reap(t);
				break;
			case SOW:
				sow(t);
				break;
			default:
				till(t);
				break;
		}
		last = t.rank;
		return true;
	}

	@Override
	protected void abandon(Target t) {
		stroke = 0;
	}

	/** So that from one strike to the next is a player's gap, doubled: the rest, then the next stroke. */
	@Override
	protected int restAfter() {
		return (last == REAP ? AFTER_BREAK : AFTER_USE) - STROKE;
	}

	private void reap(Target t) {
		breakBlock(t.x, t.y, t.z);
		ItemStack hoe = golem.tool();
		if (WEAR_PER_HARVEST > 0 && hoe != null) {
			hoe.damage(WEAR_PER_HARVEST, golem);
			golem.afterWear();
		}
	}

	/** Through the seed's own use, as a player's right-click on the farmland's top: beta's seeds never look at who's planting. Silent, as beta's sowing is. */
	private void sow(Target t) {
		int slot = seedSlot();
		if (slot < 0) return;
		ItemStack seed = golem.slot(slot);
		if (!seed.getItem().useOnBlock(seed, null, world, t.x, t.y - 1, t.z, 1)) return;
		golem.setSlot(slot, seed);
		golem.worked++;
	}

	/** A hoe's tilling, as beta's: its sound, farmland, one use of the hoe. */
	private void till(Target t) {
		int y = t.y - 1;
		Block farmland = Block.FARMLAND;
		Sounds.play(world, t.x + 0.5, y + 0.5, t.z + 0.5, farmland.soundGroup.getSound(), (farmland.soundGroup.getVolume() + 1.0F) / 2.0F, farmland.soundGroup.getPitch() * 0.8F);
		world.setBlock(t.x, y, t.z, farmland.id);
		ItemStack hoe = golem.tool();
		if (hoe != null) {
			hoe.damage(1, golem);
			golem.afterWear();
		}
		golem.worked++;
	}

	private boolean ripe(int x, int y, int z) {
		return Block.BLOCKS[world.getBlockId(x, y, z)] instanceof CropBlock && world.getBlockMeta(x, y, z) >= Crop.RIPE;
	}

	private static boolean isSeed(ItemStack stack) {
		return stack != null && stack.count > 0 && stack.getItem() instanceof SeedsItem;
	}

	/** The first slot with seeds in it, or -1. */
	private int seedSlot() {
		for (int i = 0; i < GolemEntity.SLOTS; i++) if (isSeed(golem.slot(i))) return i;
		return -1;
	}

	// Its field: by place in memory, and in its NBT by place relative to its post, a byte each way (its area is within 48 of the post, and the field within FIELD_RANGE of its
	// floor). Written and read against the same post: a hoe handed over again writes it from the new post and reads it back there.

	private void remember(int x, int y, int z) {
		field.add(cell(x, y, z));
	}

	private static long cell(int x, int y, int z) {
		return (long) (x + 0x2000000) << 34 | (long) (z + 0x2000000) << 8 | y & 0xFF;
	}

	private static int cellX(long c) {
		return (int) (c >>> 34) - 0x2000000;
	}

	private static int cellZ(long c) {
		return (int) (c >>> 8 & 0x3FFFFFFL) - 0x2000000;
	}

	private static int cellY(long c) {
		return (int) (c & 0xFF);
	}

	@Override
	protected void write(NbtCompound nbt) {
		int px = golem.postX(), py = golem.postY(), pz = golem.postZ(), n = 0;
		byte[] cells = new byte[field.size() * 3];
		for (long c : field) {
			int dx = cellX(c) - px, dy = cellY(c) - py, dz = cellZ(c) - pz;
			if (dx < -128 || dx > 127 || dy < -128 || dy > 127 || dz < -128 || dz > 127) continue;
			cells[n++] = (byte) dx;
			cells[n++] = (byte) dy;
			cells[n++] = (byte) dz;
		}
		nbt.putByteArray("Field", java.util.Arrays.copyOf(cells, n));
	}

	@Override
	protected void read(NbtCompound nbt) {
		byte[] cells = nbt.getByteArray("Field");
		int px = golem.postX(), py = golem.postY(), pz = golem.postZ();
		for (int i = 0; i + 2 < cells.length; i += 3) field.add(cell(px + cells[i], py + cells[i + 1], pz + cells[i + 2]));
	}

	@Override
	protected String describe() {
		return "(a field of " + field.size() + ", " + gaps + " gaps" + (seedSlot() >= 0 ? ")" : ", no seeds on it)");
	}
}
