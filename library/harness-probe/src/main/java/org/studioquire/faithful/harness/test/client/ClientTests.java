package org.studioquire.faithful.harness.test.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import org.studioquire.faithful.harness.test.Checks;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/** Drives a run of mods' tests from the client: for each mod, its world tests where the world runs, then its client tests here. One tick at a time, under the probe's world check. */
public final class ClientTests {
	/** Long enough for any honest world test: its own limits end it first. */
	private static final int WORLD_LIMIT = 6000, PLACED_LIMIT = 200, CLIENT_LIMIT = 6000;

	private final List<String> mods;
	private final Map<String, List<WorldCheck>> checks = new TreeMap<>();
	private final SelfTest results;
	private int mod = -1, check, ticks, asked, reviving;
	private boolean inWorld, settling, ending;

	private ClientTests(SelfTest results) {
		this.results = results;
		for (EntrypointContainer<WorldCheck> c : FabricLoader.getInstance().getEntrypointContainers(TestHost.CLIENT_ENTRYPOINT, WorldCheck.class)) {
			List<WorldCheck> list = checks.computeIfAbsent(TestHost.owner(c.getProvider().getMetadata().getId()), k -> new ArrayList<>());
			if (!TestHost.restarted() || c.getEntrypoint().afterRestart()) list.add(c.getEntrypoint());
		}
		Map<String, Boolean> all = new TreeMap<>();
		TestHost.mods().forEach(m -> all.put(m, true));
		checks.keySet().forEach(m -> all.put(m, true));
		mods = new ArrayList<>(all.keySet());
	}

	/** Null unless this run was asked for them. */
	public static ClientTests start(SelfTest results) {
		return TestHost.MODE == null || WithoutTests.ASKED ? null : new ClientTests(results);
	}

	/** True when every mod's are over. */
	public boolean tick(Minecraft mc) {
		ticks++;
		if (reviving > 0) {
			reviving--;
			return false;
		}
		if (ending) {
			if (TestHost.answers < asked && ticks < PLACED_LIMIT) return false;
			results.owner(null);
			// The game's own stop skips beta's last save, and the next launch reads this world.
			if (!mc.world.isRemote) mc.world.saveWithLoadingDisplay(true, null);
			return true;
		}
		if (mod < 0 || (!inWorld && !settling && check >= checksOf().size())) return next(mc);
		if (inWorld) {
			if (TestHost.answers >= asked) {
				inWorld = false;
				settling = true;
				ticks = 0;
			} else if (ticks > WORLD_LIMIT) {
				Checks.of(mods.get(mod)).fail("its world test was still running after " + WORLD_LIMIT + " ticks");
				inWorld = false;
				check = checksOf().size();
			}
			return false;
		}
		if (settling) {
			// The plot's chunks and the move there are on their way to a client on a server.
			boolean placed = mc.player != null && mc.world.isPosLoaded((int) Math.floor(mc.player.x), 64, (int) Math.floor(mc.player.z)) && mc.player.onGround;
			if (!placed && ticks < PLACED_LIMIT) return false;
			if (ticks < 10) return false;
			settling = false;
			if (!checksOf().isEmpty()) checksOf().get(0).start(mc, results);
			return false;
		}
		if (ticks > CLIENT_LIMIT) {
			Checks.of(mods.get(mod)).fail("its client test was still running after " + CLIENT_LIMIT + " ticks");
			check = checksOf().size();
			return false;
		}
		if (checksOf().get(check).tick(mc) && ++check < checksOf().size()) checksOf().get(check).start(mc, results);
		return false;
	}

	private List<WorldCheck> checksOf() {
		return checks.getOrDefault(mods.get(mod), new ArrayList<>());
	}

	private boolean next(Minecraft mc) {
		// One player goes through every mod's test, and a world saved with them dead opens on the death screen.
		if (mod >= 0 && mc.player.health <= 0) {
			Checks.of(mods.get(mod)).note("its test left the player dead: brought back for the next");
			mc.setScreen(null);
			mc.player.respawn();
			reviving = 40;
			return false;
		}
		if (++mod >= mods.size()) {
			String last = mods.get(mods.size() - 1);
			ending = true;
			ticks = 0;
			asked = TestHost.answers + 1;
			TestHost.channel.toServer(out -> {
				out.writeByte(TestHost.OP_END);
				out.writeUTF(last);
			});
			return false;
		}
		String name = mods.get(mod);
		int plot = mod;
		results.owner(name);
		check = 0;
		ticks = 0;
		asked = TestHost.answers + 1;
		inWorld = true;
		// The hotbar's first slot, as the world's side sets it: a server can't tell a client which slot it holds, and the last mod's test may have picked another.
		mc.player.inventory.selectedSlot = 0;
		TestHost.channel.toServer(out -> {
			out.writeByte(TestHost.OP_RUN);
			out.writeUTF(name);
			out.writeInt(plot);
			out.writeBoolean(TestHost.restarted());
		});
		return false;
	}
}
