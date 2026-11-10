package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.entity.player.PlayerEntity;

/** A player using a bed, where the world runs, after beta's own checks of the Nether and an occupied bed. */
public final class Beds {
	public static final class Sleep {
		public final PlayerEntity player;
		public final int x, y, z;
		boolean spawnOnly;

		public Sleep(PlayerEntity player, int x, int y, int z) {
			this.player = player;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		/** Veto, sticky: no sleep and no night skipped; the bed becomes the player's respawn point, whatever the hour. */
		public void setSpawnOnly() {
			spawnOnly = true;
		}

		public boolean spawnOnly() {
			return spawnOnly;
		}
	}

	private static final Listeners<Consumer<Sleep>> SLEEP = new Listeners<>("Beds.onSleep");

	private Beds() {
	}

	public static void onSleep(Consumer<Sleep> listener) {
		SLEEP.add(listener);
	}

	/** The core's. */
	public static Sleep fire(PlayerEntity player, int x, int y, int z) {
		Sleep sleep = new Sleep(player, x, y, z);
		SLEEP.each(l -> l.accept(sleep));
		return sleep;
	}
}
