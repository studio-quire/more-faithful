package org.studioquire.faithful.towercheck.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.api.Worldgen;

/**
 * Real generation by a script, for {@code :harness:buriedTowersTest} and the pictures: chunks loaded in a chosen order, some a tick, as a player's walk would bring them but in any order we like, and
 * what really stands in them written to files. With or without Buried Towers, which only {@link Survey} touches.
 */
public final class TowerCheck implements FaithfulMod {
	private static final String DONE = "[tower-check] DONE ";
	static final boolean TOWERS = FabricLoader.getInstance().isModLoaded("buried-towers");

	private List<int[]> queue = new ArrayList<>();
	private int perTick, position;
	/** Each chunk's load in the last {@code gen}, decoration of its neighbours included, for {@code tools/gen_bench.py}. */
	private long[] times;
	private int quiet = -1;

	@Override
	public void init(Mod mod) {
		Commands.register("towertest", "Buried Towers' test driver", (args, reply) -> {
			try {
				command(args);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		Ticks.onWorld(this::tick);
	}

	private static List<int[]> order(int x0, int z0, int size, String order) {
		List<int[]> chunks = new ArrayList<>();
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) chunks.add(new int[] {x0 + x, z0 + z});
		}
		if (order.equals("back")) {
			Collections.reverse(chunks);
		} else if (order.startsWith("shuffle")) {
			Collections.shuffle(chunks, new Random(Long.parseLong(order.substring(7))));
		} else if (order.equals("rings")) {
			double c = (size - 1) / 2.0;
			chunks.sort((a, b) -> Double.compare(Math.max(Math.abs(a[0] - x0 - c), Math.abs(a[1] - z0 - c)), Math.max(Math.abs(b[0] - x0 - c), Math.abs(b[1] - z0 - c))));
		} else if (!order.equals("rows")) {
			throw new IllegalArgumentException("No such order: " + order);
		}
		return chunks;
	}

	/** {@code gen x0 z0 size order perTick}, {@code settle}, {@code sites x0 z0 size file}, {@code places rx0 rz0 squares margin file}, {@code plans rx0 rz0 squares file}, {@code tower x z file} (x and z any cell in its square), {@code cells x0 z0 size file}, {@code doors x0 z0 size file}, {@code crash saves}. */
	private void command(String[] args) throws IOException {
		switch (args[0]) {
			case "gen":
				queue = order(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4]);
				if (args.length > 6) queue = new ArrayList<>(queue.subList(0, Integer.parseInt(args[6])));
				perTick = Integer.parseInt(args[5]);
				position = 0;
				break;
			case "settle":
				quiet = 0;
				break;
			case "sites":
				Survey.sites(world(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), file(args[4]));
				System.out.println(DONE + "sites");
				break;
			case "places":
				Survey.places(world(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), file(args[5]));
				System.out.println(DONE + "places");
				break;
			case "tower":
				System.out.println(DONE + "tower " + Survey.tower(world(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), file(args[3])));
				break;
			case "cells":
				cells(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), file(args[4]));
				System.out.println(DONE + "cells");
				break;
			case "plans":
				Survey.plans(world(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), file(args[4]));
				System.out.println(DONE + "plans");
				break;
			case "report":
				Survey.report(world(), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), file(args[4]));
				System.out.println(DONE + "report");
				break;
			case "set":
				world().setBlock(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
				System.out.println(DONE + "set");
				break;
			case "doors":
				doors(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), file(args[4]));
				System.out.println(DONE + "doors");
				break;
			case "get":
				System.out.println(DONE + "get " + world().getBlockId(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3])));
				break;
			case "crash": {
				ServerWorld world = (ServerWorld) world();
				for (int i = 0; i < Integer.parseInt(args[1]); i++) world.chunkCache.save(false, null);
				Runtime.getRuntime().halt(1);
				break;
			}
			default:
				throw new IllegalArgumentException(args[0]);
		}
		Pace.hurry(waitingOnTicks());
	}

	private boolean waitingOnTicks() {
		return position < queue.size() || quiet >= 0;
	}

	private static String timing(long[] nanos) {
		long[] sorted = nanos.clone();
		java.util.Arrays.sort(sorted);
		long total = 0;
		int over20 = 0;
		for (long n : sorted) {
			total += n;
			if (n > 20_000_000L) over20++;
		}
		return String.format(java.util.Locale.ROOT, "chunks=%d total_ms=%.1f mean_ms=%.3f p50_ms=%.3f p99_ms=%.3f max_ms=%.2f over20=%d", sorted.length, total / 1e6, total / 1e6 / sorted.length,
				sorted[sorted.length / 2] / 1e6, sorted[(int) (sorted.length * 0.99)] / 1e6, sorted[sorted.length - 1] / 1e6, over20);
	}

	private static World world() {
		return Server.world(Worldgen.OVERWORLD);
	}

	private static Path file(String name) throws IOException {
		Path path = Paths.get(name).toAbsolutePath();
		Files.createDirectories(path.getParent());
		return path;
	}

	private void tick(World world) {
		if (world.dimension.id != Worldgen.OVERWORLD) return;
		if (position < queue.size()) {
			if (position == 0) times = new long[queue.size()];
			for (int i = 0; i < perTick && position < queue.size(); i++, position++) {
				int[] chunk = queue.get(position);
				long start = System.nanoTime();
				((ServerWorld) world).chunkCache.loadChunk(chunk[0], chunk[1]);
				times[position] = System.nanoTime() - start;
			}
			if (position == queue.size()) System.out.println(DONE + "gen " + timing(times));
		}
		if (quiet >= 0 && ++quiet >= 40) {
			quiet = -1;
			System.out.println(DONE + "settle");
		}
		Pace.hurry(waitingOnTicks());
	}

	/**
	 * Every iron door in a square of chunks with a lever by it, a line each: how it stands (closed, and its leaf across the doorway between solid jambs), then its lever pulled as a player would,
	 * how it stands then, and the lever pulled back.
	 */
	private static void doors(int x0, int z0, int size, Path file) throws IOException {
		World world = world();
		List<String> lines = new ArrayList<>();
		for (int x = x0 * 16; x < (x0 + size) * 16; x++) {
			for (int z = z0 * 16; z < (z0 + size) * 16; z++) {
				for (int y = 1; y < 127; y++) {
					if (world.getBlockId(x, y, z) != Block.IRON_DOOR.id || (world.getBlockMeta(x, y, z) & 8) != 0) continue;
					int[] lever = lever(world, x, y, z);
					if (lever == null) continue;
					String before = door(world, x, y, z);
					Block.LEVER.onUse(world, lever[0], lever[1], lever[2], null);
					String after = door(world, x, y, z);
					Block.LEVER.onUse(world, lever[0], lever[1], lever[2], null);
					lines.add(x + " " + y + " " + z + " before " + before + " pulled " + after + " back " + door(world, x, y, z));
				}
			}
		}
		Files.write(file, lines);
	}

	private static int[] lever(World world, int x, int y, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy <= 2; dy++) if (world.getBlockId(x + dx, y + dy, z + dz) == Block.LEVER.id) return new int[] {x + dx, y + dy, z + dz};
			}
		}
		return null;
	}

	/** {@code open} or {@code closed}, then {@code across} if the leaf stands between two solid jambs, so nothing passes, or {@code aside}. Beta's own rule for where the leaf is ({@code DoorBlock}). */
	private static String door(World world, int x, int y, int z) {
		int meta = world.getBlockMeta(x, y, z), side = (meta & 4) == 0 ? meta - 1 & 3 : meta & 3;
		boolean alongX = side == 0 || side == 2;
		boolean across = alongX ? world.shouldSuffocate(x - 1, y, z) && world.shouldSuffocate(x + 1, y, z) : world.shouldSuffocate(x, y, z - 1) && world.shouldSuffocate(x, y, z + 1);
		return ((meta & 4) == 0 ? "closed" : "open") + "," + (across ? "across" : "aside");
	}

	/** Every cell of a square of chunks. */
	private void cells(int x0, int z0, int size, Path file) throws IOException {
		World world = world();
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file), 1 << 16)) {
			byte[] header = new byte[12];
			put(header, 0, x0);
			put(header, 4, z0);
			put(header, 8, size);
			out.write(header);
			byte[] ids = new byte[32768], metas = new byte[32768];
			for (int cx = x0; cx < x0 + size; cx++) {
				for (int cz = z0; cz < z0 + size; cz++) {
					Chunk chunk = world.getChunk(cx, cz);
					out.write(chunk.terrainPopulated ? 1 : 0);
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							for (int y = 0; y < 128; y++) {
								ids[x << 11 | z << 7 | y] = (byte) chunk.getBlockId(x, y, z);
								metas[x << 11 | z << 7 | y] = (byte) chunk.getBlockMeta(x, y, z);
							}
						}
					}
					out.write(ids);
					out.write(metas);
				}
			}
		}
	}

	private static void put(byte[] to, int at, int v) {
		to[at] = (byte) (v >> 24);
		to[at + 1] = (byte) (v >> 16);
		to[at + 2] = (byte) (v >> 8);
		to[at + 3] = (byte) v;
	}
}
