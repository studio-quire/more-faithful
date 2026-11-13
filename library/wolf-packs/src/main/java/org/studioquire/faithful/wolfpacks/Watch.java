package org.studioquire.faithful.wolfpacks;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Monster;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.studioquire.faithful.wolfpacks.mixin.MobEntityAccessor;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Sounds;

/**
 * Sitting wolves keep watch (the rule Sitting Wolves Watch), where the world runs. A tamed wolf that's sitting goes for a
 * monster it can see from its seat ({@link #sees}), within {@link #SEES} blocks, if beta's pathfinder has a way for it to where it could bite it;
 * then it walks back to the spot it left and sits again. It takes a monster only from its seat, so what it sees from there is what it covers. With no way
 * to a monster it sees, it stays sitting and growls. It needs no owner near: it keeps a place, for whoever is there.
 *
 * <p>Off its seat it's on watch still, with its spot remembered ({@link Hunt#post}, saved with the wolf): it doesn't follow its owner (beta's follow and
 * its teleport are held off), it follows a monster no further than {@link #LEASH} blocks from its spot, and the pack's own rules ({@link Packs#tick})
 * leave it alone. Whatever fight takes a sitting wolf off its seat ends the same way: a monster it saw, something that hit it, or beta's call when its
 * owner is hurt. Only its owner's click stands it up for good.
 *
 * <p>Which monsters: beta's (`Monster`), any mod's too, that would go for a player on sight: not a spider at peace in daylight nor a calm pig zombie,
 * until either is after someone; never ghasts, as beta's wolves; creepers only with Wolves Fight Creepers on. A creeper bitten by a wolf on watch turns on
 * that wolf, as beta's monsters turn on what hurt them (`MonsterEntityMixin`): the pack's rule that it keeps coming for the player is for wolves at heel.
 */
public final class Watch {
	/** How far a sitting wolf sees a monster, and how far from its spot it follows one before it lets it go. */
	static final double SEES = 8, LEASH = 16;
	/** A sitting wolf looks round this often, in ticks. */
	static final int LOOK_EVERY = 10;
	/** A monster it found no way to is left this long before it asks the pathfinder again: a search that fails walks everywhere in reach. */
	static final int RETRY = 40;
	/** Beta's own reach for a monster's path, and the reach of the path back, which starts from as far as {@link #LEASH}. */
	static final float WAY = 16.0F, WAY_HOME = 24.0F;
	/** Beta's mobs drop a path within 1.6 blocks of its end, so the last steps are walked straight; within {@link #ARRIVED} it sits. */
	static final double LAST_STEPS = 1.7, ARRIVED = 0.25;
	/** Going back, this long without getting nearer and it sits where it is. */
	static final int STUCK = 100;
	/** A growling wolf stays roused this long after it last saw what it can't get to. */
	static final int ALARM = 30;
	/**
	 * Growls: at once when a monster it can't get to comes into view, unless a wolf near by did within {@link #GROWL_TOGETHER} ticks (one growl from a
	 * kennel, not five) or itself within {@link #GROWL_OWN}; then while it stays in view, every {@link #GROWL_EVERY} ticks and up to {@link #GROWL_SPREAD}
	 * more, if no wolf within {@link #HEARD} blocks has growled in that time. So a place growls about every 10 to 18 seconds, however many wolves sit there.
	 */
	static final int GROWL_OWN = 100, GROWL_EVERY = 200, GROWL_SPREAD = 160, GROWL_TOGETHER = 40;
	static final double HEARD = 16;

	private static final Codec<double[]> SPOT = Codec.of(spot -> {
		NbtCompound nbt = new NbtCompound();
		nbt.putDouble("x", spot[0]);
		nbt.putDouble("y", spot[1]);
		nbt.putDouble("z", spot[2]);
		return nbt;
	}, nbt -> new double[] {nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z")});

	/** The spot a wolf on watch left, while it's off it: saved with the wolf, so one away from its seat at a save still goes back. */
	public static Attachment<Entity, double[]> post;
	private static int growls;

	private Watch() {
	}

	static void init(Mod mod) {
		post = mod.attachment("post", Attachment.entity(SPOT)).persist();
	}

	public static boolean on() {
		return WolfPacks.watch != null && WolfPacks.watch.get();
	}

	/** On watch and off its seat: out after a monster or on its way back. */
	public static boolean onDuty(WolfEntity wolf) {
		return !wolf.world.isRemote && on() && wolf.isTamed() && spot(wolf) != null;
	}

	/** The spot it left, or null on its seat. */
	public static double[] spot(WolfEntity wolf) {
		Hunt hunt = Packs.hunt(wolf);
		if (!hunt.postRead) {
			hunt.postRead = true;
			hunt.post = post.get(wolf);
		}
		return hunt.post;
	}

	/** Growling at something it sees and can't get to. */
	public static boolean alarmed(WolfEntity wolf) {
		return growlsAt(wolf) != null;
	}

	/** How many growls so far, for the test. */
	public static int growls() {
		return growls;
	}

	/**
	 * At the start of a tamed wolf's thinking each tick, where the world runs, before the pack's. True when it's on watch and off its seat, which the pack
	 * leaves alone.
	 */
	public static boolean tick(WolfEntity wolf) {
		if (wolf.world.isRemote || !wolf.isTamed() || !wolf.isAlive()) return false;
		Hunt hunt = Packs.hunt(wolf);
		double[] at = spot(wolf);
		long now = Packs.now();
		if (!on()) {
			if (at != null) offDuty(wolf, hunt);
			return false;
		}
		if (wolf.isInSittingPose()) {
			// Seated again, by its own return or its owner's click: wherever it sits is its spot.
			if (at != null) offDuty(wolf, hunt);
			Entity held = wolf.getTarget();
			if ((held == null || !held.isAlive()) && (wolf.age + wolf.id) % LOOK_EVERY == 0) look(wolf, hunt, now);
			return hunt.post != null;
		}
		if (at == null) return false;
		Entity enemy = wolf.getTarget();
		if (enemy != null && !enemy.isAlive()) {
			wolf.setTarget(null);
			enemy = null;
		}
		if (enemy != null) {
			// Beta sets targets itself too: a hit wolf turns on what hit it.
			if (enemy != hunt.enemy) {
				hunt.enemy = enemy;
				hunt.closeAt = now;
			}
			double apart = wolf.getDistance(enemy);
			if (apart <= Packs.REACH) hunt.closeAt = now;
			// Past its leash it still fights what's on top of it.
			boolean strayed = apart > Packs.REACH && distance(wolf, at) > LEASH;
			if (!strayed && now - hunt.closeAt <= Packs.GIVE_UP) {
				hunt.homeBest = Double.MAX_VALUE;
				return true;
			}
			hunt.noWay().put(enemy, now + Packs.FORGET);
			wolf.setTarget(null);
			wolf.setPath(null);
		}
		hunt.enemy = null;
		home(wolf, hunt, at, now);
		return true;
	}

	/** A sitting wolf looks round: the first monster it sees that it has a way to, it goes for; with none of those, it growls at one it has no way to. */
	private static void look(WolfEntity wolf, Hunt hunt, long now) {
		List<LivingEntity> seen = null;
		for (Object o : wolf.world.collectEntitiesByClass(LivingEntity.class, wolf.boundingBox.expand(SEES, SEES, SEES))) {
			LivingEntity e = (LivingEntity) o;
			if (!(e instanceof Monster) || !e.isAlive() || !minded(e) || wolf.getDistance(e) > SEES || !sees(wolf, e)) continue;
			if (seen == null) seen = new ArrayList<>();
			seen.add(e);
		}
		if (seen == null) return;
		if (seen.size() > 1) {
			// As a pack spreads out: the one with the fewest wolves on it, then the nearest. With Packs Spread Out off, the nearest.
			Map<Entity, Integer> on = Packs.on() ? wolvesOn(wolf) : null;
			seen.sort((a, b) -> {
				int byWolves = on == null ? 0 : Integer.compare(on.getOrDefault(a, 0), on.getOrDefault(b, 0));
				return byWolves != 0 ? byWolves : Double.compare(wolf.getSquaredDistance(a), wolf.getSquaredDistance(b));
			});
		}
		LivingEntity noWayTo = null;
		boolean asked = false;
		for (LivingEntity e : seen) {
			Long until = hunt.noWay == null ? null : hunt.noWay.get(e);
			if (until != null && now < until) {
				if (noWayTo == null) noWayTo = e;
				continue;
			}
			// One search a look; the others at the next.
			if (asked) continue;
			asked = true;
			Path way = wolf.world.findPath(wolf, e, WAY);
			if (reaches(way, wolf, e)) {
				leave(wolf, hunt);
				wolf.setSitting(false);
				wolf.setTarget(e);
				wolf.setPath(way);
				hunt.enemy = e;
				hunt.closeAt = now;
				hunt.alarmedUntil = -1;
				return;
			}
			hunt.noWay().put(e, now + RETRY);
			if (noWayTo == null) noWayTo = e;
		}
		if (noWayTo != null) alarm(wolf, hunt, noWayTo, now);
	}

	/**
	 * Eye to eye, as beta's mobs see, but for what stands in the way: only blocks with a body (beta's own ray with its second switch on). Beta's sight
	 * stops at the outline of anything, tall grass and flowers too, which its monsters' eyes are high enough to look over and a wolf's, 0.64 up, aren't:
	 * a wolf sat in a meadow would see nothing.
	 */
	static boolean sees(WolfEntity wolf, Entity e) {
		return wolf.world.raycast(Vec3d.createCached(wolf.x, wolf.y + wolf.getEyeHeight(), wolf.z), Vec3d.createCached(e.x, e.y + e.getEyeHeight(), e.z), false, true) == null;
	}

	/** A monster that would go for a player on sight, and that wolves fight at all. */
	static boolean minded(LivingEntity e) {
		if (e instanceof GhastEntity) return false;
		if (e instanceof CreeperEntity) return Packs.fightCreepers();
		if (e instanceof PigZombieEntity) return ((MobEntity) e).getTarget() != null;
		// Beta's spider goes for a player only in the dark, and keeps one it's already after.
		if (e instanceof SpiderEntity) return ((MobEntity) e).getTarget() != null || e.getBrightnessAtEyes(1.0F) < 0.5F;
		return true;
	}

	/**
	 * Whether beta's path ends where the wolf could bite it. The pathfinder answers with the nearest it can get when it can't get there, so a path alone
	 * says nothing; with no path at all the wolf is where it will be.
	 */
	static boolean reaches(Path way, WolfEntity wolf, Entity e) {
		PathNode end = way == null ? null : way.getEnd();
		double x = end == null ? wolf.x : end.x + 0.5, y = end == null ? wolf.boundingBox.minY : end.y, z = end == null ? wolf.z : end.z + 0.5;
		double dx = x - e.x, dz = z - e.z;
		return dx * dx + dz * dz <= 1.5 * 1.5 && e.boundingBox.minY < y + wolf.height + 0.5 && e.boundingBox.maxY > y;
	}

	private static Map<Entity, Integer> wolvesOn(WolfEntity wolf) {
		Map<Entity, Integer> on = new IdentityHashMap<>();
		for (Object o : wolf.world.collectEntitiesByClass(WolfEntity.class, wolf.boundingBox.expand(SEES + LEASH, SEES + LEASH, SEES + LEASH))) {
			WolfEntity other = (WolfEntity) o;
			Entity t = other.getTarget();
			if (other != wolf && other.isTamed() && other.isAlive() && t != null) on.merge(t, 1, Integer::sum);
		}
		return on;
	}

	/** Beta is about to stand a sitting wolf up for a fight: its owner was hurt (the call to arms). It goes back to its spot after. */
	public static void stoodUp(WolfEntity wolf) {
		if (wolf.world.isRemote || !on() || !wolf.isTamed() || !wolf.isInSittingPose()) return;
		leave(wolf, Packs.hunt(wolf));
	}

	/**
	 * Something is hitting a wolf, which stands a sitting one up in beta and turns it on what hit it. Hit by a mob or another player, it goes back to its
	 * spot after; stood up by its owner's hit, or by fire, a fall or a cactus, it's beta's wolf, which comes to its owner.
	 */
	public static void hit(WolfEntity wolf, Entity by) {
		if (!(by instanceof LivingEntity) || by == wolf) return;
		if (by instanceof PlayerEntity && ((PlayerEntity) by).name.equalsIgnoreCase(wolf.getOwnerName())) return;
		stoodUp(wolf);
	}

	/** Leaving its seat, on watch still: its spot is where it sits, or the spot it last came back to if it hasn't been moved since. */
	private static void leave(WolfEntity wolf, Hunt hunt) {
		boolean unmoved = hunt.seat != null && Math.abs(wolf.x - hunt.restX) + Math.abs(wolf.z - hunt.restZ) < 0.05 && Math.abs(wolf.boundingBox.minY - hunt.seat[1]) < 0.5;
		hunt.post = unmoved ? hunt.seat : new double[] {wolf.x, wolf.boundingBox.minY, wolf.z};
		hunt.postRead = true;
		post.set(wolf, hunt.post);
		hunt.homeBest = Double.MAX_VALUE;
		hunt.homePath = null;
	}

	private static void offDuty(WolfEntity wolf, Hunt hunt) {
		hunt.post = null;
		hunt.homePath = null;
		post.set(wolf, null);
	}

	/** On its way back: beta's path to its spot's cell, then the last steps straight (its mixin steers them, {@link #lastSteps}). */
	private static void home(WolfEntity wolf, Hunt hunt, double[] at, long now) {
		double flat = flat(wolf, at), up = Math.abs(wolf.boundingBox.minY - at[1]);
		if (flat <= ARRIVED && up < 1) {
			sit(wolf, hunt, at);
			return;
		}
		if (flat < hunt.homeBest - 0.25) {
			hunt.homeBest = flat;
			hunt.homeBestAt = now;
		}
		if (now - hunt.homeBestAt > STUCK) {
			// No way back: it keeps watch from here.
			sit(wolf, hunt, null);
			return;
		}
		if (flat <= LAST_STEPS && up < 1.5) {
			if (wolf.hasPath()) wolf.setPath(null);
			return;
		}
		// Beta swaps any mob's path for a stroll now and then, and drops it one tick in a hundred: this one is put back.
		Path has = ((MobEntityAccessor) wolf).wolfpacks$path();
		if ((has == null || has != hunt.homePath) && now >= hunt.pathAt) {
			hunt.homePath = wolf.world.findPath(wolf, MathHelper.floor(at[0]), MathHelper.floor(at[1]), MathHelper.floor(at[2]), WAY_HOME);
			wolf.setPath(hunt.homePath);
			hunt.pathAt = now + 10;
		}
	}

	private static void sit(WolfEntity wolf, Hunt hunt, double[] at) {
		wolf.setSitting(true);
		wolf.setPath(null);
		wolf.velocityX = 0;
		wolf.velocityZ = 0;
		hunt.seat = at;
		hunt.restX = wolf.x;
		hunt.restZ = wolf.z;
		hunt.settling = true;
		offDuty(wolf, hunt);
	}

	/** For its mixin, after beta's thinking each tick: the spot to walk straight at, on the last steps back, or null. */
	public static double[] lastSteps(WolfEntity wolf) {
		if (wolf.isInSittingPose() || wolf.getTarget() != null || !onDuty(wolf)) return null;
		double[] at = spot(wolf);
		return flat(wolf, at) <= LAST_STEPS && Math.abs(wolf.boundingBox.minY - at[1]) < 1.5 ? at : null;
	}

	/** For its mixin: true once after it sat down by itself. */
	public static boolean settled(WolfEntity wolf) {
		Hunt hunt = Packs.hunt(wolf);
		boolean settling = hunt.settling;
		hunt.settling = false;
		return settling;
	}

	/** For its mixin: what a sitting wolf is growling at, which it faces, or null. */
	public static Entity growlsAt(WolfEntity wolf) {
		if (wolf.world.isRemote || !wolf.isInSittingPose() || !on()) return null;
		Hunt hunt = Packs.hunt(wolf);
		return hunt.alarm != null && Packs.now() <= hunt.alarmedUntil && hunt.alarm.isAlive() ? hunt.alarm : null;
	}

	private static void alarm(WolfEntity wolf, Hunt hunt, LivingEntity at, long now) {
		boolean fresh = now > hunt.alarmedUntil;
		hunt.alarm = at;
		hunt.alarmedUntil = now + ALARM;
		if (fresh) hunt.growlAt = Math.max(now, hunt.growled + GROWL_OWN);
		if (now < hunt.growlAt) return;
		if (!heard(wolf, now - (fresh ? GROWL_TOGETHER : GROWL_EVERY))) {
			// Beta's own growl, an angry wolf's, at a wolf's own loudness.
			Sounds.play(wolf.world, wolf.x, wolf.y, wolf.z, "mob.wolf.growl", 0.4F, (wolf.world.random.nextFloat() - wolf.world.random.nextFloat()) * 0.2F + 1.0F);
			hunt.growled = now;
			growls++;
		}
		hunt.growlAt = now + GROWL_EVERY + wolf.world.random.nextInt(GROWL_SPREAD);
	}

	/** Whether another tamed wolf near by has growled since then. */
	private static boolean heard(WolfEntity wolf, long since) {
		for (Object o : wolf.world.collectEntitiesByClass(WolfEntity.class, wolf.boundingBox.expand(HEARD, HEARD, HEARD))) {
			WolfEntity other = (WolfEntity) o;
			if (other != wolf && other.isTamed() && Packs.hunt(other).growled >= since) return true;
		}
		return false;
	}

	private static double flat(WolfEntity wolf, double[] at) {
		double dx = wolf.x - at[0], dz = wolf.z - at[2];
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static double distance(WolfEntity wolf, double[] at) {
		double dx = wolf.x - at[0], dy = wolf.boundingBox.minY - at[1], dz = wolf.z - at[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
