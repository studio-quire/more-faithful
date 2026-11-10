package org.studioquire.faithful.harness.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import org.studioquire.faithful.harness.Out;
import org.studioquire.faithful.harness.mixin.EntityAccessor;
import org.studioquire.faithful.harness.test.server.ServerMove;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * Mods' tests on the side that runs the world, at the client's word, one mod at a time. In single player it's the same path, as a channel's message arrives at the next tick.
 * {@code -Dfaithful.test} turns it on: {@code first} or {@code restarted} on a client, {@code host} on a server, and {@code without} on a client whose server has none of this. Only ever on a test server: it empties a player's inventory and builds where it likes.
 */
public final class TestHost {
	public static final String MODE = System.getProperty("faithful.test");
	public static final int OP_RUN = 1, OP_SCENE = 2, OP_END = 3;
	public static final String ENTRYPOINT = "faithful:test", CLIENT_ENTRYPOINT = "faithful:test-client";
	private static final int PLOT_SPACING = 96, CLEAR_HEIGHT = 14;

	public static Channel channel;
	/** On the client: how many answers have come. */
	public static int answers;

	private static Map<String, List<WorldTest>> tests;
	private static final List<WorldTest> running = new ArrayList<>();
	private static PlayerEntity asker;
	private static String current;
	/** The mod whose turn it is on the plot, in a first run, and where that is. */
	private static String keeping;
	private static double plotX, plotY, plotZ;

	private TestHost() {
	}

	/** On a client: whether this run is the world's second life, after a save and a restart. */
	public static boolean restarted() {
		return "restarted".equals(MODE);
	}

	/** {@code some-mod-selftest}'s tests are {@code some-mod}'s. */
	public static String owner(String provider) {
		return provider.endsWith("-selftest") ? provider.substring(0, provider.length() - "-selftest".length()) : provider;
	}

	public static void register(Channel test) {
		channel = test;
		if (MODE == null) return;
		test.onServer(TestHost::receive);
		test.onClient((player, in) -> answers++);
		Ticks.onServer(TestHost::tick);
	}

	private static Map<String, List<WorldTest>> tests() {
		if (tests == null) {
			tests = new TreeMap<>();
			for (EntrypointContainer<WorldTest> c : FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, WorldTest.class)) {
				tests.computeIfAbsent(owner(c.getProvider().getMetadata().getId()), k -> new ArrayList<>()).add(c.getEntrypoint());
			}
		}
		return tests;
	}

	/** The mods with a {@link WorldTest}. */
	public static Set<String> mods() {
		return tests().keySet();
	}

	private static List<WorldTest> of(String mod) {
		return tests().getOrDefault(mod, Collections.emptyList());
	}

	private static void receive(PlayerEntity player, DataInputStream in) throws IOException {
		int op = in.readUnsignedByte();
		String mod = in.readUTF();
		current = mod;
		if (op == OP_RUN) {
			int plot = in.readInt();
			begin(mod, plot, in.readBoolean(), player);
		} else if (op == OP_END) {
			keep(player);
			if (plotY > 0) move(player, plotX, plotY, plotZ);
		} else if (op == OP_SCENE) {
			String name = in.readUTF();
			String[] args = new String[in.readUnsignedByte()];
			for (int i = 0; i < args.length; i++) args[i] = in.readUTF();
			boolean found = false;
			for (WorldTest t : of(mod)) {
				if (!t.scene(name, args)) continue;
				found = true;
				if (!running.contains(t)) running.add(t);
			}
			if (!found) Checks.of(mod).fail("no scene " + name + " " + Arrays.toString(args));
		}
		asker = player;
	}

	/** One player goes through every mod's test, so what a mod left in their hands for the restart is put by, and given back then. */
	private static Path kept(String mod) {
		return FabricLoader.getInstance().getGameDir().resolve("faithful-test").resolve(mod + ".dat");
	}

	private static void keep(PlayerEntity player) throws IOException {
		if (keeping == null) return;
		NbtCompound nbt = new NbtCompound();
		nbt.put("Inventory", player.inventory.writeNbt(new NbtList()));
		Files.createDirectories(kept(keeping).getParent());
		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(kept(keeping)))) {
			NbtIo.write(nbt, out);
		}
		keeping = null;
	}

	private static void giveBack(String mod, PlayerEntity player) throws IOException {
		if (!Files.isRegularFile(kept(mod))) {
			empty(player);
			return;
		}
		try (DataInputStream in = new DataInputStream(Files.newInputStream(kept(mod)))) {
			player.inventory.readNbt(NbtIo.read(in).getList("Inventory"));
		}
	}

	private static void empty(PlayerEntity player) {
		Arrays.fill(player.inventory.main, null);
		Arrays.fill(player.inventory.armor, null);
		player.inventory.setCursorStack(null);
	}

	private static void begin(String mod, int plot, boolean restarted, PlayerEntity player) throws IOException {
		keep(player);
		World world = player.world;
		int x = world.getProperties().getSpawnX() + plot * PLOT_SPACING, z = world.getProperties().getSpawnZ();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) ServerMove.load(world, x, z, WorldTest.PLOT + 16);
		// By the plot's corner, where the floor shows after a restart, whatever a test left in the middle.
		int y = Math.min(Math.max(world.getTopSolidBlockY(x + WorldTest.PLOT, z + WorldTest.PLOT), 64), 127 - CLEAR_HEIGHT);
		if (restarted) {
			giveBack(mod, player);
		} else {
			clearPlot(world, x, y, z);
			empty(player);
			keeping = mod;
		}
		player.health = 20;
		player.fireTicks = 0;
		// A client picks its own slot and says so, which its ClientTests does too; a server has no word for it in beta.
		player.inventory.selectedSlot = 0;
		plotX = x + 0.5;
		plotY = y + 0.01;
		plotZ = z + 0.5;
		move(player, plotX, plotY, plotZ);
		player.currentScreenHandler.sendContentUpdates();
		for (WorldTest t : of(mod)) {
			t.place(world, player, x, y, z, restarted);
			t.begin(Checks.of(mod));
			running.add(t);
		}
	}

	/** Liquid at the plot's edge, or one step from an open edge, turns to stone first: a spawn beside a lake floods the plot once it's cleared, and one test's torch then couldn't light through the water. */
	private static void sealCell(World world, int x, int y, int z, int outX, int outZ) {
		if (world.getMaterial(x, y, z).isFluid() || world.getBlockId(x, y, z) == 0 && world.getMaterial(x + outX, y, z + outZ).isFluid()) world.setBlock(x, y, z, Block.STONE.id);
	}

	private static void clearPlot(World world, int x, int y, int z) {
		int r = WorldTest.PLOT;
		for (int d = -r - 1; d <= r + 1; d++) {
			for (int dy = 127 - y; dy >= -1; dy--) {
				sealCell(world, x + d, y + dy, z - r - 1, 0, -1);
				sealCell(world, x + d, y + dy, z + r + 1, 0, 1);
				sealCell(world, x - r - 1, y + dy, z + d, -1, 0);
				sealCell(world, x + r + 1, y + dy, z + d, 1, 0);
			}
		}
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				// All the way up: sand left over it falls on the player whenever its queued block tick comes (a spawn under a sandy hill buried one).
				for (int dy = 127 - y; dy >= 0; dy--) world.setBlock(x + dx, y + dy, z + dz, 0);
				world.setBlock(x + dx, y - 1, z + dz, Block.STONE.id);
			}
		}
		for (Object o : new ArrayList<Object>(world.entities)) {
			Entity e = (Entity) o;
			if (!(e instanceof PlayerEntity) && Math.abs(e.x - x) <= r + 2 && Math.abs(e.z - z) <= r + 2) e.markDead();
		}
	}

	private static void move(PlayerEntity player, double x, double y, double z) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) ServerMove.to(player, x, y, z);
		else player.setPosition(x, y + player.standingEyeHeight, z);
		player.velocityX = player.velocityY = player.velocityZ = 0;
		((EntityAccessor) player).fallDistance(0);
		// Beta files an entity under its chunk in the entity's own update, and finds things near a place by chunk (a blast's reach, a pickup), while a test's
		// first steps run in this very tick: without this, a blast on the plot misses the player, still filed where the last test left them.
		player.world.updateEntity(player, false);
	}

	private static void tick() {
		if (asker == null) return;
		running.removeIf(WorldTest::advance);
		if (!running.isEmpty()) return;
		PlayerEntity to = asker;
		asker = null;
		Out.done("test " + current);
		channel.toPlayer(to, out -> out.writeByte(1));
	}
}
