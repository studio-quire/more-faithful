package org.studioquire.faithful.golems.selftest.client;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.faithful.golems.selftest.LevelRun;
import org.studioquire.faithful.golems.selftest.Look;
import org.studioquire.faithful.harness.test.client.ClientTest;

/**
 * What only a client shows or does: a player's own pace at mining and walking, the numbers the exchange halves; a golem made and handed its pick by real clicks, in single player and on
 * a server; the golem and its poles in a picture. With a level run, the land from above and at eye level instead.
 */
public final class GolemsClientTest extends ClientTest {
	private int sx, sy, sz;

	@Override
	protected void script() {
		// A look run is GolemsLookTest's.
		if (Look.asked()) return;
		// A follow run is the world test's, the player walked by it.
		if (org.studioquire.faithful.golems.selftest.FollowRun.asked() != null) {
			check("a follow run asks nothing of the client", true);
			return;
		}
		if (org.studioquire.faithful.golems.selftest.ArcherRun.asked() != null) {
			check("an archer survey asks nothing of the client", true);
			return;
		}
		LevelRun run = LevelRun.asked();
		if (run != null) {
			if (!restarted()) level(run);
			check("the client test ran", true);
			return;
		}
		if (restarted()) return;
		if (!remote()) measurePlayer();
		clicks();
		archer();
		if (!remote()) clank();
	}

	/** A sentry drawing its bow on a creeper 6 blocks off: the client is sent the bow up and its look on the creeper. */
	private void archer() {
		scene("archer");
		await("a sentry with its bow drawn", 100, () -> sentry() != null && sentry().drawsBow());
		wait(5);
		then(() -> {
			GolemEntity g = sentry();
			check("the client sees a sentry draw its bow", g != null && g.drawsBow());
			List<?> creepers = g == null ? java.util.Collections.emptyList() : mc.world.collectEntitiesByClass(net.minecraft.entity.mob.CreeperEntity.class, g.boundingBox.expand(10.0, 4.0, 10.0));
			if (g != null && !creepers.isEmpty()) {
				net.minecraft.entity.Entity c = (net.minecraft.entity.Entity) creepers.get(0);
				float toward = (float) (Math.atan2(c.z - g.z, c.x - g.x) * 180.0 / Math.PI) - 90.0F;
				float off = (((g.yaw - toward) % 360.0F) + 540.0F) % 360.0F - 180.0F;
				check("and looking at the creeper (" + Math.round(off) + " degrees off)", Math.abs(off) < 15.0F);
			} else {
				check("and looking at the creeper: the client sees it", false);
			}
			if (g != null && !remote()) look(g, "sentry-single");
		});
		// Its first arrow as soon as it may, as a skeleton's.
		until("and sees its arrows fly", 60, () -> sentry() != null && !mc.world.collectEntitiesByClass(net.minecraft.entity.projectile.ArrowEntity.class, sentry().boundingBox.expand(8.0, 4.0, 8.0)).isEmpty());
		scene("archer_done");
	}

	/** The golem the client sees holding a bow, or null. */
	private GolemEntity sentry() {
		for (Object o : mc.world.collectEntitiesByClass(GolemEntity.class, mc.player.boundingBox.expand(32.0, 16.0, 32.0))) {
			GolemEntity g = (GolemEntity) o;
			if (g.getHeldItem() != null && g.getHeldItem().getItem() == Item.BOW) return g;
		}
		return null;
	}

	/** Its clank, the mod's own short clip, is among the game's sounds and reads as one: looked up and read, never played. */
	private void clank() {
		then(() -> {
			net.minecraft.client.sound.Sound sound = null;
			try {
				// The sound manager's first table is its sounds (then streaming and music); found by its type, as its name differs between builds.
				for (java.lang.reflect.Field f : mc.soundManager.getClass().getDeclaredFields()) {
					if (f.getType() != net.minecraft.client.sound.SoundEntry.class) continue;
					f.setAccessible(true);
					sound = ((net.minecraft.client.sound.SoundEntry) f.get(mc.soundManager)).get("golems.clank");
					break;
				}
				javax.sound.sampled.AudioFormat format = sound == null ? null : javax.sound.sampled.AudioSystem.getAudioFileFormat(sound.soundFile).getFormat();
				check("its clank is one of the game's sounds, a short clip it can read" + (format == null ? "" : ": " + format), format != null && format.getSampleRate() == 44100.0F);
			} catch (ReflectiveOperationException | java.io.IOException | javax.sound.sampled.UnsupportedAudioFileException e) {
				check("its clank is one of the game's sounds: " + e, false);
			}
		});
	}

	/** A player's own numbers, on the real client: mining as a held button does it, walking as a held key does. */
	private void measurePlayer() {
		int fx = (int) Math.floor(mc.player.x), fy = (int) Math.floor(mc.player.y - 1.62), fz = (int) Math.floor(mc.player.z);
		int[] cell = {0}, ticks = {0}, first = {-1}, ends = {0};
		scene("row", fx, fy, fz);
		await("the blocks to mine", 40, () -> mc.world.getBlockId(fx + 2, fy, fz) == Block.STONE.id && mc.player.getHand() != null);
		// The game calls off mining each tick no button is held, unless it's told it isn't ticking.
		then(() -> mc.interactionManager.noTick = true);
		await("eight blocks mined", 400, () -> {
			ticks[0]++;
			if (cell[0] >= 8) return true;
			int cx = fx + 2 + cell[0] % 2, cz = fz - 2 + cell[0] / 2;
			mc.interactionManager.processBlockBreakingAction(cx, fy, cz, 1);
			if (mc.world.getBlockId(cx, fy, cz) == 0) {
				if (first[0] < 0) first[0] = ticks[0];
				if (++cell[0] >= 8) ends[0] = ticks[0];
			}
			return false;
		});
		then(() -> {
			mc.interactionManager.noTick = false;
			int took = ends[0] - first[0];
			System.out.println("[golems] a player: 7 blocks of stone with a diamond pick in " + took + " ticks");
			check("a player's block, button held, takes 12 ticks", took == 7 * 12);
		});
		double[] from = {0.0};
		int[] walked = {0};
		Input before = mc.player.input;
		then(() -> {
			mc.player.yaw = 90.0F;
			mc.player.input = new Input() {
				@Override
				public void update(PlayerEntity player) {
					movementForward = 1.0F;
				}
			};
		});
		wait(10);
		then(() -> from[0] = -mc.player.x);
		await("a walk", 80, () -> ++walked[0] >= 20);
		then(() -> {
			mc.player.input = before;
			double speed = (-mc.player.x - from[0]) / walked[0];
			System.out.println("[golems] a player walks " + speed + " blocks a tick");
			check("a player's walking was measured", speed > 0.1);
		});
	}

	/** Real right clicks: the core on an iron stack, then a pick on the golem it made. */
	private void clicks() {
		then(() -> {
			int gx = (int) Math.floor(mc.player.x) + 2, gy = (int) Math.floor(mc.player.y - 1.62), gz = (int) Math.floor(mc.player.z) + 2;
			double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
			scene("stack", gx, gy, gz);
			await("the iron and core", 60, () -> mc.player.getHand() != null && mc.player.getHand().getItem() == Golems.core.get() && mc.world.getBlockId(gx, gy + 1, gz) == Block.IRON_BLOCK.id);
			then(() -> check("the core makes a golem from the client", mc.interactionManager.interactBlock(mc.player, mc.world, mc.player.getHand(), gx, gy + 1, gz, 1)));
			wait(20);
			scene("stack_done");
			await("the pick in hand", 60, () -> mc.player.getHand() != null && mc.player.getHand().getItem() == Item.IRON_PICKAXE);
			then(() -> {
				GolemEntity g = near(gx, gz);
				check("the client sees the golem it made", g != null);
				if (g != null) mc.interactionManager.interactEntity(mc.player, g);
			});
			wait(20);
			then(() -> {
				GolemEntity g = near(gx, gz);
				check("and then with the pick in its hand", g != null && g.getHeldItem() != null && g.getHeldItem().getItem() == Item.IRON_PICKAXE);
				check("gone from the player's", mc.player.getHand() == null);
				boolean lit = true;
				for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) lit &= mc.world.getBlockId(gx + c[0], gy, gz + c[1]) == Golems.poleLit.get().id;
				check("and the client sees its poles glow", lit);
				if (g != null && !remote()) look(g, "golem-single");
				else if (g != null) picture("golem-server");
			});
			// How hurt it is reaches the client as its cracks: past half at 19 of 40, and an ingot taking a stage off.
			scene("hurt");
			await("a hurt golem drawn cracked past half", 60, () -> near(gx, gz) != null && near(gx, gz).cracks() == 2);
			scene("mended");
			await("an ingot taking a stage of its cracks off", 60, () -> near(gx, gz) != null && near(gx, gz).cracks() == 1);
			then(() -> check("the client is shown how hurt a golem is, and its mending", true));
			scene("given_done");
			// Back on the ground where it stood, within a click of the golem, after the picture from the air in single player.
			if (!remote()) {
				then(() -> {
					GolemEntity g = near(gx, gz);
					if (g != null) hold(px, py, pz, g.x, g.y + 0.9, g.z, 10);
				});
			}
			heldClicks(gx, gz);
		});
	}

	/**
	 * The right button held down on a golem, as the window has it from the system, through beta's repeat of a held click every 5 ticks: its bow taken back is never loosed, by the
	 * same click or a repeat (on the golem or on the ground), nor handed back; handed over, it isn't taken back; and an empty hand tells it to follow once.
	 */
	private void heldClicks(int gx, int gz) {
		int[] changes = {0}, follows = {0};
		boolean[] shot = {false};
		scene("held_bow");
		await("a golem with a bow, the player's hand empty and 16 arrows", 60, () -> near(gx, gz) != null && toolId(near(gx, gz)) == Item.BOW.id && mc.player.getHand() == null && arrows() == 16);
		press(gx, gz, true, changes, follows, shot);
		then(() -> {
			check("a held click takes the bow back once (" + changes[0] + " changes seen)", changes[0] == 1 && toolId(near(gx, gz)) == 0);
			check("into the hand, never loosed: the same click, a repeat on the golem or on the ground (" + arrows() + " arrows)", !shot[0] && arrows() == 16
					&& mc.player.getHand() != null && mc.player.getHand().getItem() == Item.BOW);
		});
		scene("held_taken");
		press(gx, gz, false, changes, follows, shot);
		then(() -> {
			check("a held click hands it the bow once (" + changes[0] + " changes seen), not taken back", changes[0] == 1 && toolId(near(gx, gz)) == Item.BOW.id && mc.player.getHand() == null);
			check("and nothing shot", !shot[0] && arrows() == 16);
		});
		scene("held_given");
		await("a golem with no tool, the player's hand empty", 60, () -> near(gx, gz) != null && toolId(near(gx, gz)) == 0 && mc.player.getHand() == null);
		press(gx, gz, false, changes, follows, shot);
		then(() -> {
			if (!remote()) check("a held click with an empty hand tells it to follow once (" + follows[0] + " times)", follows[0] == 1);
		});
		scene("held_done");
	}

	/**
	 * The right button pressed with the cursor on the golem and held 30 ticks (the last 15 on the ground before the player instead, if asked), then let go, the game believing its
	 * window has the mouse; counts the golem's tool changing and, in single player where its following is to hand, its following, and sees any arrow near.
	 */
	private void press(int gx, int gz, boolean thenGround, int[] changes, int[] follows, boolean[] shot) {
		int[] n = {0}, last = {0};
		String[] following = {null};
		await("the cursor on the golem", 40, () -> {
			aimAt(near(gx, gz));
			return mc.crosshairTarget != null && mc.crosshairTarget.entity != null && mc.crosshairTarget.entity == near(gx, gz);
		});
		then(() -> {
			check("no screen open for the clicks", mc.currentScreen == null);
			changes[0] = follows[0] = 0;
			shot[0] = false;
			last[0] = toolId(near(gx, gz));
			following[0] = near(gx, gz).following();
			mc.focused = true;
			rightButton(true);
		});
		await("the button held and let go", 60, () -> {
			GolemEntity g = near(gx, gz);
			int id = toolId(g);
			if (id != last[0]) changes[0]++;
			last[0] = id;
			String f = g == null ? null : g.following();
			if (!java.util.Objects.equals(f, following[0])) follows[0]++;
			following[0] = f;
			if (g != null && !mc.world.collectEntitiesByClass(net.minecraft.entity.projectile.ArrowEntity.class, g.boundingBox.expand(8.0, 4.0, 8.0)).isEmpty()) shot[0] = true;
			n[0]++;
			if (thenGround && n[0] > 15) {
				aimAt(mc.player.x - 1.5, Math.floor(mc.player.y - 1.62), mc.player.z - 1.5);
			} else {
				aimAt(g);
			}
			if (n[0] == 30) rightButton(false);
			// Ten more, for a server's word on the last of it to come.
			return n[0] >= 40;
		});
		then(() -> mc.focused = false);
	}

	/** Pressed or let go as the system would tell the window (an SDL right button, 3), into our LWJGL's queue and its held state alike. */
	private static void rightButton(boolean down) {
		try {
			Class.forName("org.studioquire.oldfaithful.lwjgl.MouseState").getMethod("button", int.class, boolean.class, long.class).invoke(null, 3, down, System.nanoTime());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("a held click needs our LWJGL component's mouse", e);
		}
	}

	private void aimAt(GolemEntity g) {
		if (g != null) aimAt(g.x, (g.boundingBox.minY + g.boundingBox.maxY) / 2.0, g.z);
	}

	private void aimAt(double x, double y, double z) {
		double dx = x - mc.player.x, dy = y - mc.player.y, dz = z - mc.player.z;
		mc.player.yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		mc.player.pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
		mc.player.prevYaw = mc.player.yaw;
		mc.player.prevPitch = mc.player.pitch;
		mc.gameRenderer.updateTargetedEntity(1.0F);
	}

	private static int toolId(GolemEntity g) {
		return g == null || g.getHeldItem() == null ? 0 : g.getHeldItem().itemId;
	}

	private int arrows() {
		int n = 0;
		for (net.minecraft.item.ItemStack s : mc.player.inventory.main) if (s != null && s.getItem() == Item.ARROW) n += s.count;
		return n;
	}

	private GolemEntity near(int x, int z) {
		List<?> golems = mc.world.collectEntitiesByClass(GolemEntity.class, mc.player.boundingBox.expand(12.0, 8.0, 12.0));
		for (Object o : golems) {
			GolemEntity g = (GolemEntity) o;
			if (Math.abs(g.x - (x + 0.5)) < 2.0 && Math.abs(g.z - (z + 0.5)) < 2.0) return g;
		}
		return null;
	}

	/** From a little off to the side and above it, with the poles of the plot behind. */
	private void look(GolemEntity g, String name) {
		hold(g.x - 3.2, g.y + 2.0, g.z - 3.2, g.x, g.y + 0.9, g.z, 20);
		picture(name);
	}

	private void level(LevelRun run) {
		String tag = run.property("name", "run") + (remote() ? "-server-" : "-single-");
		wait(10);
		then(() -> {
			sx = LevelRun.siteX;
			sy = LevelRun.siteY;
			sz = LevelRun.siteZ;
			mc.options.hideHud = true;
			int r = Integer.parseInt(run.property("half", "12")) + 6;
			if (!remote()) {
				mc.world.setTime(6000L);
				view(tag + "top", sx + 0.5, sy + 60.0, sz + 0.5, sx + 0.5, sy, sz + 0.5001, 160);
				view(tag + "high-1", sx + r + 0.5, sy + 20.0, sz + r + 0.5, sx, sy - 2.0, sz, 60);
				view(tag + "high-2", sx - r + 0.5, sy + 20.0, sz - r + 0.5, sx, sy - 2.0, sz, 60);
			} else {
				scene("level_time");
			}
			scene("level_to_golem");
			wait(20);
			then(() -> {
				List<?> golems = mc.world.collectEntitiesByClass(GolemEntity.class, mc.player.boundingBox.expand(16.0, 32.0, 16.0));
				check("the client sees the golem", !golems.isEmpty());
				if (golems.isEmpty()) return;
				GolemEntity g = (GolemEntity) golems.get(0);
				turnTo(tag + "golem", g.x, g.y + 0.9, g.z, 40);
			});
			then(() -> mc.options.hideHud = false);
		});
	}

	private void view(String name, double x, double y, double z, double toX, double toY, double toZ, int ticks) {
		hold(x, y, z, toX, toY, toZ, ticks);
		picture(name);
	}

	/** The player held in the air for a view, in single player. */
	private void hold(double x, double y, double z, double toX, double toY, double toZ, int ticks) {
		double dx = toX - x, dy = toY - y, dz = toZ - z;
		float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		float pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
		int[] n = {0};
		await("the view to fill in", ticks + 20, () -> {
			mc.player.setPositionAndAngles(x, y, z, yaw, pitch);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			return ++n[0] >= ticks;
		});
	}

	/** Turning to look from where the player stands, and a picture. */
	private void turnTo(String name, double toX, double toY, double toZ, int ticks) {
		int[] n = {0};
		await("the view to fill in", ticks + 20, () -> {
			double dx = toX - mc.player.x, dy = toY - mc.player.y, dz = toZ - mc.player.z;
			mc.player.yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
			mc.player.pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
			return ++n[0] >= ticks;
		});
		picture(name);
	}
}
