package org.studioquire.faithful.plainf3.selftest.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.passive.PigEntity;
import org.lwjgl.input.Keyboard;
import org.studioquire.faithful.harness.test.client.ClientTest;
import org.studioquire.oldfaithful.core.api.client.Input;
import org.studioquire.oldfaithful.selftest.LibraryScreens;
import org.studioquire.oldfaithful.selftest.StartupCheck;

/** A real F3 through SDL, then what the screen actually drew. In single player a pig stands in view, for the number beta would tag it with. */
public final class PlainF3Test extends ClientTest {
	private static final int F3 = 60;

	private int presses;
	private PigEntity pig;

	private void press() {
		StartupCheck.pushKey(F3, 0, true);
		StartupCheck.pushKey(F3, 0, false);
	}

	private static boolean drew(List<String> lines, String... starts) {
		for (String line : lines) {
			for (String start : starts) {
				if (line.startsWith(start)) return true;
			}
		}
		return false;
	}

	@Override
	protected void script() {
		Input.onKey(k -> {
			if (k.code == Keyboard.KEY_F3 && k.pressed) presses++;
		});
		then(() -> {
			mc.setScreen(null);
			press();
		});
		await("F3 to reach the game", 20, () -> presses == 1);
		then(() -> {
			if (!mc.world.isRemote) {
				double yaw = Math.toRadians(mc.player.yaw);
				pig = new PigEntity(mc.world);
				pig.setPosition(mc.player.x - Math.sin(yaw) * 3, mc.player.y - 1, mc.player.z + Math.cos(yaw) * 3);
				mc.world.spawnEntity(pig);
			}
			LibraryScreens.clear();
		});
		wait(5);
		then(() -> {
			List<String> lines = new ArrayList<>(LibraryScreens.drawn);
			check("F3 opens the screen", mc.options.debugHud);
			check("the frame rate is drawn", drew(lines, "Minecraft Beta 1.7.3 ("));
			check("chunk counts are drawn", drew(lines, "C: "));
			check("memory is drawn", drew(lines, "Used memory"));
			check("no position or facing", !drew(lines, "x: ", "y: ", "z: ", "f: "));
			check("no entity counts", !drew(lines, "E: ", "P: "));
			if (pig != null) check("no number on the pig", !lines.contains(Integer.toString(pig.id)));
		});
		picture("f3-screen");
		then(() -> {
			if (pig != null) pig.markDead();
			press();
		});
		await("F3 again", 20, () -> presses == 2);
		wait(5);
		then(() -> check("F3 closes it", !mc.options.debugHud));
	}
}
