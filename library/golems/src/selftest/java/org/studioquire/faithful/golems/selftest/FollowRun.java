package org.studioquire.faithful.golems.selftest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.GolemEntity;

/**
 * A golem and a tamed wolf following the player over the same walk, to see how the golem follows beside beta's own follower: open ground, round a corner, up and down a slope, through
 * a doorway, along water and over a stream. Only when a {@code golems/follow.properties} is found in a folder above the game's, and only in single player, where the world test can walk
 * the player itself. Beside the file: the three trails from above ({@code <name>.png}) and what each follower did ({@code .txt}).
 */
public final class FollowRun {
	/** The player's walking speed, blocks a tick. */
	private static final double WALK = 0.2;
	private static final int AFTER = 200;

	private final File dir;
	private final Properties p;
	private World world;
	private PlayerEntity player;
	private GolemEntity golem;
	private WolfEntity wolf;
	private int ox, oy, oz, ticks, leg;
	private double px, pz;
	/** The player's way, relative to the course's corner, x and z: round the course's features, then a diagonal across open ground, where a walk from cell to cell shows. */
	private static final int[][] WAY = {{2, 0}, {18, 0}, {18, -10}, {4, -10}, {4, 10}, {22, 10}, {40, -8}, {40, 10}};
	private final List<double[]> trail = new ArrayList<>();

	private FollowRun(File dir, Properties p) {
		this.dir = dir;
		this.p = p;
	}

	public static FollowRun asked() {
		for (File f = new File(System.getProperty("user.dir")).getAbsoluteFile(); f != null; f = f.getParentFile()) {
			File file = new File(f, "golems/follow.properties");
			if (!file.isFile()) continue;
			Properties p = new Properties();
			try (java.io.Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (IOException e) {
				return null;
			}
			return new FollowRun(file.getParentFile(), p);
		}
		return null;
	}

	private String name() {
		return p.getProperty("name", "follow");
	}

	void start(GolemsTest test, World world, PlayerEntity player, int x, int y, int z) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			test.report("a follow run is single player's", true);
			return;
		}
		this.world = world;
		this.player = player;
		ox = x + 40;
		oy = y;
		oz = z;
		// Over the course to begin with, so its land loads; it falls to the ground until the walk starts.
		player.setPosition(ox + 2.5, world.getTopSolidBlockY(ox + 2, oz) + 1 + player.standingEyeHeight, oz + 0.5);
		test.levelLand(() -> world.isPosLoaded(ox + 44, 64, oz + 14) && world.isPosLoaded(ox, 64, oz - 14), () -> {
			build();
			px = ox + WAY[0][0] + 0.5;
			pz = oz + WAY[0][1] + 0.5;
			place();
			golem = new GolemEntity(world);
			golem.setPositionAndAngles(px - 1.5, oy, pz + 1.5, 0.0F, 0.0F);
			world.spawnEntity(golem);
			wolf = new WolfEntity(world);
			wolf.setPositionAndAngles(px - 1.5, oy, pz - 1.5, 0.0F, 0.0F);
			wolf.setTamed(true);
			wolf.setOwnerName(player.name);
			wolf.setSitting(false);
			world.spawnEntity(wolf);
			player.inventory.main[player.inventory.selectedSlot] = null;
			golem.interact(player);
			test.report("the golem follows the player", player.name.equals(golem.following()));
		});
		test.levelSteps(4000, () -> {
			ticks++;
			if (leg < WAY.length - 1) walk();
			place();
			trail.add(new double[] {px, pz, golem.x, golem.z, golem.boundingBox.minY, golem.yaw, wolf.x, wolf.z, wolf.boundingBox.minY, wolf.yaw});
			return leg >= WAY.length - 1 && ++ticksAfter > AFTER;
		}, () -> finish(test));
	}

	private int ticksAfter;

	/** The player a step along its way. */
	private void walk() {
		double tx = ox + WAY[leg + 1][0] + 0.5, tz = oz + WAY[leg + 1][1] + 0.5, dx = tx - px, dz = tz - pz, d = Math.sqrt(dx * dx + dz * dz);
		if (d <= WALK) {
			px = tx;
			pz = tz;
			leg++;
		} else {
			px += dx / d * WALK;
			pz += dz / d * WALK;
		}
	}

	private void place() {
		int bx = (int) Math.floor(px), bz = (int) Math.floor(pz);
		int y = oy;
		while (y < oy + 6 && world.getBlockId(bx, y, bz) != 0 && Block.BLOCKS[world.getBlockId(bx, y, bz)].material.blocksMovement()) y++;
		float yaw = leg < WAY.length - 1 ? (float) (Math.atan2(WAY[leg + 1][1] - WAY[leg][1], WAY[leg + 1][0] - WAY[leg][0]) * 180.0 / Math.PI) - 90.0F : player.yaw;
		player.setPositionAndAngles(px, y + player.standingEyeHeight, pz, yaw, 0.0F);
		player.velocityX = player.velocityY = player.velocityZ = 0.0;
	}

	/** Flat grass over the course, with a wall at the corner, a slope of steps, a wall with a doorway, water along the way and a stream across it. */
	private void build() {
		for (int i = -2; i <= 46; i++) {
			for (int k = -16; k <= 16; k++) {
				int x = ox + i, z = oz + k;
				for (int h = -4; h <= 10; h++) world.setBlock(x, oy + h, z, h == -1 ? Block.GRASS_BLOCK.id : h < -1 ? Block.STONE.id : 0);
			}
		}
		// Round a corner: the inside of the turn walled.
		for (int i = 10; i <= 16; i++) for (int k = -10 + 2; k <= -1; k++) for (int h = 0; h < 3; h++) set(i, h, k, Block.COBBLESTONE.id);
		// Up and down a slope: steps along the way north, across its width.
		int[][] steps = {{-4, 1}, {-5, 1}, {-6, 2}, {-7, 2}, {-8, 1}};
		for (int[] s : steps) for (int i = 17; i <= 20; i++) for (int h = 0; h < s[1]; h++) set(i, h, s[0], Block.DIRT.id);
		// Through a doorway: a wall across the way west, one gap two high.
		for (int k = -14; k <= -6; k++) for (int h = 0; h < 3; h++) if (k != -10 || h == 2) set(11, h, k, Block.COBBLESTONE.id);
		// Along water: a channel beside the way south, then a stream across it.
		for (int k = -8; k <= 8; k++) set(2, -1, k, Block.WATER.id);
		for (int i = 2; i <= 7; i++) set(i, -1, 5, Block.WATER.id);
	}

	private void set(int i, int h, int k, int id) {
		world.setBlock(ox + i, oy + h, oz + k, id);
	}

	private void finish(GolemsTest test) {
		StringWriter out = new StringWriter();
		PrintWriter w = new PrintWriter(out);
		double walked = 0.0;
		for (int t = 1; t < trail.size(); t++) walked += Math.hypot(trail.get(t)[0] - trail.get(t - 1)[0], trail.get(t)[1] - trail.get(t - 1)[1]);
		w.printf("run %s: the player walked %.1f blocks in %d ticks, then stood %d%n", name(), walked, trail.size() - AFTER, AFTER);
		describe(w, "golem", 2);
		describe(w, "wolf", 6);
		w.flush();
		System.out.print("[golems] " + out);
		try {
			Files.write(new File(dir, name() + ".txt").toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
			picture();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		test.report("the follow run was written", true);
	}

	/** A follower's walk: how far behind, how often it stood while the player was well off, how many times it set off again, how it turned, how far it went. */
	private void describe(PrintWriter w, String who, int c) {
		double sum = 0.0, most = 0.0, went = 0.0;
		int standing = 0, starts = 0, spins = 0, n = 0, wobbles = 0;
		boolean moving = false;
		double lastTurn = 0.0;
		for (int t = 1; t < trail.size(); t++) {
			double[] a = trail.get(t - 1), b = trail.get(t);
			double lag = Math.hypot(b[0] - b[c], b[1] - b[c + 1]), step = Math.hypot(b[c] - a[c], b[c + 1] - a[c + 1]);
			went += step;
			if (t < trail.size() - AFTER) {
				sum += lag;
				n++;
				most = Math.max(most, lag);
			}
			boolean now = step > 0.03;
			if (!now && lag > 5.0 && t < trail.size() - AFTER) standing++;
			if (now && !moving) starts++;
			moving = now;
			double turn = wrap(b[c + 3] - a[c + 3]);
			if (Math.abs(turn) > 45.0) spins++;
			// Its heading swinging one way then the other within a few ticks while it walks: a zigzag.
			if (now && Math.abs(turn) > 10.0) {
				if (Math.signum(turn) != Math.signum(lastTurn) && lastTurn != 0.0) wobbles++;
				lastTurn = turn;
			}
		}
		double[] end = trail.get(trail.size() - 1);
		w.printf("%s: behind by %.1f on average, %.1f at most; stood %d ticks while the player was more than 5 off; set off %d times; turned more than 45 degrees in a tick %d times;"
				+ " swung its heading back and forth %d times walking; went %.1f blocks; at the end %.1f from the player%n", who, sum / Math.max(1, n), most, standing, starts, spins, wobbles, went,
				Math.hypot(end[0] - end[c], end[1] - end[c + 1]));
	}

	private static double wrap(double a) {
		while (a < -180.0) a += 360.0;
		while (a >= 180.0) a -= 360.0;
		return a;
	}

	private void picture() throws IOException {
		int scale = 12, w = 50 * scale, h = 34 * scale;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int i = -2; i < 48; i++) {
			for (int k = -16; k < 18; k++) {
				int x = ox + i, z = oz + k, colour = 0x5c8f3c;
				for (int hh = 3; hh >= -1; hh--) {
					int id = world.getBlockId(x, oy + hh, z);
					if (id == 0) continue;
					colour = id == Block.WATER.id ? 0x3060c0 : id == Block.COBBLESTONE.id ? 0x707070 : id == Block.DIRT.id ? 0x866043 + 0x101010 * hh : 0x5c8f3c;
					break;
				}
				for (int a = 0; a < scale; a++) for (int b = 0; b < scale; b++) image.setRGB((i + 2) * scale + a, (k + 16) * scale + b, colour);
			}
		}
		Graphics2D g = image.createGraphics();
		int[][] cols = {{0, 0xffffff}, {2, 0x40e0ff}, {6, 0xffa030}};
		for (int[] col : cols) {
			g.setColor(new Color(col[1]));
			for (int t = 1; t < trail.size(); t++) {
				double[] a = trail.get(t - 1), b = trail.get(t);
				g.drawLine(ix(a[col[0]]), iz(a[col[0] + 1]), ix(b[col[0]]), iz(b[col[0] + 1]));
				if (t % 20 == 0) g.fillOval(ix(b[col[0]]) - 2, iz(b[col[0] + 1]) - 2, 5, 5);
			}
		}
		g.dispose();
		ImageIO.write(image, "png", new File(dir, name() + ".png"));
	}

	private int ix(double x) {
		return (int) Math.round((x - ox + 2) * 12);
	}

	private int iz(double z) {
		return (int) Math.round((z - oz + 16) * 12);
	}
}
