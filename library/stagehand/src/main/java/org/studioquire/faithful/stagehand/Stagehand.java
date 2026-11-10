package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.faithful.stagehand.server.ServerSide;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * Scripts dropped in a folder, run where the world runs (docs/stagehand.md). Idle it's one look at a folder a second, on the tick, which won't usually exist: no thread, no watch on the folder, and
 * nothing written anywhere until a script comes. Everything here happens on the world's thread.
 */
public final class Stagehand implements FaithfulMod {
	/** A script that's still running after its first tick, past this many, is turned away. */
	static final int MOST_RUNNING = 16;
	private static final int LOOK_EVERY_TICKS = 20;

	private static final List<Run> RUNS = new ArrayList<>();
	private static Side side;
	private static int sinceLook;
	private static boolean inTick;
	private static boolean complained;

	@Override
	public void init(Mod mod) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			side(new ServerSide());
			Ticks.onServer(Stagehand::tick);
		}
		Lifecycle.onServerStop(Stagehand::stopped);
	}

	public static void side(Side here) {
		side = here;
	}

	static Side side() {
		return side;
	}

	/** Whichever tick runs on this side, each tick. */
	public static void tick() {
		if (inTick) return;
		inTick = true;
		try {
			advance();
			if (++sinceLook >= LOOK_EVERY_TICKS) {
				sinceLook = 0;
				Inbox.look();
			}
		} catch (RuntimeException e) {
			// The core would turn this listener off after a few. A script's own trouble doesn't come here, so this is a bug of ours: said once, and the rest goes on.
			if (!complained) {
				complained = true;
				System.out.println("[stagehand] WARN " + e);
				e.printStackTrace(System.out);
			}
		} finally {
			inTick = false;
		}
	}

	private static void advance() {
		if (RUNS.isEmpty()) return;
		for (Run run : new ArrayList<>(RUNS)) {
			if (run.tick(side)) RUNS.remove(run);
		}
	}

	/** Starts a script that's just arrived. */
	static void start(Run run) {
		if (run.tick(side)) return;
		RUNS.add(run);
		if (RUNS.size() > MOST_RUNNING) {
			run.end("failed", "too_many", "already " + MOST_RUNNING + " scripts waiting on something");
			run.tick(side);
			RUNS.remove(run);
		}
	}

	/** Ends the scripts running: this one, or all but the one asking. How many it ended. */
	static int cancel(String id, Run asking) {
		int ended = 0;
		for (Run run : RUNS) {
			if (run != asking && (id == null || run.id.equals(id))) {
				run.end("cancelled", "cancelled", asking.id + " cancelled it");
				ended++;
			}
		}
		return ended;
	}

	private static void stopped() {
		for (Run run : RUNS) {
			run.end("stopped", "stopped", "the world closed");
			run.tick(side);
		}
		RUNS.clear();
	}
}
