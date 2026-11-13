package org.studioquire.faithful.golems;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.Tools;

/**
 * The pickaxe: a leveller. In its area it takes the highest bare ground it can walk to and reach, and never goes below the floor it was put to work on, the block under its post. Bare
 * ground is a block of the land ({@link Ground}) with nothing but air or a plant on it, nothing liquid touching it and nothing hung on its sides, that the pick in its hand gets something
 * from. What it can't reach or mustn't touch is left as it is: a tree on the pillar of earth it stands on, lava behind its wall, a spire too tall to reach the top of. So it peels the
 * land from the top in layers, and leaves a flat yard at its floor with a cut face at the edge of its area.
 * <p>
 * In a pattern a watcher can follow (nearest-first read as wandering): a layer at a time from the top down, and within a layer in rows, back and forth
 * across its area as a lawn is mown, from the corner nearest its post, a row finished before the next and a layer before the one below. What it can't take or reach is skipped and the
 * row carries on.
 * <p>
 * A block takes twice the ticks {@code Block.getHardness(player)} would give a player holding the same pick.
 */
final class PickaxeJob extends Job {
	private int ticks;
	private float progress;

	PickaxeJob(GolemEntity golem) {
		super(golem);
	}

	@Override
	public String kind() {
		return "pickaxe";
	}

	/** Places in a layer's order: an area is no more than 97 across (poles within 48 of its post), so fewer than this. */
	private static final int IN_A_LAYER = 10000;

	/** Every bare block from the floor up, ranked by its height and then by its place in the rows: higher ranks come first. */
	@Override
	protected void findWork(Area area, List<Target> found) {
		int floor = golem.postY();
		for (int x = area.minX; x <= area.maxX; x++) {
			for (int z = area.minZ; z <= area.maxZ; z++) {
				if (!world.isPosLoaded(x, 64, z)) continue;
				int order = rowOrder(area, x, z);
				for (int y = Math.min(world.getTopY(x, z), 126); y >= floor; y--) {
					if (bare(world, x, y, z) && takes(x, y, z)) found.add(new Target(x, y, z, y * IN_A_LAYER + IN_A_LAYER - 1 - order));
				}
			}
		}
	}

	/** Its place in a layer: rows along x, the first on the side of its area nearest its post, each row from the end the last one finished at, starting at the corner nearest the post. */
	private int rowOrder(Area area, int x, int z) {
		boolean fromMinX = golem.postX() - area.minX <= area.maxX - golem.postX(), fromMinZ = golem.postZ() - area.minZ <= area.maxZ - golem.postZ();
		int width = area.maxX - area.minX + 1;
		int row = fromMinZ ? z - area.minZ : area.maxZ - z, along = fromMinX ? x - area.minX : area.maxX - x;
		return row * width + (row % 2 == 0 ? along : width - 1 - along);
	}

	@Override
	protected boolean stillWork(Target t) {
		return t.y >= golem.postY() && golem.area().contains(t.x, t.z) && bare(world, t.x, t.y, t.z) && takes(t.x, t.y, t.z);
	}

	/** A block of the land, nothing on it but air or a plant, nothing liquid beside or under it, and nothing hung on its sides: a torch, a ladder, a sign. */
	static boolean bare(World world, int x, int y, int z) {
		if (!Ground.isGround(world.getBlockId(x, y, z))) return false;
		int above = world.getBlockId(x, y + 1, z);
		if (above != 0 && !Ground.isCover(above)) return false;
		if (Ground.isLiquid(world.getBlockId(x, y - 1, z))) return false;
		for (int d = 0; d < 4; d++) {
			int sx = x + (d == 0 ? 1 : d == 1 ? -1 : 0), sz = z + (d == 2 ? 1 : d == 3 ? -1 : 0);
			int id = world.getBlockId(sx, y, sz);
			if (id == 0 || Ground.isCover(id)) continue;
			Block side = Block.BLOCKS[id];
			if (side == null) continue;
			if (side.material.isFluid() || !side.material.blocksMovement() || side.getCollisionShape(world, sx, y, sz) == null) return false;
		}
		return true;
	}

	/** What the pick in its hand gets something from, as a player's would. */
	private boolean takes(int x, int y, int z) {
		Block block = Block.BLOCKS[world.getBlockId(x, y, z)];
		return block != null && block.getHardness() >= 0.0F && Tools.canHarvest(golem.tool(), block);
	}

	/** Not on the block it's taking. */
	@Override
	protected boolean mayStandAt(int x, int y, int z, Target t) {
		return !(x == t.x && z == t.z && y - 1 == t.y);
	}

	@Override
	protected boolean work(Target t) {
		Block block = Block.BLOCKS[world.getBlockId(t.x, t.y, t.z)];
		ticks++;
		if (ticks % GolemEntity.EXCHANGE == 0) progress += Tools.breakProgress(block, golem.tool(), golem.onGround, golem.inWater());
		// A player hears the block every 4 ticks.
		if (ticks % (4 * GolemEntity.EXCHANGE) == 0) {
			Sounds.play(world, t.x + 0.5, t.y + 0.5, t.z + 0.5, block.soundGroup.getSound(), (block.soundGroup.getVolume() + 1.0F) / 8.0F, block.soundGroup.getPitch() * 0.5F);
		}
		if (progress < 1.0F) return false;
		breakBlock(t.x, t.y, t.z);
		abandon(t);
		return true;
	}

	@Override
	protected void abandon(Target t) {
		ticks = 0;
		progress = 0.0F;
	}

	@Override
	protected String describe() {
		return ticks > 0 ? "mining, " + Math.round(progress * 100) + "%" : "";
	}
}
