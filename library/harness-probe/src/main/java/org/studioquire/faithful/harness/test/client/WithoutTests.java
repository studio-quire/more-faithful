package org.studioquire.faithful.harness.test.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/**
 * Mods' tests on a server that has none of them, the core's alone or the real one: entrypoint {@code faithful:test-without}, a {@link ClientTest}. Nothing answers there, so no scenes, and no plot:
 * it's wherever the server spawned the player, which can be in water, so a test that needs ground makes its own ({@link ClientTest#ownGround}).
 */
public final class WithoutTests {
	public static final String ENTRYPOINT = "faithful:test-without";
	static final boolean ASKED = "without".equals(TestHost.MODE);
	private static final int LIMIT = 3000;

	private final List<EntrypointContainer<WorldCheck>> tests;
	private final SelfTest results;
	private int next, ticks;
	private WorldCheck running;

	private WithoutTests(SelfTest results) {
		this.results = results;
		tests = new ArrayList<>(FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, WorldCheck.class));
	}

	/** Null unless this run was asked for them. */
	public static WithoutTests start(SelfTest results) {
		return ASKED ? new WithoutTests(results) : null;
	}

	/** True when every mod's are over. */
	public boolean tick(Minecraft mc) {
		if (running == null) {
			if (next >= tests.size()) {
				results.owner(null);
				return true;
			}
			EntrypointContainer<WorldCheck> test = tests.get(next++);
			results.owner(TestHost.owner(test.getProvider().getMetadata().getId()));
			running = test.getEntrypoint();
			running.start(mc, results);
			ticks = 0;
			return false;
		}
		if (++ticks > LIMIT) results.fail("its test on a server without it was still running after " + LIMIT + " ticks", new AssertionError());
		if (ticks > LIMIT || running.tick(mc)) running = null;
		return false;
	}
}
