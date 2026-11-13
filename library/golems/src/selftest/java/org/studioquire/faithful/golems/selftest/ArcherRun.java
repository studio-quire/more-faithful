package org.studioquire.faithful.golems.selftest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.harness.test.server.ServerMove;

/**
 * The archer against what it shoots, many times over, for numbers to balance it by (the user's lever, its rate, 2026-11-10): only when a {@code golems/archer.properties} is found in a
 * folder above the game's. In a walled arena roofed with glowstone east of the plot (lit to 9 on the floor: nothing spawns, nothing burns, and a spider stays hostile under 0.5 of full
 * brightness), a fresh golem with a bow and 32 arrows at the middle, and a foe that has seen it set down {@code distance} off, a different way each trial. A trial ends when the foe is
 * killed, a creeper goes off, the golem goes down, or after {@code maxTicks}. Modes: {@code ground}, as built (stepping back); {@code stand}, never stepping back, as the first build's
 * archer stood its ground; {@code perch}, creepers only, the golem on a pillar 4 high with a fence round its foot 4 out, as the self-test's tower. Beside the file, a table by mode, foe
 * and rate: kills, creepers gone off, the golem down, shots and the health it lost on average.
 * <p>
 * Keys: {@code name}; {@code trials} (6) each; {@code rates} (30,45,60); {@code foes} (creeper,zombie,skeleton,spider); {@code modes} (ground,stand,perch); {@code distance} (10);
 * {@code maxTicks} (600) a trial; {@code half} (14), the arena's half width inside. Run it on a server alone, quickly: {@code -Plibrary.serverOnly -Ptest.speed=20}.
 */
public final class ArcherRun {
	private static final Pattern SHOTS = Pattern.compile("(\\d+) shots");

	private final File dir;
	private final Properties p;
	private World world;
	private PlayerEntity player;
	private int cx, cy, cz, half, maxTicks, trials, distance;
	private final List<String[]> combos = new ArrayList<>();
	private int combo, trial, ticks, trialsDone;
	private GolemEntity golem;
	private LivingEntity foe;
	private boolean running;
	private int lowest, backedStart;
	private double nearest;
	private final Map<String, int[]> table = new LinkedHashMap<>();
	private final StringBuilder log = new StringBuilder();

	private ArcherRun(File dir, Properties p) {
		this.dir = dir;
		this.p = p;
	}

	public static ArcherRun asked() {
		for (File f = new File(System.getProperty("user.dir")).getAbsoluteFile(); f != null; f = f.getParentFile()) {
			File file = new File(f, "golems/archer.properties");
			if (!file.isFile()) continue;
			Properties p = new Properties();
			try (java.io.Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (IOException e) {
				return null;
			}
			return new ArcherRun(file.getParentFile(), p);
		}
		return null;
	}

	private String property(String key, String fallback) {
		return p.getProperty(key, fallback).trim();
	}

	private int prop(String key, int fallback) {
		return Integer.parseInt(property(key, String.valueOf(fallback)));
	}

	private static boolean onServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	void start(GolemsTest test, World world, PlayerEntity player, int x, int y, int z) {
		this.world = world;
		this.player = player;
		half = prop("half", 14);
		maxTicks = prop("maxTicks", 600);
		trials = prop("trials", 6);
		distance = prop("distance", 10);
		cx = x + half + 18;
		cy = y;
		cz = z;
		for (String mode : property("modes", "ground,stand,perch").split(",")) {
			for (String foe : property("foes", "creeper,zombie,skeleton,spider").split(",")) {
				if (mode.trim().equals("perch") && !foe.trim().equals("creeper")) continue;
				for (String rate : property("rates", "30,45,60").split(",")) combos.add(new String[] {mode.trim(), foe.trim(), rate.trim()});
			}
		}
		if (onServer()) ServerMove.load(world, cx, cz, half + 16);
		world.difficulty = 1;
		build();
		test.levelSteps(combos.size() * trials * (maxTicks + 5) + 200, this::tick, () -> finish(test));
	}

	/** The arena: a stone floor two deep, walls of stone six high, a glowstone roof; for a perch, the pillar and the fence round its foot, else neither. Only what differs is set. */
	private void build() {
		boolean perch = combo < combos.size() && combos.get(combo)[0].equals("perch");
		int w = half + 1;
		for (int i = -w; i <= w; i++) {
			for (int k = -w; k <= w; k++) {
				boolean wall = Math.abs(i) == w || Math.abs(k) == w;
				put(i, -2, k, Block.STONE.id);
				put(i, -1, k, Block.STONE.id);
				put(i, 6, k, Block.GLOWSTONE.id);
				boolean pillar = perch && i == 0 && k == 0;
				boolean fence = perch && Math.max(Math.abs(i), Math.abs(k)) == 4;
				for (int h = 0; h < 6; h++) put(i, h, k, wall || pillar && h < 4 ? Block.STONE.id : fence && h == 0 ? Block.FENCE.id : 0);
			}
		}
	}

	private void put(int i, int h, int k, int id) {
		if (world.getBlockId(cx + i, cy + h, cz + k) != id) world.setBlock(cx + i, cy + h, cz + k, id);
	}

	private boolean tick() {
		if (combo >= combos.size()) return true;
		if (!running) {
			begin();
			return false;
		}
		ticks++;
		lowest = Math.min(lowest, golem.health);
		if (foe.isAlive()) nearest = Math.min(nearest, Math.sqrt((foe.x - golem.x) * (foe.x - golem.x) + (foe.z - golem.z) * (foe.z - golem.z)));
		String outcome = foe.health <= 0 ? "killed" : !foe.isAlive() ? (foe instanceof CreeperEntity ? "went off" : "gone") : golem.health <= 0 ? "golem down" : ticks >= maxTicks ? "time" : null;
		if (outcome == null) return false;
		String[] c = combos.get(combo);
		Matcher m = SHOTS.matcher(golem.status());
		int shots = m.find() ? Integer.parseInt(m.group(1)) : 0;
		int lost = GolemEntity.MAX_HEALTH - Math.max(0, golem.health);
		String key = c[0] + " " + c[1] + " " + c[2];
		int[] row = table.computeIfAbsent(key, k -> new int[8]);
		row[0]++;
		if (outcome.equals("killed")) row[1]++;
		if (outcome.equals("went off")) row[2]++;
		if (outcome.equals("golem down")) row[3]++;
		if (outcome.equals("time")) row[4]++;
		row[5] += shots;
		row[6] += lost;
		row[7] += ticks;
		log.append(String.format(Locale.ROOT, "%s trial %d: %s after %d ticks, %d shots, the golem lost %d, the foe came within %.1f, stepped back %d times%n", key, trial + 1, outcome, ticks, shots,
				lost, nearest, golem.backedOff() - backedStart));
		running = false;
		golem.markDead();
		foe.markDead();
		if (++trial >= trials) {
			trial = 0;
			combo++;
			// Written as each set ends, so a run cut short still leaves what it found.
			write();
		}
		trialsDone++;
		if (trialsDone % 10 == 0) System.out.println("[golems] archer survey: " + trialsDone + " trials done");
		return combo >= combos.size();
	}

	private void begin() {
		String[] c = combos.get(combo);
		build();
		for (Object o : world.collectEntitiesByClass(Entity.class, Box.createCached(cx - half - 2, cy - 2, cz - half - 2, cx + half + 3, cy + 8, cz + half + 3))) {
			if (!(o instanceof PlayerEntity)) ((Entity) o).markDead();
		}
		set("org.studioquire.faithful.golems.BowJob", "shootEvery", Integer.parseInt(c[2]));
		set("org.studioquire.faithful.golems.Worker", "standGround", c[0].equals("stand"));
		boolean perch = c[0].equals("perch");
		golem = new GolemEntity(world);
		golem.setPositionAndAngles(cx + 0.5, cy + (perch ? 4 : 0), cz + 0.5, 0.0F, 0.0F);
		world.spawnEntity(golem);
		ItemStack hand = player.inventory.main[player.inventory.selectedSlot];
		player.inventory.main[player.inventory.selectedSlot] = new ItemStack(Item.BOW);
		golem.interact(player);
		player.inventory.main[player.inventory.selectedSlot] = hand;
		golem.setSlot(0, new ItemStack(Item.ARROW, 16));
		golem.setSlot(1, new ItemStack(Item.ARROW, 16));
		double a = Math.toRadians(45.0 + trial * 137.5);
		double d = Math.min(distance, half - 1.5);
		foe = c[1].equals("creeper") ? new CreeperEntity(world) : c[1].equals("skeleton") ? new SkeletonEntity(world) : c[1].equals("spider") ? new SpiderEntity(world) : new ZombieEntity(world);
		foe.setPositionAndAngles(cx + 0.5 + Math.cos(a) * d, cy, cz + 0.5 + Math.sin(a) * d, 0.0F, 0.0F);
		world.spawnEntity(foe);
		((MobEntity) foe).setTarget(golem);
		ticks = 0;
		lowest = golem.health;
		nearest = Double.MAX_VALUE;
		backedStart = golem.backedOff();
		running = true;
	}

	private static void set(String type, String field, Object value) {
		try {
			java.lang.reflect.Field f = Class.forName(type).getDeclaredField(field);
			f.setAccessible(true);
			f.set(null, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("the survey's switch " + type + "." + field, e);
		}
	}

	private void finish(GolemsTest test) {
		set("org.studioquire.faithful.golems.BowJob", "shootEvery", 30);
		set("org.studioquire.faithful.golems.Worker", "standGround", false);
		System.out.print("[golems] archer survey\n" + write());
		test.report("the archer survey ran " + trialsDone + " trials", trialsDone == combos.size() * trials);
	}

	/** The table so far, written beside the file, and returned. */
	private String write() {
		StringBuilder out = new StringBuilder();
		out.append(String.format(Locale.ROOT, "%-24s %6s %7s %8s %11s %5s %7s %9s %9s%n", "mode foe rate", "trials", "killed", "went off", "golem down", "time", "shots", "lost (40)", "ticks"));
		for (Map.Entry<String, int[]> e : table.entrySet()) {
			int[] r = e.getValue();
			out.append(String.format(Locale.ROOT, "%-24s %6d %7d %8d %11d %5d %7.1f %9.1f %9.0f%n", e.getKey(), r[0], r[1], r[2], r[3], r[4], r[5] / (double) r[0], r[6] / (double) r[0], r[7] / (double) r[0]));
		}
		try {
			Files.write(new File(dir, property("name", "archer") + (onServer() ? "-server" : "-single") + ".txt").toPath(), (out + "\n" + log).getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toString();
	}
}
