package org.studioquire.faithful.wolfpacks;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.studioquire.faithful.wolfpacks.mixin.CreeperEntityAccessor;
import org.studioquire.faithful.wolfpacks.mixin.PlayerEntityAccessor;

/**
 * A pack spreading out over a fight, where the world runs.
 *
 * <p>Beta has one call to arms ({@code PlayerEntity.commandWolvesToAttack}): when a player hits something, or something hurts them, every tamed wolf of
 * theirs within 16 blocks that has no target takes that one thing (sitting ones only when the player was hurt, and they get up), unless it's a creeper, a
 * ghast, one of their own wolves, or a player with PvP off. The wolf keeps it until it's dead and then stands idle. That call is kept, whoever it rouses, and
 * each wolf it sends takes its own enemy: of the monsters after the player or their wolves and what the call was about, the one with the fewest wolves on it,
 * what the call was about first among equals, then the nearest. While the fight lasts (a wolf of theirs holds an enemy, or did within {@link #LINGER}), a
 * free wolf takes the next the same way, so the pack works through a group. A wolf that can't get to its enemy lets it go.
 *
 * <p>Guarding (its own rule, the user's ask after their first play, 2026-11-10) starts a fight beta's call wouldn't: a mob within {@link #GUARD} blocks
 * of the player that has noticed them (after them or one of their wolves: beta's own target, so a spider at peace by day, a calm pig zombie or a zombie
 * that hasn't seen them doesn't count), or, at any range the packs reach, a mob that has shot at them and is still after them (a skeleton's arrow, hit or
 * miss: beta's call already answers one that hits). Anything else coming is left until it's that close, or until the call. Free, standing wolves within the call's reach respond; a sitting one stays seated.
 *
 * <p>Wolves Fight Creepers (its own rule, the user's call the same day) lifts beta's exception for creepers everywhere: the call, guarding and the spread.
 *
 * <p>What's left as beta: a wolf that's hit turns on what hit it.
 *
 * <p>A sitting wolf's watch is {@link Watch}'s. A wolf on watch and off its seat is left out of the pack's own looking ({@link #tick}); beta's call can
 * still send it while it's free, and the others count it as a wolf on its monster when they spread.
 */
public final class Packs {
	/** Beta's reach for its call: wolves within 16 blocks of the player across, 4 up or down. Free wolves join a fight from as far. */
	static final double CALL = 16, CALL_UP = 4;
	/** How far round the player a monster after them or their wolves is an enemy. */
	static final double ENEMY = 16, ENEMY_UP = 8;
	/** What a call was about stays an enemy while it's within this of the player, across and up or down. */
	static final double CALLED = 24;
	/** A fight goes on this long, in ticks, after the last wolf in it held an enemy, so a monster coming just after is still taken. */
	static final int LINGER = 60;
	/** A free wolf in a fight looks for an enemy this often, in ticks, and at once when its own has died. */
	static final int LOOK_EVERY = 5;
	/** A wolf that hasn't been within {@link #REACH} blocks of its enemy for {@link #GIVE_UP} ticks lets it go, and leaves it alone for {@link #FORGET}. */
	static final double REACH = 3;
	static final int GIVE_UP = 160, FORGET = 600;
	/** Guarding: a monster this near the player, feet to feet, is gone for unasked. */
	static final double GUARD = 4;

	/** Ticks where the world runs: the world's own clock jumps at a night's sleep and {@code /time}. */
	private static long clock;
	/** Each player's fight, by player: a new life is a new object, and a new pack. */
	private static final Map<PlayerEntity, Pack> PACKS = new WeakHashMap<>();
	/** What each shooter last shot at: it stays a shooter at that while it's still after it. */
	private static final Map<Entity, Entity> SHOT_AT = new WeakHashMap<>();

	static final class Pack {
		long fightUntil = -1;
		/** What beta's call was about, and what guarding found, while each lasts. */
		final List<LivingEntity> called = new ArrayList<>();
		/** The tick guarding last looked round this player, and the nearest it found: what every wolf goes for with Packs Spread Out off. */
		long guardedAt = -1;
		LivingEntity guarded;
	}

	private Packs() {
	}

	static void tickClock() {
		clock++;
	}

	static long now() {
		return clock;
	}

	public static boolean on() {
		return WolfPacks.spreadOut != null && WolfPacks.spreadOut.get();
	}

	public static boolean guarding() {
		return WolfPacks.guarding != null && WolfPacks.guarding.get();
	}

	public static boolean fightCreepers() {
		return WolfPacks.fightCreepers != null && WolfPacks.fightCreepers.get();
	}

	/** Whether this player's pack is in a fight now. */
	public static boolean fighting(PlayerEntity player) {
		Pack pack = PACKS.get(player);
		return pack != null && clock <= pack.fightUntil;
	}

	private static Pack pack(PlayerEntity player) {
		Pack pack = PACKS.get(player);
		if (pack == null) PACKS.put(player, pack = new Pack());
		return pack;
	}

	static Hunt hunt(WolfEntity wolf) {
		return ((Hunt.Holder) wolf).wolfpacks$hunt();
	}

	/** In beta's call to arms, for each wolf it sends: what this one takes in place of {@code called}, which beta sends them all at. */
	public static Entity call(PlayerEntity owner, WolfEntity wolf, Entity called) {
		Pack pack = pack(owner);
		if (called instanceof LivingEntity && !pack.called.contains(called)) pack.called.add((LivingEntity) called);
		pack.fightUntil = clock + LINGER;
		Entity choice = choose(owner, wolf, called);
		return choice == null ? called : choice;
	}

	/**
	 * Beta's call about a creeper, which beta drops at its first line, with Wolves Fight Creepers on: the same wolves sent the same way. Not about one going
	 * off: beta calls the wolves as its blast hurts the player, just before it's marked dead, and sitting wolves would get up for nothing.
	 */
	public static void callAboutCreeper(PlayerEntity owner, CreeperEntity creeper, boolean keepSitting) {
		if (((CreeperEntityAccessor) creeper).wolfpacks$fuseTime() >= 30) return;
		// Beta's own box for its call.
		Box reach = Box.createCached(owner.x, owner.y, owner.z, owner.x + 1, owner.y + 1, owner.z + 1).expand(CALL, CALL_UP, CALL);
		for (Object o : owner.world.collectEntitiesByClass(WolfEntity.class, reach)) {
			WolfEntity wolf = (WolfEntity) o;
			if (!ownWolf(owner, wolf) || wolf.getTarget() != null || keepSitting && wolf.isInSittingPose()) continue;
			Watch.stoodUp(wolf);
			wolf.setSitting(false);
			Entity enemy = on() ? call(owner, wolf, creeper) : creeper;
			wolf.setTarget(enemy);
			took(wolf, enemy);
		}
	}

	/** After a wolf is set on an enemy: it's taken from now, and it paths there at once rather than finishing its walk to its owner first. */
	public static void took(WolfEntity wolf, Entity enemy) {
		Hunt hunt = hunt(wolf);
		hunt.enemy = enemy;
		hunt.closeAt = clock;
		wolf.setPath(null);
	}

	/** At the start of a tamed wolf's thinking each tick, where the world runs, before beta's clears a dead target. */
	public static void tick(WolfEntity wolf) {
		boolean spread = on(), guard = guarding();
		if (!wolf.isTamed() || wolf.world.isRemote || !spread && !guard) return;
		PlayerEntity owner = wolf.world.getPlayer(wolf.getOwnerName());
		if (owner == null || !owner.isAlive()) return;
		Hunt hunt = hunt(wolf);
		Entity enemy = wolf.getTarget();
		boolean freed = false;
		if (enemy != null && !spread) {
			// Guarding alone: beta's wolf, busy until its target dies.
			if (enemy.isAlive()) return;
			wolf.setTarget(null);
			freed = true;
		} else if (enemy != null) {
			if (!enemy.isAlive()) {
				wolf.setTarget(null);
				freed = true;
			} else {
				// Beta sets targets itself too: a hit wolf turns on what hit it.
				if (enemy != hunt.enemy) {
					hunt.enemy = enemy;
					hunt.closeAt = clock;
				}
				if (wolf.getDistance(enemy) <= REACH) hunt.closeAt = clock;
				if (clock - hunt.closeAt <= GIVE_UP) {
					pack(owner).fightUntil = clock + LINGER;
					return;
				}
				hunt.forgotten = enemy;
				hunt.forgetUntil = clock + FORGET;
				wolf.setTarget(null);
				wolf.setPath(null);
				freed = true;
			}
		}
		hunt.enemy = null;
		if (wolf.isInSittingPose() || !freed && wolf.age % LOOK_EVERY != 0 || !near(owner, wolf, CALL, CALL_UP)) return;
		if (guard) guard(owner);
		if (!fighting(owner)) return;
		Entity next;
		if (spread) {
			next = choose(owner, wolf, null);
		} else {
			LivingEntity guarded = pack(owner).guarded;
			next = guarded != null && guarded.isAlive() ? guarded : null;
		}
		if (next != null) {
			wolf.setTarget(next);
			took(wolf, next);
		}
	}

	/**
	 * Guarding, at most once a tick for each player: each mob after them or their wolves within {@link #GUARD} of them, and each that has shot at them,
	 * joins the fight as if beta's call had been about it, and starts one if there's none.
	 */
	static void guard(PlayerEntity owner) {
		Pack pack = pack(owner);
		if (pack.guardedAt == clock) return;
		pack.guardedAt = clock;
		pack.guarded = null;
		double best = 0;
		for (Object o : owner.world.collectEntitiesByClass(LivingEntity.class, owner.boundingBox.expand(ENEMY, ENEMY_UP, ENEMY))) {
			LivingEntity e = (LivingEntity) o;
			if (!e.isAlive() || excluded(owner, e) || !(after(owner, e) && feetApart(owner, e) <= GUARD || shotAt(owner, e))) continue;
			if (!pack.called.contains(e)) pack.called.add(e);
			pack.fightUntil = clock + LINGER;
			double d = owner.getSquaredDistance(e);
			if (pack.guarded == null || d < best) {
				pack.guarded = e;
				best = d;
			}
		}
	}

	/**
	 * Where the world runs: {@code shooter} has just loosed a shot at {@code target} (beta's skeletons, `SkeletonEntityMixin`; any mod's shooting monster
	 * may call this too). Guarding then sends wolves at it from any range while it's still after them.
	 */
	public static void shotAt(Entity shooter, Entity target) {
		SHOT_AT.put(shooter, target);
	}

	/** It has shot at the player, or one of their wolves, and is still after them. */
	static boolean shotAt(PlayerEntity owner, LivingEntity e) {
		Entity shot = SHOT_AT.get(e);
		return shot != null && e instanceof MobEntity && ((MobEntity) e).getTarget() == shot && after(owner, e);
	}

	/** The enemy this wolf should take, or null with none. */
	static Entity choose(PlayerEntity owner, WolfEntity wolf, Entity called) {
		List<LivingEntity> enemies = enemies(owner, called);
		if (enemies.isEmpty()) return null;
		Map<Entity, Integer> on = new IdentityHashMap<>();
		double far = CALLED + CALL;
		for (Object o : owner.world.collectEntitiesByClass(WolfEntity.class, owner.boundingBox.expand(far, CALLED, far))) {
			WolfEntity other = (WolfEntity) o;
			Entity t = other.getTarget();
			if (other != wolf && other.isTamed() && other.isAlive() && t != null) on.merge(t, 1, Integer::sum);
		}
		Hunt hunt = hunt(wolf);
		LivingEntity best = null;
		int bestOn = 0;
		boolean bestCalled = false;
		double bestDistance = 0;
		for (LivingEntity e : enemies) {
			if (hunt.forgets(e, clock)) continue;
			int n = on.getOrDefault(e, 0);
			boolean isCalled = e == called;
			double d = wolf.getSquaredDistance(e);
			if (best == null || n < bestOn || n == bestOn && (isCalled && !bestCalled || isCalled == bestCalled && d < bestDistance)) {
				best = e;
				bestOn = n;
				bestCalled = isCalled;
				bestDistance = d;
			}
		}
		return best;
	}

	/** What beta's calls were about and is still near, and every monster near the player that's after them or one of their wolves. */
	static List<LivingEntity> enemies(PlayerEntity owner, Entity called) {
		World world = owner.world;
		List<LivingEntity> enemies = new ArrayList<>();
		Pack pack = PACKS.get(owner);
		if (pack != null) {
			for (Iterator<LivingEntity> i = pack.called.iterator(); i.hasNext();) {
				LivingEntity e = i.next();
				// Beta sends wolves at what it's called about however far it is; what's left of earlier calls has to be near.
				if (e.isAlive() && e.world == world && (e == called || near(owner, e, CALLED, CALLED)) && !excluded(owner, e)) enemies.add(e);
				else if (e != called) i.remove();
			}
		}
		for (Object o : world.collectEntitiesByClass(LivingEntity.class, owner.boundingBox.expand(ENEMY, ENEMY_UP, ENEMY))) {
			LivingEntity e = (LivingEntity) o;
			if (e.isAlive() && after(owner, e) && !excluded(owner, e) && !enemies.contains(e)) enemies.add(e);
		}
		return enemies;
	}

	/** Beta's own exceptions to its call, creepers only with Wolves Fight Creepers off. */
	static boolean excluded(PlayerEntity owner, Entity e) {
		if (e == owner || e instanceof GhastEntity || e instanceof CreeperEntity && !fightCreepers()) return true;
		if (e instanceof WolfEntity && ownWolf(owner, (WolfEntity) e)) return true;
		return e instanceof PlayerEntity && !((PlayerEntityAccessor) owner).wolfpacks$pvp();
	}

	/** A monster going for the player or one of their wolves. Beta's slimes have no target: they hop at the nearest player within 16. */
	static boolean after(PlayerEntity owner, LivingEntity e) {
		if (e instanceof MobEntity) {
			Entity t = ((MobEntity) e).getTarget();
			return t == owner || t instanceof WolfEntity && ownWolf(owner, (WolfEntity) t);
		}
		return e instanceof SlimeEntity && e.world.getClosestPlayer(e, 16.0) == owner;
	}

	static boolean ownWolf(PlayerEntity owner, WolfEntity wolf) {
		return wolf.isTamed() && owner.name.equals(wolf.getOwnerName());
	}

	/** Feet to feet: in single player beta keeps a player's position at the eyes, on a server at the feet. */
	public static double feetApart(Entity a, Entity b) {
		double dx = a.x - b.x, dy = a.boundingBox.minY - b.boundingBox.minY, dz = a.z - b.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static boolean near(Entity a, Entity b, double across, double up) {
		return Math.abs(a.x - b.x) <= across && Math.abs(a.z - b.z) <= across && Math.abs(a.y - b.y) <= up;
	}
}
