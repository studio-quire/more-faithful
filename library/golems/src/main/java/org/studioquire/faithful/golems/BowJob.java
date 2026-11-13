package org.studioquire.faithful.golems;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.studioquire.oldfaithful.core.api.Arrows;
import org.studioquire.oldfaithful.core.api.Sounds;

/**
 * The bow: an archer. It shoots the hostiles in its area, and anything fighting it, as beta's skeleton shoots a player (the user's measure, 2026-11-10, as the sword's is a zombie's): under
 * 10 blocks off, an arrow every {@link #SHOOT_EVERY} ticks at most, aimed as a skeleton aims and as wild, and only at what it can see. Every hostile, creepers and ghasts too; what's after
 * someone first. Out of work it walks its rounds of its area, as the sword does (the user, 2026-11-10).
 * <p>
 * It moves in a fight (the user, 2026-11-10, in place of the first build's sentry that never left its post, the rate to balance it): as a skeleton it walks at what it's after until it can
 * shoot it, then stands and shoots; and better than a skeleton, it steps back from one that comes nearer than {@link #NEAREST} (a creeper {@link #CREEPER_NEAREST}, past its blast),
 * shooting as it goes, wherever there's somewhere safe to step back to. Cornered (a wall, a drop, a tower top) it stands and shoots. Where it goes to shoot from is the nearest place in
 * the ring from {@code NEAREST} to {@link #FURTHEST} round its target with a clear shot.
 * <p>
 * It looses only when its arrow, flying true, would strike its target (or another hostile) before anything else: a wall, its owner, another golem, a sheep. A skeleton's wildness then
 * sends some astray, and a stray hits whatever it meets, as beta's arrows do. It shoots what it carries, a few of each kind it takes from its chest, the first it carries at anything,
 * as a player's bow takes the first arrows it finds (the user, 2026-11-10). Missed arrows are gone: it never goes to gather them, and beta lets no one pick up an arrow that a
 * player didn't shoot.
 */
final class BowJob extends Job {
	/** A skeleton's reach: under 10 blocks from where it stands to where the target stands. */
	static final double RANGE = 10.0;
	/** Its rate, the lever to balance it by (the user, 2026-11-10): an arrow, then this many ticks before the next. 30 is a skeleton's. */
	static final int SHOOT_EVERY = 30;
	/** What it uses: {@link #SHOOT_EVERY}, but for a survey trying others ({@code ArcherRun}). */
	static int shootEvery = SHOOT_EVERY;
	/**
	 * How near it lets what it shoots come before it steps back: past a zombie's or a spider's reach with a step to spare; and a creeper further, as one lit keeps burning within 7 and
	 * its blast reaches 6. The ring it looks for a place to shoot from reaches out to 8: its arrows, aimed as a skeleton's, fall short past about that.
	 */
	static final double NEAREST = 4.0, CREEPER_NEAREST = 6.0, FURTHEST = 8.0;
	/** A skeleton's arrow, slow and wild ({@code setVelocity(..., 0.6F, 12.0F)}): a player's flies at 1.5 and as wild as 1. */
	private static final float SPEED = 0.6F, WILD = 12.0F;
	/** How many of each kind of arrow it takes from its chest and keeps by it. */
	static final int QUIVER = 16;
	/** A target out of its line stays its target this long, the bow still on it, before it looks for another: something passing behind a post. */
	private static final int LOST_SIGHT = 20;
	private static final int AFTER_SOMEONE = 2, HOSTILE = 1;

	private int sightOf = -1, seenAt, lookedFurther;
	/** Ticks it was ready to shoot and held for want of a clear line: a test's to read. */
	int held;
	/** Arrows it has loosed since it was loaded: a test's to read. */
	int shots;

	BowJob(GolemEntity golem) {
		super(golem);
	}

	@Override
	public String kind() {
		return Golems.BOW;
	}

	/** Every hostile in its area, and anything fighting it from near enough to shoot back at: the loop finds somewhere to shoot each from. */
	@Override
	protected void findWork(Area area, List<Target> found) {
		if (arrowSlot(null) < 0) return;
		for (LivingEntity e : around(area)) {
			if (guards(e, area)) found.add(new Target(e, rank(e)));
		}
	}

	/** What's about its area, as far past it as it walks, and round where it stands as far as it shoots. */
	private List<LivingEntity> around(Area area) {
		int m = Worker.MARGIN;
		double minX = Math.min(area.minX - m, golem.x - RANGE), minZ = Math.min(area.minZ - m, golem.z - RANGE);
		double maxX = Math.max(area.maxX + 1 + m, golem.x + RANGE), maxZ = Math.max(area.maxZ + 1 + m, golem.z + RANGE);
		@SuppressWarnings("unchecked")
		List<LivingEntity> near = world.collectEntitiesByClass(LivingEntity.class, Box.createCached(minX, 0.0, minZ, maxX, 128.0, maxZ));
		return near;
	}

	/** A hostile it guards against: in its area, or fighting it from within a shot and its walk past the area. */
	private boolean guards(LivingEntity e, Area area) {
		if (!e.isAlive() || !foe(e)) return false;
		if (area.contains(MathHelper.floor(e.x), MathHelper.floor(e.z))) return true;
		if (targetOf(e) != golem) return false;
		double dx = e.x - golem.x, dy = e.y - golem.y, dz = e.z - golem.z, far = RANGE + Worker.MARGIN;
		return dx * dx + dy * dy + dz * dz < far * far;
	}

	/** Every hostile: the sword's foes, and the creepers and ghasts it leaves alone (the user, 2026-11-10). */
	static boolean foe(LivingEntity e) {
		return e instanceof CreeperEntity || e instanceof GhastEntity || SwordJob.foe(e);
	}

	private static Entity targetOf(LivingEntity e) {
		return e instanceof MobEntity ? ((MobEntity) e).getTarget() : null;
	}

	private static int rank(LivingEntity e) {
		return targetOf(e) != null ? AFTER_SOMEONE : HOSTILE;
	}

	/** While it has an arrow and the target is still a hostile in reach; and it turns to something after someone when this one is after no one, looked for twice a second. */
	@Override
	protected boolean stillWork(Target t) {
		LivingEntity e = (LivingEntity) t.entity;
		if (arrowSlot(null) < 0 || !guards(e, golem.area())) return false;
		if (rank(e) == AFTER_SOMEONE || golem.age - lookedFurther < 10) return true;
		lookedFurther = golem.age;
		double ex = golem.reachX(), ey = golem.reachY() - 0.1, ez = golem.reachZ();
		for (LivingEntity other : around(golem.area())) {
			if (other != e && rank(other) == AFTER_SOMEONE && guards(other, golem.area()) && aim(ex, ey, ez, other) != null) return false;
		}
		return true;
	}

	/** Under 10 blocks, its arrow would fly to it from this eye; or it did a moment ago, so a target passing behind something keeps its bow on it. */
	@Override
	protected boolean reaches(double eyeX, double eyeY, double eyeZ, Target t) {
		LivingEntity e = (LivingEntity) t.entity;
		double dx = e.x - eyeX, dy = e.y - (eyeY - GolemEntity.EYE), dz = e.z - eyeZ;
		if (dx * dx + dy * dy + dz * dz >= RANGE * RANGE) return false;
		if (aim(eyeX, eyeY - 0.1, eyeZ, e) != null) {
			sightOf = e.id;
			seenAt = golem.age;
			return true;
		}
		return e.id == sightOf && golem.age - seenAt <= LOST_SIGHT;
	}

	/** Off what it shoots, in a ring round it: a creeper further off. */
	@Override
	protected double[] standOff(Target target) {
		return new double[] {target.entity instanceof CreeperEntity ? CREEPER_NEAREST : NEAREST, FURTHEST};
	}

	/** As far round a target as it looks for a place to shoot from. */
	@Override
	protected int standAcross() {
		return (int) Math.ceil(FURTHEST);
	}

	/** It walks its rounds while it has arrows; with none it waits at its post with its bow down. */
	@Override
	protected boolean patrols() {
		return arrowSlot(null) >= 0;
	}

	/**
	 * A tick of a fight, which is one piece: its eyes on where it aims, an arrow when it may and its line is clear, the first as soon as it has one, as a skeleton's (the user,
	 * 2026-11-10: no half-second draw). The arrow is beta's own entity for the kind it takes ({@link Arrows}), from its eye as beta's bow places one, not from over its head as a
	 * skeleton's starts, flown as a skeleton's.
	 */
	@Override
	protected boolean work(Target t) {
		LivingEntity e = (LivingEntity) t.entity;
		// Where beta's arrow starts: its eye, less a tenth, and 0.16 to the side of the way it faces.
		float yaw = (float) (Math.atan2(e.z - golem.z, e.x - golem.x) * 180.0 / Math.PI) - 90.0F;
		float turn = yaw / 180.0F * (float) Math.PI;
		double sx = golem.x - MathHelper.cos(turn) * 0.16F, sy = golem.y + GolemEntity.EYE - 0.1, sz = golem.z - MathHelper.sin(turn) * 0.16F;
		Shot shot = aim(sx, sy, sz, e);
		// No clear line: the loop keeps its eyes on the target's middle.
		if (shot == null) {
			if (golem.attackCooldown <= 0) held++;
			return false;
		}
		sightOf = e.id;
		seenAt = golem.age;
		golem.aimAt((float) e.x, (float) shot.height, (float) e.z);
		if (golem.attackCooldown > 0) return false;
		int slot = arrowSlot(e);
		if (slot < 0) return false;
		golem.yaw = yaw;
		ArrowEntity arrow = Arrows.make(golem.slot(slot), world, golem);
		if (arrow == null) return false;
		arrow.setVelocity(shot.dx, shot.dy, shot.dz, SPEED, (float) shot.spread);
		world.spawnEntity(arrow);
		Sounds.play(world, golem.x, golem.y, golem.z, "random.bow", 1.0F, 1.0F / (world.random.nextFloat() * 0.4F + 0.8F));
		ItemStack arrows = golem.slot(slot);
		arrows.count--;
		golem.setSlot(slot, arrows);
		// A bow wears as a player's does: a use a shot, which beta's bow never has, so only under World Rules' Bows Wear Out.
		golem.tool().damage(1, golem);
		golem.afterWear();
		golem.attackCooldown = shootEvery;
		shots++;
		return false;
	}

	private static final class Shot {
		final double dx, dy, dz, height, spread;

		Shot(double dx, double dy, double dz, double height, double spread) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.height = height;
			this.spread = spread;
		}
	}

	/**
	 * Where to send an arrow from here to strike the target, flying true, or null if it can't. As a skeleton aims: at the eyes less a fifth of a block, raised a fifth of a block for each
	 * block across, which drops onto a target out to about 8 blocks whatever the height between. Then higher up the body, and just over the head (an arrow strikes anything it passes within
	 * 0.3 of), for a target only partly in sight: a creeper at the foot of the wall it stands on. Each then aimed as a skeleton's arrow standing here would fly ({@link ArrowAim}), as its own
	 * starts at its eye rather than over its head.
	 */
	private Shot aim(double sx, double sy, double sz, LivingEntity e) {
		double dx = e.x - sx, dz = e.z - sz, across = Math.sqrt(dx * dx + dz * dz);
		double top = e.boundingBox.maxY;
		double[] heights = {e.y + e.getEyeHeight() - 0.2, top - 0.1, top + 0.25};
		for (double h : heights) {
			double[] aimed = ArrowAim.aim(across, h - sy + across * 0.2, SPEED, WILD);
			if (flies(sx, sy, sz, dx, aimed[0], dz, e)) return new Shot(dx, aimed[0], dz, h, aimed[1]);
		}
		return null;
	}

	/**
	 * Whether an arrow sent this way at a skeleton's speed, with none of its wildness, strikes the target or another hostile first: beta's arrow tick, step by step (its move, then 0.99 of its
	 * speed and 0.03 down), meeting blocks as beta's arrow does (through what has no collision) and anything collidable it passes within 0.3 of.
	 */
	private boolean flies(double sx, double sy, double sz, double dx, double dy, double dz, LivingEntity target) {
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1.0E-4) return false;
		double vx = dx / length * SPEED, vy = dy / length * SPEED, vz = dz / length * SPEED;
		Box b = target.boundingBox;
		List<?> near = world.getEntities(golem, Box.createCached(Math.min(sx, b.minX) - 2.0, Math.min(sy, b.minY) - 3.0, Math.min(sz, b.minZ) - 2.0, Math.max(sx, b.maxX) + 2.0,
				Math.max(sy, b.maxY) + 4.0, Math.max(sz, b.maxZ) + 2.0));
		double x = sx, y = sy, z = sz;
		for (int tick = 0; tick < 60; tick++) {
			double nx = x + vx, ny = y + vy, nz = z + vz;
			HitResult wall = world.raycast(Vec3d.createCached(x, y, z), Vec3d.createCached(nx, ny, nz), false, true);
			Vec3d from = Vec3d.createCached(x, y, z), to = wall != null ? Vec3d.createCached(wall.pos.x, wall.pos.y, wall.pos.z) : Vec3d.createCached(nx, ny, nz);
			Entity first = null;
			double nearest = Double.MAX_VALUE;
			for (Object o : near) {
				Entity other = (Entity) o;
				if (!other.isCollidable()) continue;
				HitResult hit = other.boundingBox.expand(0.3, 0.3, 0.3).raycast(from, to);
				if (hit == null) continue;
				double d = from.distanceTo(hit.pos);
				if (d < nearest) {
					nearest = d;
					first = other;
				}
			}
			if (first != null) return first == target || first instanceof LivingEntity && foe((LivingEntity) first);
			if (wall != null) return false;
			x = nx;
			y = ny;
			z = nz;
			vx *= 0.99;
			vy = vy * 0.99 - 0.03;
			vz *= 0.99;
			if (y < b.minY - 4.0) return false;
		}
		return false;
	}

	/**
	 * The arrows it shoots next: the first it carries, whatever their kind, at whatever it shoots (the user, 2026-11-10: Explosive Arrows needn't be kept for creepers), as a player's
	 * bow takes the first arrows it finds. Its slots fill from its chest in the chest's order, top left first, so the player chooses by where in the chest they put each kind.
	 */
	private int arrowSlot(LivingEntity target) {
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			if (Arrows.isArrow(golem.slot(i))) return i;
		}
		return -1;
	}

	/** A quiver of each kind: kept back when it puts things in its chest, and fetched from it when it's out of other work and has fewer. */
	@Override
	protected int wants(ItemStack stack) {
		return Arrows.isArrow(stack) ? QUIVER : 0;
	}

	/** Its bow held drawn all the while it's at a fight. */
	@Override
	protected boolean swinging(boolean atWork) {
		return atWork;
	}

	/** An archer looks about twice a second. */
	@Override
	protected int lookEvery() {
		return 10;
	}

	@Override
	protected String describe() {
		int arrows = 0;
		for (int i = 0; i < GolemEntity.SLOTS; i++) {
			if (Arrows.isArrow(golem.slot(i))) arrows += golem.slot(i).count;
		}
		return shots + " shots, " + arrows + " arrows" + (held > 0 ? ", held " + held + " ticks for a clear line" : "");
	}
}
