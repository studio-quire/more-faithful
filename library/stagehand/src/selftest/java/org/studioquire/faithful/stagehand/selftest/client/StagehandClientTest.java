package org.studioquire.faithful.stagehand.selftest.client;

import java.util.Map;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.faithful.stagehand.selftest.Scripts;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * What the player sees of it, on a server: a long move and a change of dimension, standing on ground that has arrived, never kicked. In single player: everything still works from the pause menu,
 * which is where the game is whenever someone has alt-tabbed away to talk to a session.
 */
public final class StagehandClientTest extends ClientTest {
	private double startX, lowestThere = Double.MAX_VALUE;
	private boolean sampling;

	/** A client with no chunk under it falls, so what counts is how far down it ever got, tick by tick from the moment it arrived. */
	private void sample() {
		if (sampling && mc.player != null && Math.abs(mc.player.x - startX) > 2900) lowestThere = Math.min(lowestThere, mc.player.boundingBox.minY);
	}

	private boolean groundUnder() {
		int x = MathHelper.floor(mc.player.x), y = MathHelper.floor(mc.player.boundingBox.minY), z = MathHelper.floor(mc.player.z);
		return mc.world.getBlockId(x, y - 1, z) != 0 || mc.world.getBlockId(x, y, z) != 0;
	}

	private boolean inWater() {
		int x = MathHelper.floor(mc.player.x), y = MathHelper.floor(mc.player.boundingBox.minY), z = MathHelper.floor(mc.player.z);
		return mc.world.getMaterial(x, y, z).isFluid() || mc.world.getMaterial(x, y + 1, z).isFluid();
	}

	@Override
	protected void script() {
		if (restarted()) {
			then(() -> check("still here after a restart", mc.player != null));
			return;
		}
		if (remote()) {
			refusedOnClient();
		} else {
			paused();
		}
		farAndBack();
		netherAndBack();
	}

	private void farAndBack() {
		then(() -> {
			startX = mc.player.x;
			sampling = true;
			Ticks.onClient(this::sample);
		});
		scene("far-start");
		until("the client is there", 200, () -> Math.abs(mc.player.x - startX) > 2900);
		until("and the ground has reached it", 200, this::groundUnder);
		double[] y = new double[1];
		wait(5);
		then(() -> y[0] = mc.player.boundingBox.minY);
		wait(40);
		then(() -> {
			// Landing in the sea is allowed, where nothing's left to compare: someone treading water sinks.
			check("it's standing still, not falling: " + y[0] + " then " + mc.player.boundingBox.minY, inWater() || Math.abs(y[0] - mc.player.boundingBox.minY) < 1.0 && groundUnder());
			check("and it never dropped on the way in: stands at " + y[0] + ", lowest " + lowestThere, inWater() || y[0] - lowestThere < 1.0);
			sampling = false;
			check("unhurt", mc.player.health > 0);
		});
		picture("far");
		scene("home");
		until("the client is home", 200, () -> Math.abs(mc.player.x - startX) < 16);
		wait(40);
		then(() -> check("home, on the plot", groundUnder()));
	}

	private void netherAndBack() {
		scene("nether-start");
		until("the client is in the Nether", 300, () -> mc.player.dimensionId == -1 && mc.world.dimension.isNether);
		until("with ground", 300, this::groundUnder);
		wait(40);
		then(() -> check("standing, alive, in the Nether, its screen gone: " + mc.currentScreen, mc.player.health > 0 && groundUnder() && mc.currentScreen == null));
		picture("nether");
		scene("home");
		until("the client is back", 300, () -> mc.player.dimensionId == 0 && !mc.world.dimension.isNether);
		until("on the plot", 300, () -> Math.abs(mc.player.x - startX) < 16 && groundUnder());
		wait(20);
		then(() -> check("back from the Nether, alive", mc.player.health > 0 && groundUnder() && mc.currentScreen == null));
		scene("nether-end");
	}

	/** A game joined to a server has no world of its own to act on, and says so rather than leaving a session waiting. */
	private void refusedOnClient() {
		then(() -> Scripts.send("on-client", "status\n"));
		await("the client's own answer", 200, () -> Scripts.over("on-client"));
		then(() -> {
			Map<String, Object> result = Scripts.result("on-client");
			check("a script dropped at a client on a server is told why not: " + result.get("error"), "refused".equals(result.get("state")) && "on_a_server".equals(Scripts.errorCode(result)));
		});
	}

	private void paused() {
		int[] home = new int[3];
		then(() -> {
			home[0] = MathHelper.floor(mc.player.x);
			home[1] = MathHelper.floor(mc.player.boundingBox.minY);
			home[2] = MathHelper.floor(mc.player.z);
			mc.setScreen(new GameMenuScreen());
		});
		await("the pause", 100, () -> mc.paused);
		then(() -> Scripts.send("while-paused", "give apple 2\ntp ~2 ~ ~\nstatus\nwait 10\ngive apple 3\n"));
		await("the script to get as far as it can", 200, () -> {
			Map<String, Object> result = Scripts.result("while-paused");
			return result != null && Scripts.steps(result).size() == 3;
		});
		wait(30);
		then(() -> {
			Map<String, Object> result = Scripts.result("while-paused");
			check("paused, a script runs until it wants time to pass: " + result.get("waiting_on"), "running".equals(result.get("state")) && Boolean.TRUE.equals(result.get("paused")) && Scripts.steps(result).size() == 3);
			check("what it did has happened", "single player".equals(result.get("where")) && Boolean.TRUE.equals(Scripts.data(result, 2).get("paused")));
		});
		then(() -> Scripts.send("nether-paused", "tp 10 64 10 dim=nether\nstatus\ntp ~ ~ ~\n"));
		await("the Nether from the pause menu", 900, () -> Scripts.over("nether-paused"));
		then(() -> {
			Map<String, Object> result = Scripts.result("nether-paused");
			check("to the Nether from the pause menu, the menu still up: " + result.get("error") + " " + mc.currentScreen, "done".equals(result.get("state")) && "nether".equals(Scripts.data(result, 1).get("dim")) && mc.currentScreen instanceof GameMenuScreen);
		});
		then(() -> Scripts.send("home-paused", "tp " + home[0] + " " + home[1] + " " + home[2] + " dim=overworld\n"));
		await("home from the pause menu", 900, () -> Scripts.over("home-paused"));
		then(() -> check("and back, the menu still up: " + Scripts.result("home-paused").get("error"), "done".equals(Scripts.result("home-paused").get("state")) && mc.player.dimensionId == 0 && mc.currentScreen instanceof GameMenuScreen));
		then(() -> mc.setScreen(null));
		await("while-paused to end", 200, () -> Scripts.over("while-paused"));
		then(() -> check("unpaused, it goes on: " + Scripts.result("while-paused").get("state"), "done".equals(Scripts.result("while-paused").get("state"))));
		scene("home");
		scene("empty-hands");
	}
}
