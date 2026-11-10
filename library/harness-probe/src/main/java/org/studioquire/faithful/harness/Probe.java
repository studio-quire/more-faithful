package org.studioquire.faithful.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Listeners;
import org.studioquire.oldfaithful.core.api.Mod;

/** Goes into every harness run. With {@code -Dfaithful.harness.out=<dir>}, each start leaves its registry and tables there. */
public final class Probe implements FaithfulMod {
	public static final String OUT = System.getProperty("faithful.harness.out");

	public static Channel state;

	@Override
	public void init(Mod mod) {
		state = mod.channel("state", 1);
		TestHost.register(mod.channel("test", 1).world());
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			ServerScenes.register();
			MonkeyServer.register(state);
			Lifecycle.onServerStart(Probe::report);
		}
	}

	/** Once every mod has started. */
	public static void report() {
		if (OUT == null) return;
		try {
			Path dir = Files.createDirectories(Paths.get(OUT));
			RegistryRules.run(dir);
			Tables.write(dir.resolve("tables.txt"));
			Files.write(dir.resolve("listeners.txt"), Listeners.describeAll());
			MixinEnvironment.getCurrentEnvironment().audit();
		} catch (IOException | IllegalAccessException | RuntimeException e) {
			Out.fail("harness-probe", "probe", "the report failed: " + e);
			e.printStackTrace(System.out);
		}
	}
}
