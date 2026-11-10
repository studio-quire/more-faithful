package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.impl.FlammableBlocks;

/**
 * Fire, where the world runs, never on a client on a server. Everything that lights one is heard, except worldgen.
 * <p>
 * An explosion's fires only exist for a moment in b1.7.3: it sets them in cells of its own list, then breaks every cell of that
 * list, fires included. A veto of one changes nothing you can see, unless a mod changes that order.
 * <p>
 * Beta ends a fire when rain reaches it, when nothing beside it is flammable, or at full age if what's under it won't burn. So a fire sitting on something flammable whose {@link #onBurn} is always
 * vetoed <b>burns forever</b>, rain apart: a mod that spares a block should usually put the fire out too, and {@link Burn} says where it is.
 */
public final class Fire {
	public enum Cause {
		/** From a fire nearby: into a burning block's cell, or into air beside something flammable. */
		SPREAD,
		/** A strike. Where it lands, at difficulty 2 and up; on each flash after that at any difficulty, since {@code LightningEntity.tick} has no such gate. */
		LIGHTNING,
		LAVA,
		EXPLOSION,
		/** Flint and steel. */
		PLAYER
	}

	public static final class Ignite extends Cancellable {
		public final World world;
		public final int x, y, z;
		public final Cause cause;
		/** Who's lighting it, for {@link Cause#PLAYER}; otherwise null. */
		public final PlayerEntity player;

		Ignite(World world, int x, int y, int z, Cause cause, PlayerEntity player) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.cause = cause;
			this.player = player;
		}
	}

	public static final class Burn extends Cancellable {
		public final World world;
		public final int x, y, z;
		/** What's about to burn away. */
		public final int block;
		/** The fire reaching for it, in one of the six cells beside it. */
		public final int fireX, fireY, fireZ;

		Burn(World world, int x, int y, int z, int fireX, int fireY, int fireZ) {
			this.fireX = fireX;
			this.fireY = fireY;
			this.fireZ = fireZ;
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
			this.block = world.getBlockId(x, y, z);
		}
	}

	private static final Listeners<Consumer<Ignite>> IGNITE = new Listeners<>("Fire.onIgnite");
	private static final Listeners<Consumer<Burn>> BURN = new Listeners<>("Fire.onBurn");
	private static int fireX, fireY, fireZ;

	private Fire() {
	}

	/**
	 * Beside this block, fire can start and spread: {@code burn} is how easily it catches (vanilla's 5 to 60) and {@code spread}
	 * how fast it's eaten (5 to 100). Static data: the same on both sides, set in {@code init}. Zero for both makes one of
	 * vanilla's blocks safe.
	 */
	public static void flammable(Block block, int burn, int spread) {
		FlammableBlocks.set(block.id, burn, spread);
	}

	/** Veto: a fire is about to be lit in this cell. Cancelled, the cell stays as it is. */
	public static void onIgnite(Consumer<Ignite> listener) {
		IGNITE.add(listener);
	}

	/**
	 * Veto: fire is about to consume this block, turning it to fire or air, and setting off TNT. Cancelled, the block stays and
	 * the fire goes on, which on something it can't eat means for good (above). If it would turn to fire, {@link #onIgnite} is heard next, and a veto there leaves the block too.
	 */
	public static void onBurn(Consumer<Burn> listener) {
		BURN.add(listener);
	}

	/** The core's: whether the fire is lit. */
	public static boolean ignites(World world, int x, int y, int z, Cause cause, PlayerEntity player) {
		if (IGNITE.isEmpty() || world.isRemote) return true;
		Ignite event = new Ignite(world, x, y, z, cause, player);
		IGNITE.each(l -> l.accept(event));
		return !event.cancelled();
	}

	/** The core's: which fire is ticking. */
	public static void burningAt(int x, int y, int z) {
		fireX = x;
		fireY = y;
		fireZ = z;
	}

	/** The core's: whether the block burns. */
	public static boolean burns(World world, int x, int y, int z) {
		if (BURN.isEmpty() || world.isRemote) return true;
		Burn event = new Burn(world, x, y, z, fireX, fireY, fireZ);
		BURN.each(l -> l.accept(event));
		return !event.cancelled();
	}
}
