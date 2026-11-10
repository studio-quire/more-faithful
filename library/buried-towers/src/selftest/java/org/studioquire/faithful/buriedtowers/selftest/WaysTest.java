package org.studioquire.faithful.buriedtowers.selftest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.LightType;
import net.minecraft.world.gen.feature.DungeonFeature;
import org.studioquire.faithful.buriedtowers.Loot;
import org.studioquire.faithful.buriedtowers.Towers;
import org.studioquire.faithful.buriedtowers.plan.Blocks;
import org.studioquire.faithful.harness.test.WorldTest;

/**
 * Whether the ways down work by beta's own rules, in the real game's physics, on small copies built as generation writes them: with no neighbour told. A player's fall only runs in single player here,
 * where the world's player is the one that moves; the rest runs on the server too. Real towers in real terrain are {@code :harness:buriedTowersTest}'s.
 */
public final class WaysTest extends WorldTest {
	/** The plot's middle column, lifted clear of its stone floor. */
	private static final int LIFT = 2;
	private int health;

	private void quiet(int dx, int dy, int dz, int id, int meta) {
		world.setBlockWithoutNotifyingNeighbors(x + dx, y + dy, z + dz, id, meta);
		world.blockUpdateEvent(x + dx, y + dy, z + dz);
	}

	private void wipe() {
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				for (int dy = 0; dy < 60; dy++) if (world.getBlockId(x + dx, y + dy, z + dz) != 0) quiet(dx, dy, dz, 0, 0);
			}
		}
	}

	/**
	 * A pool two deep with a rim, as a way down builds one, its water's top cell at {@code LIFT + 1}. The rim's top course is glowstone: in a cold biome beta's chunk tick turns
	 * still water open to the sky to ice, unless the cell over it has a block light of 10 or more, and on a plot on snow a pool froze and the falls into it hit ice (2026-11-10).
	 * Light changes nothing a fall meets.
	 */
	private void pool(int half) {
		for (int dx = -half - 1; dx <= half + 1; dx++) {
			for (int dz = -half - 1; dz <= half + 1; dz++) {
				boolean rim = Math.max(Math.abs(dx), Math.abs(dz)) == half + 1;
				quiet(dx, LIFT - 1, dz, Blocks.STONE, 0);
				quiet(dx, LIFT, dz, rim ? Blocks.STONE : Blocks.WATER, 0);
				quiet(dx, LIFT + 1, dz, rim ? Blocks.GLOWSTONE : Blocks.WATER, 0);
			}
		}
	}

	/** Beta's freezing rule without the biome: the top of a column, still water, with a block light under 10 over it. */
	private boolean couldFreeze(int half) {
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				int top = world.getTopSolidBlockY(x + dx, z + dz);
				if (top < 128 && world.getBlockId(x + dx, top - 1, z + dz) == Blocks.WATER && world.getBlockMeta(x + dx, top - 1, z + dz) == 0
						&& world.getBrightness(LightType.BLOCK, x + dx, top, z + dz) < 10) return true;
			}
		}
		return false;
	}

	private void unfrozen(int half) {
		await("no water there that a cold biome could freeze", 40, () -> !couldFreeze(half));
	}

	private void place(double dy) {
		// Beta's death leaves a player 0.2 high with their eyes 0.1 up, and setting their health back doesn't undo it: after one, a drop into sand reads as suffocation.
		if (player.height < 1.0F) throw new IllegalStateException("the player died earlier in this test and is still the size of a dead one, so nothing after can be read");
		player.setPosition(x + 0.5, y + dy + player.standingEyeHeight, z + 0.5);
		player.velocityX = player.velocityY = player.velocityZ = 0;
		health = player.health;
	}

	private double feet() {
		return player.boundingBox.minY;
	}

	private boolean settled() {
		return player.onGround || player.checkWaterCollisions() && Math.abs(player.velocityY) < 0.2;
	}

	private void fallInto(String what, int height) {
		then(() -> {
			player.health = 20;
			place(LIFT + 3 + height);
		});
		wait(2);
		until(what + ": the player comes down", 400, () -> feet() < y + LIFT + 3 && settled());
		wait(10);
		then(() -> {
			System.out.println("[buried-towers-selftest] " + what + ": health " + health + " to " + player.health);
			check(what + ": no harm (" + health + " to " + player.health + ")", player.health == health);
		});
	}

	private void physics() {
		// A fall into two of water, from each height a tower has.
		then(this::wipe);
		then(() -> pool(1));
		wait(5);
		unfrozen(1);
		fallInto("a fall of 6", 6);
		fallInto("a fall of 16", 16);
		fallInto("a fall of 40", 40);

		// A pool with a cell of gravel in the bottom layer of it: what a dug plug leaves. One deep now.
		then(() -> {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) quiet(dx, LIFT, dz, Blocks.GRAVEL, 0);
			}
		});
		wait(5);
		then(() -> {
			player.health = 20;
			place(LIFT + 3 + 16);
		});
		wait(2);
		until("a fall into one of water lands", 400, () -> player.onGround);
		wait(10);
		then(() -> {
			System.out.println("[buried-towers-selftest] a fall of 16 into one deep: health " + health + " to " + player.health);
			check("one of water doesn't save a long fall, so pools are two deep and three under gravel", player.health < health);
		});

		// Falls onto stone, to read the numbers: three is free, the wall climb's first drop.
		then(this::wipe);
		then(() -> quiet(0, LIFT, 0, Blocks.STONE, 0));
		for (int height : new int[] {3, 4, 6}) {
			then(() -> {
				player.health = 20;
				place(LIFT + 1 + height);
			});
			wait(2);
			until("a drop of " + height + " lands", 200, () -> player.onGround);
			wait(4);
			then(() -> {
				System.out.println("[buried-towers-selftest] a drop of " + height + " onto stone: health " + health + " to " + player.health);
				check("a drop of " + height + " takes " + Math.max(0, height - 3), health - player.health == Math.max(0, height - 3));
			});
		}

		// Webs, for the record: what beta does with a fall through them.
		then(this::wipe);
		then(() -> {
			quiet(0, LIFT - 1, 0, Blocks.STONE, 0);
			quiet(0, LIFT, 0, Blocks.WEB, 0);
			quiet(0, LIFT + 1, 0, Blocks.WEB, 0);
		});
		then(() -> {
			player.health = 20;
			place(LIFT + 13);
		});
		wait(2);
		until("the fall through webs ends", 800, () -> player.onGround);
		wait(4);
		then(() -> {
			System.out.println("[buried-towers-selftest] a fall of 13 through two webs: health " + health + " to " + player.health);
			check("webs don't break a fall: it costs (" + health + " to " + player.health + ")", player.health < health);
		});

		// A gravel plug over a shaft and a pool: floating, until a dig tells it.
		then(this::wipe);
		then(() -> {
			pool(2);
			// Three deep: one more course of water and rim, as the plug's own pool has.
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) quiet(dx, LIFT + 2, dz, Math.max(Math.abs(dx), Math.abs(dz)) == 3 ? Blocks.GLOWSTONE : Blocks.WATER, 0);
			}
			for (int dx = -1; dx <= 0; dx++) {
				for (int dz = -1; dz <= 0; dz++) {
					quiet(dx, LIFT + 17, dz, Blocks.STONE, 0);
					quiet(dx, LIFT + 18, dz, Blocks.GRAVEL, 0);
				}
			}
			for (int dx = -2; dx <= 1; dx++) {
				for (int dz = -2; dz <= 1; dz++) if (world.getBlockId(x + dx, y + LIFT + 18, z + dz) == 0) quiet(dx, LIFT + 18, dz, Blocks.STONE, 0);
			}
		});
		unfrozen(2);
		// A generated plug has no tick waiting, and placing one here does: it's let run out on its support, then the support goes quietly.
		wait(120);
		then(() -> {
			for (int dx = -1; dx <= 0; dx++) {
				for (int dz = -1; dz <= 0; dz++) quiet(dx, LIFT + 17, dz, 0, 0);
			}
		});
		wait(10);
		then(() -> {
			check("a plug floats until it's told", world.getBlockId(x, y + LIFT + 18, z) == Block.GRAVEL.id || world.getBlockId(x - 1, y + LIFT + 18, z - 1) == Block.GRAVEL.id);
			player.health = 20;
			place(LIFT + 19);
			player.setPosition(x, y + LIFT + 19 + player.standingEyeHeight, z);
		});
		wait(5);
		then(() -> world.setBlock(x, y + LIFT + 18, z, 0));
		then(() -> world.setBlock(x - 1, y + LIFT + 18, z - 1, 0));
		until("the plug's gone and the player's down", 400, () -> feet() < y + LIFT + 4 && settled());
		wait(40);
		then(() -> {
			System.out.println("[buried-towers-selftest] a dug plug: health " + health + " to " + player.health);
			check("riding a plug down does no harm (" + health + " to " + player.health + ")", player.health == health);
			int left = 0;
			for (int dx = -1; dx <= 0; dx++) {
				for (int dz = -1; dz <= 0; dz++) if (world.getBlockId(x + dx, y + LIFT + 18, z + dz) == Block.GRAVEL.id) left++;
			}
			check("and none of it stays up: " + left, left == 0);
		});

		// A crumbling step: loose gravel against a post, over a pool. Standing on it changes nothing; a torch put on its side drops it, by beta's own rules.
		then(this::wipe);
		then(() -> {
			pool(1);
			for (int dy = LIFT; dy <= LIFT + 10; dy++) quiet(1, dy, 0, Blocks.STONE, 0);
			// Written here it would fall at once, as it doesn't in a new chunk: it settles on a support first, which goes.
			quiet(0, LIFT + 7, 0, Blocks.STONE, 0);
			quiet(0, LIFT + 8, 0, Blocks.GRAVEL, 0);
		});
		wait(20);
		unfrozen(1);
		then(() -> quiet(0, LIFT + 7, 0, 0, 0));
		wait(10);
		then(() -> {
			player.health = 20;
			place(LIFT + 9);
		});
		wait(100);
		then(() -> {
			check("the step holds whoever stands on it", world.getBlockId(x, y + LIFT + 8, z) == Block.GRAVEL.id && feet() > y + LIFT + 8.5);
			world.setBlock(x - 1, y + LIFT + 8, z, Block.TORCH.id);
		});
		until("a torch put on it drops it", 100, () -> world.getBlockId(x, y + LIFT + 8, z) != Block.GRAVEL.id);
		until("the player's in the water", 200, () -> feet() < y + LIFT + 3 && settled());
		then(() -> check("the fall does no harm", player.health == health));

		// A sand floor's crust: sand over two of air, on stone, solid floor round it, as a chunk's written. It holds whoever stands on it; a torch put on it brings the crust down, and whoever's on it
		// drops with it, unharmed and not caught in the sand.
		then(this::wipe);
		then(() -> {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					boolean hole = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
					quiet(dx, LIFT, dz, Blocks.STONE, 0);
					for (int dy = LIFT + 1; dy <= LIFT + 2; dy++) quiet(dx, dy, dz, Blocks.STONE, 0);
					quiet(dx, LIFT + 3, dz, hole ? Blocks.SAND : Blocks.STONE, 0);
				}
			}
		});
		wait(20);
		then(() -> {
			// Settled on what was under it, which goes as a chunk would have it: with nothing told.
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					quiet(dx, LIFT + 1, dz, 0, 0);
					quiet(dx, LIFT + 2, dz, 0, 0);
				}
			}
		});
		wait(10);
		then(() -> {
			player.health = 20;
			place(LIFT + 4);
		});
		wait(100);
		then(() -> {
			check("the sand crust holds whoever stands on it", world.getBlockId(x, y + LIFT + 3, z) == Block.SAND.id && feet() > y + LIFT + 3.5);
			world.setBlock(x + 1, y + LIFT + 4, z, Block.TORCH.id);
		});
		until("a torch put on the crust brings it down", 100, () -> world.getBlockId(x, y + LIFT + 3, z) != Block.SAND.id);
		until("the player drops with it", 200, () -> feet() < y + LIFT + 3 && settled());
		wait(40);
		then(() -> {
			System.out.println("[buried-towers-selftest] through a sand crust: health " + health + " to " + player.health + ", feet at " + (feet() - y));
			check("the drop through the crust does no harm", player.health == health);
			check("and leaves the player standing clear of the sand", !player.isInsideWall());
		});

		// A well: a source in a curb, a column falling out of it to a pool, and nothing wet round it.
		then(this::wipe);
		then(() -> {
			pool(1);
			int top = LIFT + 24;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					// Its top course glowstone too, as the pool's rim.
					quiet(dx, top, dz, Blocks.GLOWSTONE, 0);
					quiet(dx, top - 1, dz, Blocks.STONE, 0);
				}
			}
			quiet(0, top, 0, Blocks.WATER, 0);
			for (int dy = top - 1; dy >= LIFT + 2; dy--) quiet(0, dy, 0, Blocks.WATER, Blocks.FALLING);
		});
		wait(20);
		unfrozen(1);
		then(() -> {
			player.health = 20;
			player.air = 300;
			place(LIFT + 24);
		});
		until("the well takes the player down", 600, () -> feet() < y + LIFT + 3);
		wait(5);
		then(() -> {
			System.out.println("[buried-towers-selftest] a well of 22: air left " + player.air + ", health " + health + " to " + player.health);
			check("a long well is survived: air " + player.air, player.health == health && player.air > 0);
		});
		// Told it should do something, the column must stay what it is.
		then(() -> world.setBlock(x + 1, y + LIFT + 10, z, Blocks.STONE));
		then(() -> world.setBlock(x + 1, y + LIFT + 10, z, 0));
		wait(100);
		then(() -> {
			int wet = 0;
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) continue;
					for (int dy = 0; dy < 40; dy++) {
						int id = world.getBlockId(x + dx, y + dy, z + dz);
						if (id == Block.WATER.id || id == Block.FLOWING_WATER.id) wet++;
					}
				}
			}
			int spread = 0;
			for (int d = 0; d < 4; d++) {
				int id = world.getBlockId(x + Blocks.DX[d], y + LIFT + 10, z + Blocks.DZ[d]);
				if (id == Block.WATER.id || id == Block.FLOWING_WATER.id) spread++;
			}
			check("a disturbed column doesn't spread: " + spread + " beside it, " + wet + " further off", spread == 0 && wet == 0);
		});
		then(this::wipe);
	}

	/** An iron door and its lever, in the metas {@link Blocks} gives. */
	private void door() {
		then(() -> {
			int side = Blocks.NORTH;
			quiet(1, 0, 0, Blocks.STONE, 0);
			quiet(1, 1, 0, Blocks.STONE, 0);
			quiet(-1, 0, 0, Blocks.STONE, 0);
			quiet(-1, 1, 0, Blocks.STONE, 0);
			quiet(0, 2, 0, Blocks.STONE, 0);
			quiet(0, 0, 0, Blocks.IRON_DOOR, Blocks.doorOn(side));
			quiet(0, 1, 0, Blocks.IRON_DOOR, Blocks.doorOn(side) | 8);
			// On the block beside the door, facing the corridor.
			quiet(1, 1, 1, Blocks.LEVER, Blocks.leverOn(Blocks.NORTH));
		});
		wait(2);
		then(() -> {
			check("a door starts closed", (world.getBlockMeta(x, y, z) & 4) == 0);
			check("its leaf across the doorway, on the north side", leaf() == Blocks.NORTH);
			Block.BLOCKS[Blocks.LEVER].onUse(world, x + 1, y + 1, z + 1, player);
		});
		wait(5);
		then(() -> {
			check("its lever opens it", (world.getBlockMeta(x, y, z) & 4) != 0);
			int open = leaf();
			check("and the leaf swings out of the doorway: " + open, open == Blocks.EAST || open == Blocks.WEST);
		});
		then(() -> {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					for (int dy = 0; dy <= 2; dy++) quiet(dx, dy, dz, 0, 0);
				}
			}
		});
	}

	/** Every chest kind at every depth, many times over: no slot holds more than its item stacks to (food is one a slot in beta). */
	private void loot() {
		then(() -> {
			quiet(0, 0, 0, Blocks.CHEST, 0);
			ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(x, y, z);
			int filled = 0;
			String over = null;
			// What the valuables in a chest are worth on average, by kind, at each depth: a diamond eight, a gold ingot two, an iron one.
			double[][] worth = new double[Loot.KINDS.length][5];
			for (int kind = 0; kind < Loot.KINDS.length; kind++) {
				for (int k = 0; k <= 4; k++) {
					for (int seed = 0; seed < 200; seed++) {
						for (int i = 0; i < chest.size(); i++) chest.setStack(i, null);
						Loot.fill(chest, Loot.KINDS[kind], k / 4.0, new Random(seed * 31L + k));
						for (int i = 0; i < chest.size(); i++) {
							ItemStack stack = chest.getStack(i);
							if (stack == null) continue;
							filled++;
							if (stack.count > stack.getMaxCount() || stack.count < 1) over = Loot.KINDS[kind] + ": " + stack.count + " of " + stack.itemId;
							int each = stack.itemId == Item.DIAMOND.id ? 8 : stack.itemId == Item.GOLD_INGOT.id ? 2 : stack.itemId == Item.IRON_INGOT.id ? 1 : 0;
							worth[kind][k] += each * stack.count / 200.0;
						}
					}
				}
			}
			check("no chest slot holds more than its item stacks to, over " + filled + " filled: " + (over == null ? "none" : over), over == null && filled > 0);
			// A hidden room's chest rewards noticing it: better than any floor's chest at its depth, from halfway down, and short of the vault's best (which has three more beside it).
			int hidden = Arrays.asList(Loot.KINDS).indexOf("hidden"), best = Arrays.asList(Loot.KINDS).indexOf("vault-best");
			String order = "";
			boolean ordered = true;
			for (int k = 2; k <= 4; k++) {
				double floors = 0;
				for (int kind = 0; kind < Loot.KINDS.length; kind++) {
					if (!Loot.KINDS[kind].startsWith("vault") && kind != hidden) floors = Math.max(floors, worth[kind][k]);
				}
				ordered &= floors < worth[hidden][k] && worth[hidden][k] < worth[best][k];
				order += String.format(" %.2f: floors' best %.1f, hidden %.1f, vault's best %.1f;", k / 4.0, floors, worth[hidden][k], worth[best][k]);
			}
			check("a hidden room's chest is worth more than a floor's and less than the vault's best:" + order, ordered);
			for (int i = 0; i < chest.size(); i++) chest.setStack(i, null);
			quiet(0, 0, 0, 0, 0);
		});
	}

	/** Which side of its cell the door at the test's middle stands on, as beta draws and collides it. */
	private int leaf() {
		Block door = Block.BLOCKS[Blocks.IRON_DOOR];
		door.updateBoundingBox(world, x, y, z);
		if (door.maxZ - door.minZ < 0.5) return door.minZ < 0.5 ? Blocks.NORTH : Blocks.SOUTH;
		return door.minX < 0.5 ? Blocks.WEST : Blocks.EAST;
	}

	/** Beta's own dungeon in solid rock, as generation would place one, and the veto. */
	private void veto() {
		then(() -> {
			int floor = y + 20;
			for (int dx = -7; dx <= 7; dx++) {
				for (int dz = -7; dz <= 7; dz++) {
					for (int dy = 18; dy <= 28; dy++) quiet(dx, dy, dz, Blocks.STONE, 0);
				}
			}
			// One open cell in the wall: all beta wants to call it a dungeon.
			quiet(4, 20, 0, 0, 0);
			quiet(4, 21, 0, 0, 0);
			boolean placed = new DungeonFeature().generate(world, wide(), x, floor, z);
			check("the generator still goes through a dungeon", placed);
			check("and no spawner comes of it", world.getBlockId(x, floor, z) == Block.STONE.id);
			check("nor a chest", !(world.getBlockEntity(x + 1, floor, z) instanceof ChestBlockEntity));
			boolean rock = true;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) rock &= world.getBlockId(x + dx, floor, z + dz) == Block.STONE.id;
			}
			check("and every cell is as it was", rock);
		});
		then(this::wipe);
	}

	private static Random wide() {
		for (long seed = 0; ; seed++) {
			Random probe = new Random(seed);
			if (probe.nextInt(2) == 1 && probe.nextInt(2) == 1) return new Random(seed);
		}
	}

	@Override
	protected void script() {
		if (restarted()) {
			check("the query still answers", Towers.nearest(world, x, z) != null);
			return;
		}
		Towers.Found found = Towers.nearest(world, x, z);
		check("a tower is found near the world's start", found != null);
		if (found != null) {
			check("it has floors, each with a place to stand: " + found.floors.size(), found.floors.size() >= 4);
			check("and the last has no way down", found.floors.get(found.floors.size() - 1).wayDown == null);
		}
		veto();
		door();
		loot();
		if (singlePlayer()) physics();
		if (FabricLoader.getInstance().isModLoaded("stagehand")) stagehand();
	}

	/** Stagehand's real folder, as a session uses it: there's no class of its here, as a tower needs none. */
	private void stagehand() {
		Path folder = FabricLoader.getInstance().getGameDir().resolve("stagehand");
		Path result = folder.resolve("out/towers-test.json");
		then(() -> {
			try {
				Files.createDirectories(folder.resolve("in"));
				Files.deleteIfExists(result);
				Path temp = folder.resolve("in/towers-test.tmp");
				String script = "locate tower\ntp tower nearest floor=2\nstatus\ntp tower nearest\nstatus\ntp " + x + " " + y + " " + z + "\n";
				Files.write(temp, script.getBytes(StandardCharsets.UTF_8));
				Files.move(temp, folder.resolve("in/towers-test.txt"), StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		until("Stagehand answers", 900, () -> read(result).contains("\"state\": \"done\"") || read(result).contains("\"state\": \"failed\""));
		then(() -> {
			String text = read(result);
			System.out.println("[buried-towers-selftest] " + text.replace('\n', ' ').replace("  ", ""));
			check("Stagehand finds a tower and takes the player to its floor and its gate", text.contains("\"state\": \"done\"") && text.contains("\"floors\"") && text.contains("\"way\""));
			// Its own test wants a game where nobody's used it yet.
			try (Stream<Path> walk = Files.walk(folder)) {
				walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private static String read(Path file) {
		try {
			return Files.isRegularFile(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
		} catch (IOException e) {
			return "";
		}
	}
}
