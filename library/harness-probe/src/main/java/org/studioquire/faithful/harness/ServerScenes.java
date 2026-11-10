package org.studioquire.faithful.harness;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.ServerWorld;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Server;

/** The task's handle on a running server: {@code hprobe <scene> [args]} on its console, each ending in a DONE line. */
final class ServerScenes {
	private ServerScenes() {
	}

	private static ServerPlayerEntity player(String name) {
		ServerPlayerEntity p = (ServerPlayerEntity) Server.player(name);
		if (p == null) throw new IllegalArgumentException("no player " + name);
		return p;
	}

	static void register() {
		Commands.register("hprobe", "hprobe <scene>: test only", (args, reply) -> {
			ServerWorld world = (ServerWorld) Server.world(0);
			try {
				switch (args[0]) {
					case "place": new Arena(world).place(); break;
					case "give": new Arena(world).give(player(args[1])); break;
					case "touch": new Arena(world).touch(); break;
					case "snapshot": new Arena(world).snapshot(Paths.get(args[1])); break;
					case "statics": Statics.write(Paths.get(args[1])); break;
					case "outcomes": new Outcomes(world, player(args[1])).run(Paths.get(args[2])); break;
					case "scenarios": Scenario.runAll(world, player(args[1])); break;
					default: reply.say("no such scene");
				}
			} catch (IOException | RuntimeException e) {
				Out.fail("harness-probe", "scene", args[0] + ": " + e);
				e.printStackTrace(System.out);
			}
			Out.done(args[0]);
		});
	}
}
