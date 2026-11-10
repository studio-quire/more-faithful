package org.studioquire.faithful.harness;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * A mod's own short test in the harness: an entrypoint {@code harness:scenario}, from a test-only jar (a {@code scenario} source set with {@code scenarioRemapJar}, or a test mod's
 * main). It runs where the world runs, in single player and on a server, once, so it does what it does at once: place, use, read back.
 */
public interface Scenario {
	interface Check {
		void that(String what, boolean ok);
	}

	void run(World world, PlayerEntity player, Check check);

	/** Every mod's, each kept from the others. */
	static void runAll(World world, PlayerEntity player) {
		for (EntrypointContainer<Scenario> c : FabricLoader.getInstance().getEntrypointContainers("harness:scenario", Scenario.class)) {
			String mod = c.getProvider().getMetadata().getId();
			String side = world.isRemote ? "a client" : FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER ? "a server" : "single player";
			try {
				c.getEntrypoint().run(world, player, (what, ok) -> {
					if (!ok) Out.fail(mod, "scenario", what + ", on " + side);
				});
				Out.info("scenario " + mod + " ran on " + side);
			} catch (RuntimeException | LinkageError e) {
				Out.fail(mod, "scenario", "threw on " + side + ": " + e);
				e.printStackTrace(System.out);
			}
		}
	}
}
