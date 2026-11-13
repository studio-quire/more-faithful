package org.studioquire.faithful.golems;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * What a golem does with the tool it was given. The loop round a job is every job's ({@link Worker}): find the nearest work of its kind in its area that it can safely walk to and
 * reach, walk there, do one, pick up what drops, and when full or out of work carry everything to its chest, taking a spare tool there when its own breaks and iron to mend itself
 * when it's cracked. A job says only what counts
 * as work ({@link #findWork}, {@link #stillWork}) and how one piece of it is done ({@link #work}); the rest has defaults to override.
 * <p>
 * Something lying about ({@code new Target(itemEntity, rank)}) is the loop's own kind of work: it walks to where it would touch it and steps into it, as a player picks things up, so a
 * job only lists what it wants picked up and says while it still does; {@link #work}, {@link #reaches} and {@link #mayStandAt} are never asked about one.
 * <p>
 * A new job is a subclass and a line in {@link Golems#init}: {@code job("hoe", HoeJob::new)}, keyed by the tool class that sets it. Runs only where the world runs.
 */
public abstract class Job {
	protected final GolemEntity golem;
	protected final World world;

	protected Job(GolemEntity golem) {
		this.golem = golem;
		this.world = golem.world;
	}

	/** The tool class that sets it: {@code Tools.Tool.toolClass}. A spare is any tool of this class. */
	public abstract String kind();

	/**
	 * All the work there is in its area now, each with its rank. It needn't ask whether the golem can get to it: the loop passes over what it can't reach, and takes the highest rank it
	 * can, nearest first. Called each time it looks for its next piece, so keep it to reading the area.
	 */
	protected abstract void findWork(Area area, List<Target> found);

	/** Whether this is still work: asked as it walks there and each tick it works. */
	protected abstract boolean stillWork(Target target);

	/** A tick of doing one piece, on work it can reach. True once the piece is over, done or not; then it rests ({@link #restAfter}) and picks up what dropped. */
	protected abstract boolean work(Target target);

	/** A piece left unfinished: it was called away, the tool went, the target changed or got out of reach. Forget any progress on it. */
	protected void abandon(Target target) {
	}

	/** Ticks of rest after a piece, the pause a player has between two: 6 ticks between blocks (5, and one to start the next), at the exchange. */
	protected int restAfter() {
		return 6 * GolemEntity.EXCHANGE;
	}

	/**
	 * Whether it can work the target from a place with its eye here: by default within a player's reach ({@link GolemEntity#REACH}) of the nearest part of it, and nothing solid in
	 * the way. The loop walks it nearer than that wherever it can ({@link GolemEntity#CLOSE}); this is how far it may stretch where it can't.
	 */
	protected boolean reaches(double eyeX, double eyeY, double eyeZ, Target target) {
		if (distance(eyeX, eyeY, eyeZ, target) > GolemEntity.REACH) return false;
		return target.entity != null ? sees(eyeX, eyeY, eyeZ, target.centerX(), target.centerY(), target.centerZ(), Integer.MIN_VALUE, 0, 0) : seesBlock(eyeX, eyeY, eyeZ, target.x, target.y, target.z);
	}

	/** From an eye to the nearest part of the target: of its block, or of the entity's box. */
	protected final double distance(double eyeX, double eyeY, double eyeZ, Target target) {
		Box box = target.entity != null ? target.entity.boundingBox : null;
		double nx = clamp(eyeX, box != null ? box.minX : target.x, box != null ? box.maxX : target.x + 1);
		double ny = clamp(eyeY, box != null ? box.minY : target.y, box != null ? box.maxY : target.y + 1);
		double nz = clamp(eyeZ, box != null ? box.minZ : target.z, box != null ? box.maxZ : target.z + 1);
		double dx = nx - eyeX, dy = ny - eyeY, dz = nz - eyeZ;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Whether it may stand with its feet in this cell to work the target: the pickaxe won't stand on the block it's taking. */
	protected boolean mayStandAt(int x, int y, int z, Target target) {
		return true;
	}

	/**
	 * How far off it stands to work this, across the land from its middle to the target's: null to walk right up to it, as a person goes up to their work (the default); a range
	 * {@code {nearest, furthest}} to stand off at, as the bow does. The loop looks for somewhere to stand in that ring, and while it works, steps back from a target that comes
	 * nearer than the first, wherever there's somewhere safe to step back to, working on as it goes.
	 */
	protected double[] standOff(Target target) {
		return null;
	}

	/** Whether, out of work, it walks its rounds of its area, strolling from place to place with pauses as beta's mobs wander, rather than waiting at its post. A guard's way. */
	protected boolean patrols() {
		return false;
	}

	/**
	 * How far round a target the loop looks for somewhere to stand, across, where it can't get within {@link GolemEntity#CLOSE} of it. It has to cover every place {@link #reaches}
	 * can say yes from: with a player's reach of 4 from a player's eye, 1.62 up, four across, five below (a block four over its head) and three above.
	 */
	protected int standAcross() {
		return 4;
	}

	/** How far below a target its feet may be, and above (see {@link #standAcross}). */
	protected int standBelow() {
		return 5;
	}

	protected int standAbove() {
		return 3;
	}

	/** Whether it's seen swinging its tool this tick; {@code atWork} is whether it's at a piece now. A pick swings all the while it's at a block. */
	protected boolean swinging(boolean atWork) {
		return atWork;
	}

	/** How long it waits at its post, out of work, before it looks again: 10 seconds. */
	protected int lookEvery() {
		return 200;
	}

	/**
	 * How many like this its work waiting could use, all its slots together: the hoe's seeds for the gaps in its field, the axe's saplings for its stumps. As many are kept back when it
	 * empties what it carries, and when it's out of other work and short, it fetches the rest from its chest, whoever put them there.
	 */
	protected int wants(ItemStack stack) {
		return 0;
	}

	/** What it needs back after a restart, past what the golem keeps (its tool, post, area and what it carries); a tool of the same kind handed to it carries this over too. */
	protected void write(NbtCompound nbt) {
	}

	protected void read(NbtCompound nbt) {
	}

	/** What it's doing, for a test to print. */
	protected String describe() {
		return "";
	}

	// For jobs to use.

	/** A block it can see from here: a ray to its middle or the middle of its top meets nothing else on the way but plants and snow. */
	protected boolean seesBlock(double eyeX, double eyeY, double eyeZ, int x, int y, int z) {
		return sees(eyeX, eyeY, eyeZ, x + 0.5, y + 0.5, z + 0.5, x, y, z) || sees(eyeX, eyeY, eyeZ, x + 0.5, y + 0.95, z + 0.5, x, y, z);
	}

	private boolean sees(double ex, double ey, double ez, double tx, double ty, double tz, int bx, int by, int bz) {
		HitResult hit = world.raycast(Vec3d.createCached(ex, ey, ez), Vec3d.createCached(tx, ty, tz));
		if (hit == null || (hit.blockX == bx && hit.blockY == by && hit.blockZ == bz)) return true;
		// Beta's ray stops on tall grass and flowers, as it lets a player pick them; to a golem they're no wall.
		return Ground.isCover(world.getBlockId(hit.blockX, hit.blockY, hit.blockZ));
	}

	/** Breaks a block as a player's hand does: its sound and pieces, its drops where it stood, and the tool's wear. */
	protected void breakBlock(int x, int y, int z) {
		int id = world.getBlockId(x, y, z), meta = world.getBlockMeta(x, y, z);
		Block block = Block.BLOCKS[id];
		if (block == null) return;
		world.worldEvent(2001, x, y, z, id + meta * 256);
		boolean gone = world.setBlock(x, y, z, 0);
		if (gone) block.onMetadataChange(world, x, y, z, meta);
		golem.wearOnBlock(id, x, y, z);
		if (gone) block.dropStacks(world, x, y, z, meta);
		golem.worked++;
	}

	private static double clamp(double v, double min, double max) {
		return v < min ? min : v > max ? max : v;
	}
}
