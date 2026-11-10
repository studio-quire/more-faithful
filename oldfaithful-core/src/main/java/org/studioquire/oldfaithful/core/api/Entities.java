package org.studioquire.oldfaithful.core.api;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;

/** Entities, where the world runs. Never on a client on a server: what a client sees of them is the server's. */
public final class Entities {
	public static final class Spawn extends Cancellable {
		public final World world;
		public final Entity entity;

		Spawn(World world, Entity entity) {
			this.world = world;
			this.entity = entity;
		}
	}

	public static final class Damage extends Cancellable {
		public final LivingEntity entity;
		/** What hit it, or null: fire, falling, drowning and the like. An arrow, not its shooter. */
		public final Entity source;
		private int amount;

		Damage(LivingEntity entity, Entity source, int amount) {
			this.entity = entity;
			this.source = source;
			this.amount = amount;
		}

		public int amount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}
	}

	public static final class Death {
		public final LivingEntity entity;
		/** What killed it, or null. */
		public final Entity adversary;

		Death(LivingEntity entity, Entity adversary) {
			this.entity = entity;
			this.adversary = adversary;
		}
	}

	public static final class MonsterSpawning extends Cancellable {
		public final World world;

		MonsterSpawning(World world) {
			this.world = world;
		}
	}

	private static final Listeners<Consumer<Spawn>> SPAWN = new Listeners<>("Entities.onSpawn");
	private static final Listeners<Consumer<MonsterSpawning>> MONSTERS = new Listeners<>("Entities.onMonsterSpawning");
	private static final Listeners<Consumer<Damage>> DAMAGE = new Listeners<>("Entities.onDamage");
	private static final Listeners<Consumer<Death>> DIED = new Listeners<>("Entities.onDied");

	private Entities() {
	}

	/**
	 * Veto: a new entity is joining the world, whatever it is: mobs, items, arrows, minecarts. Not one read from a chunk, and not a
	 * player. Cancelled, it never joins.
	 */
	public static void onSpawn(Consumer<Spawn> listener) {
		SPAWN.add(listener);
	}

	/**
	 * Veto, asked each tick of each world that would let them: monsters may appear on their own. Cancelled, none do this tick: natural spawning, in the Nether too, and the one that wakes a sleeper.
	 * Animals aren't touched, nor is the difficulty, a spawner, or a monster already there.
	 */
	public static void onMonsterSpawning(Consumer<MonsterSpawning> listener) {
		MONSTERS.add(listener);
	}

	/**
	 * Filter and veto: something living is being hurt. {@code amount} chains in order, each listener seeing the last's; a cancel
	 * is sticky and every listener still hears. Asked before vanilla's checks, so a hit in the half second of protection after
	 * another is heard and then may do less than it says, or nothing. A player's amount has the difficulty in it already.
	 */
	public static void onDamage(Consumer<Damage> listener) {
		DAMAGE.add(listener);
	}

	/** Notice: something living has died, players too, before it drops anything. */
	public static void onDied(Consumer<Death> listener) {
		DIED.add(listener);
	}

	/** One more kind in a biome's natural spawns, at a weight beside vanilla's 8 to 12. Those lists are read where the world runs. */
	@SuppressWarnings("unchecked")
	public static void addNaturalSpawn(Biome biome, SpawnGroup group, Class<? extends LivingEntity> entity, int weight) {
		((List<EntitySpawnGroup>) biome.getSpawnableEntities(group)).add(new EntitySpawnGroup(entity, weight));
	}

	/** Takes a kind out of a biome's natural spawns. */
	@SuppressWarnings("unchecked")
	public static void removeNaturalSpawn(Biome biome, SpawnGroup group, Class<? extends LivingEntity> entity) {
		((List<EntitySpawnGroup>) biome.getSpawnableEntities(group)).removeIf(e -> e.clazz == entity);
	}

	/** The core's: whether the entity joins the world. */
	public static boolean spawning(World world, Entity entity) {
		if (SPAWN.isEmpty() || world.isRemote || entity instanceof PlayerEntity) return true;
		Spawn event = new Spawn(world, entity);
		SPAWN.each(l -> l.accept(event));
		return !event.cancelled();
	}

	/** The core's. */
	public static boolean monsterSpawning(World world) {
		if (MONSTERS.isEmpty()) return true;
		MonsterSpawning event = new MonsterSpawning(world);
		MONSTERS.each(l -> l.accept(event));
		return !event.cancelled();
	}

	public static boolean hasDamageListeners() {
		return !DAMAGE.isEmpty();
	}

	/** The core's. */
	public static Damage damaged(LivingEntity entity, Entity source, int amount) {
		Damage event = new Damage(entity, source, amount);
		DAMAGE.each(l -> l.accept(event));
		return event;
	}

	/** The core's. */
	public static void died(LivingEntity entity, Entity adversary) {
		if (DIED.isEmpty() || entity.world.isRemote) return;
		Death event = new Death(entity, adversary);
		DIED.each(l -> l.accept(event));
	}
}
