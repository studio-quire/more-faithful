package org.studioquire.faithful.golems;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Monster;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * The sword: a guard. It goes for the monsters in its area as beta's own melee monsters go for a player: it walks right up to one and hits it from where a zombie would, no more than once
 * a second (a zombie's rate), with the sword's own damage, the sword wearing a point a hit as a player's does. What's after someone (its owner, another player, the golem itself) comes
 * first, then the nearest. Beta's monsters turn on whatever hits them, so a fight costs it health: a zombie gets in a hit or two before an iron sword's three put it down.
 * <p>
 * Anything fighting it is answered from as far past its area as it walks; anything else is left once it's out of the area. When the fighting's over it picks up what its kills
 * dropped, takes it home, and walks its rounds of its area, looking about every second.
 * <p>
 * Left alone: creepers and ghasts, as beta's tame wolves leave them (a creeper hit at a zombie's rate blows up before it dies to any sword but a diamond one, taking the ground it
 * guards and half the golem with it); zombie pigmen unless they're angry at someone; wolves unless wild and angry; animals, players and golems.
 */
final class SwordJob extends Job {
	/** A zombie's reach: under 2 blocks between them, centre to centre. */
	private static final double HIT_RANGE = 2.0;
	/** A zombie's rate: a hit, then 20 ticks before the next. */
	private static final int HIT_EVERY = 20;
	/** A swing is 8 ticks on a player, at the exchange. */
	private static final int SWING_TICKS = 8 * GolemEntity.EXCHANGE;
	private static final int AFTER_SOMEONE = 2, MONSTER = 1, SPOILS = 0;

	/** What its kills dropped and it hasn't picked up yet: the loop picks them up as it does anything lying about. */
	private final List<ItemEntity> spoils = new ArrayList<>();
	private int swingEnd;
	/** Hits it has landed since it was loaded: a test's to read. */
	int hits;

	SwordJob(GolemEntity golem) {
		super(golem);
	}

	@Override
	public String kind() {
		return "sword";
	}

	@Override
	protected void findWork(Area area, List<Target> found) {
		int m = Worker.MARGIN;
		Box box = Box.createCached(area.minX - m, 0.0, area.minZ - m, area.maxX + 1 + m, 128.0, area.maxZ + 1 + m);
		for (Object o : world.collectEntitiesByClass(LivingEntity.class, box)) {
			LivingEntity e = (LivingEntity) o;
			if (guards(e)) found.add(new Target(e, targetOf(e) != null ? AFTER_SOMEONE : MONSTER));
		}
		spoils.removeIf(item -> item.dead);
		for (ItemEntity item : spoils) {
			if (golem.wouldTake(item) && golem.hasRoomFor(item.stack)) found.add(new Target(item, SPOILS));
		}
	}

	/** A foe it should be fighting: in its area, or fighting it from no further past its edge than it walks. */
	private boolean guards(LivingEntity e) {
		if (!e.isAlive() || !foe(e)) return false;
		Area a = golem.area();
		int x = MathHelper.floor(e.x), z = MathHelper.floor(e.z), m = Worker.MARGIN;
		if (a.contains(x, z)) return true;
		return targetOf(e) == golem && x >= a.minX - m && x <= a.maxX + m && z >= a.minZ - m && z <= a.maxZ + m;
	}

	/** What would attack a player on sight, or is angry: beta's monsters, angry zombie pigmen and wild wolves; never a creeper or a ghast. */
	static boolean foe(LivingEntity e) {
		if (e instanceof CreeperEntity || e instanceof GhastEntity) return false;
		if (e instanceof PigZombieEntity) return ((PigZombieEntity) e).getTarget() != null;
		if (e instanceof WolfEntity) return !((WolfEntity) e).isTamed() && ((WolfEntity) e).isAngry();
		return e instanceof Monster;
	}

	private static Entity targetOf(LivingEntity e) {
		return e instanceof MobEntity ? ((MobEntity) e).getTarget() : null;
	}

	@Override
	protected boolean stillWork(Target t) {
		if (t.entity instanceof ItemEntity) {
			ItemEntity item = (ItemEntity) t.entity;
			return !item.dead && spoils.contains(item) && golem.hasRoomFor(item.stack);
		}
		return guards((LivingEntity) t.entity);
	}

	/** A foe: as a zombie reaches, under 2 blocks from where it stands to where the foe stands, their bodies level with each other somewhere, and nothing in the way. */
	@Override
	protected boolean reaches(double eyeX, double eyeY, double eyeZ, Target t) {
		double feet = eyeY - GolemEntity.EYE;
		Box b = t.entity.boundingBox;
		if (b.maxY <= feet || b.minY >= feet + GolemEntity.HEIGHT) return false;
		double dx = t.entity.x - eyeX, dy = b.minY - feet, dz = t.entity.z - eyeZ;
		return dx * dx + dy * dy + dz * dz < HIT_RANGE * HIT_RANGE && super.reaches(eyeX, eyeY, eyeZ, t);
	}

	/** Under 2 blocks, its body level with the foe's: from the cells beside it, one up or one down. */
	@Override
	protected int standAcross() {
		return 1;
	}

	@Override
	protected int standBelow() {
		return 1;
	}

	@Override
	protected int standAbove() {
		return 1;
	}

	/** A hit when it may, and nothing else: the fight is over when the foe is dead, or gone from where it guards ({@link #stillWork}). */
	@Override
	protected boolean work(Target t) {
		if (golem.attackCooldown <= 0) hit((LivingEntity) t.entity);
		return false;
	}

	/** A player's blow with the same sword: its damage, a point of wear. What a killing blow drops is noted, to be picked up once the fighting's done. */
	private void hit(LivingEntity e) {
		ItemStack sword = golem.tool();
		int damage = sword.getAttackDamage(e);
		Box around = e.boundingBox.expand(1.5, 1.5, 1.5);
		List<?> before = e.health <= damage ? world.collectEntitiesByClass(ItemEntity.class, around) : null;
		e.damage(golem, damage);
		sword.getItem().postHit(sword, e, golem);
		golem.afterWear();
		golem.attackCooldown = HIT_EVERY;
		swingEnd = golem.age + SWING_TICKS;
		hits++;
		if (before == null || e.isAlive()) return;
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, around)) {
			if (!before.contains(o) && spoils.size() < 64) spoils.add((ItemEntity) o);
		}
	}

	/** Each blow is seen as a swing, wherever it is by the swing's end: the foe a blow knocks back has it walking again at once. */
	@Override
	protected boolean swinging(boolean atWork) {
		return golem.age < swingEnd;
	}

	/** A guard looks about every second. */
	@Override
	protected int lookEvery() {
		return 20;
	}

	/** Out of work, it walks its rounds of its area (the user, 2026-11-10), poles' or the square round its post. */
	@Override
	protected boolean patrols() {
		return true;
	}

	@Override
	protected String describe() {
		return hits + " hits" + (spoils.isEmpty() ? "" : ", " + spoils.size() + " things dropped to pick up");
	}
}
