package org.studioquire.faithful.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import org.studioquire.faithful.harness.mixin.EntityRegistryAccessor;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * One of everything, in the sky over the spawn: every mod block at a few metas, every mod item in chests and on the floor, every mod entity. Laid out by name,
 * so the layout doesn't shift with the numbering, and read back by name. The bounds go in the world's folder, so a copy of the world still knows them.
 */
final class Arena {
	private static final int Y = 100, WIDTH = 32, STEP = 2;
	private static final int[] METAS = {0, 1, 7, 15};
	private static final String BOUNDS = "harness-arena.txt";

	private final ServerWorld world;
	private final int x0, z0;
	private int cell;

	Arena(ServerWorld world) {
		this.world = world;
		Vec3i spawn = world.getSpawnPos();
		x0 = spawn.x;
		z0 = spawn.z;
	}

	private int x(int n) {
		return x0 + n % WIDTH * STEP;
	}

	private int z(int n) {
		return z0 + n / WIDTH * STEP;
	}

	private static List<String> sorted(List<String> names) {
		Collections.sort(names);
		return names;
	}

	static List<String> blockNames() {
		List<String> names = new ArrayList<>();
		for (int id = ContentRegistry.FIRST_BLOCK; id < ContentRegistry.BLOCK_LIMIT; id++) {
			if (ContentRegistry.blockName(id) != null) names.add(ContentRegistry.blockName(id));
		}
		return sorted(names);
	}

	static List<String> itemNames() {
		List<String> names = new ArrayList<>();
		for (int id = ContentRegistry.FIRST_BLOCK; id < Item.ITEMS.length; id++) {
			if (ContentRegistry.itemName(id) != null) names.add(ContentRegistry.itemName(id));
		}
		return sorted(names);
	}

	static List<String> entityNames() {
		List<String> names = new ArrayList<>();
		for (String name : EntityRegistryAccessor.ids().keySet()) {
			if (name.indexOf(':') > 0) names.add(name);
		}
		return sorted(names);
	}

	private void floor(int n) {
		for (int dx = 0; dx < STEP; dx++) {
			for (int dz = 0; dz < STEP; dz++) world.setBlock(x(n) + dx, Y - 1, z(n) + dz, Block.STONE.id);
		}
	}

	/** Under a plant, what it can stand on: on stone it's gone at its first random tick, which says nothing about the mod. */
	private void soil(Block block, int n) {
		for (Block under : new Block[] {Block.STONE, Block.DIRT, Block.FARMLAND, Block.SAND}) {
			world.setBlock(x(n), Y - 1, z(n), under.id);
			if (block.canPlaceAt(world, x(n), Y, z(n))) return;
		}
		world.setBlock(x(n), Y - 1, z(n), Block.STONE.id);
	}

	void place() throws IOException {
		List<String> blocks = blockNames(), items = itemNames(), entities = entityNames();
		for (String name : blocks) {
			int id = ContentRegistry.blockId(name);
			for (int meta : METAS) {
				floor(cell);
				try {
					soil(Block.BLOCKS[id], cell);
					world.setBlock(x(cell), Y, z(cell), id, meta);
				} catch (RuntimeException e) {
					Out.fail(Owners.namespace(name), "place-threw", name + " at meta " + meta + ": " + e);
				}
				cell++;
			}
		}
		ChestBlockEntity chest = null;
		int slot = 27;
		for (String name : items) {
			if (slot == 27) {
				floor(cell);
				world.setBlock(x(cell), Y, z(cell), Block.CHEST.id);
				chest = (ChestBlockEntity) world.getBlockEntity(x(cell), Y, z(cell));
				cell++;
				slot = 0;
			}
			chest.setStack(slot++, stack(name));
		}
		for (String name : items) {
			floor(cell);
			world.spawnEntity(new ItemEntity(world, x(cell) + 0.5, Y + 0.2, z(cell) + 0.5, stack(name)));
			cell++;
		}
		for (String name : entities) {
			floor(cell);
			Entity e = EntityRegistry.create(name, world);
			if (e == null) {
				Out.fail(Owners.namespace(name), "entity-create", name + " can't be made from its name");
			} else {
				e.setPositionAndAnglesKeepPrevAngles(x(cell) + 0.5, Y, z(cell) + 0.5, 0, 0);
				world.spawnEntity(e);
			}
			cell++;
		}
		Files.write(boundsFile(), Collections.singletonList(String.valueOf(cell)));
		Out.info("arena: " + blocks.size() + " blocks, " + items.size() + " items, " + entities.size() + " entities in " + cell + " cells");
	}

	private static Path boundsFile() {
		return Paths.get("world", BOUNDS);
	}

	private static ItemStack stack(String name) {
		int id = ContentRegistry.itemId(name);
		Item item = Item.ITEMS[id];
		ItemStack s = new ItemStack(id, 1, item.getMaxDamage() > 0 ? 1 : 0);
		Stacks.data(s).putString("harness", name);
		return s;
	}

	/** One of each of the first items into a player's inventory, so a save of a player has them too. */
	void give(PlayerEntity player) {
		int slot = 9;
		for (String name : itemNames()) {
			if (slot >= player.inventory.main.length) break;
			player.inventory.main[slot++] = stack(name);
		}
	}

	/** Loads the arena's chunks. */
	void touch() throws IOException {
		int cells = Integer.parseInt(Files.readAllLines(boundsFile()).get(0).trim());
		for (int n = 0; n < cells; n++) world.getBlockId(x(n), Y, z(n));
	}

	/** Everything in the arena, by name, a line each, sorted: text a task can compare across runs. */
	void snapshot(Path file) throws IOException {
		int cells = Integer.parseInt(Files.readAllLines(boundsFile()).get(0).trim());
		List<String> lines = new ArrayList<>();
		for (int n = 0; n < cells; n++) {
			int x = x(n), z = z(n);
			int id = world.getBlockId(x, Y, z);
			if (id != 0) lines.add("cell\t" + n + "\t" + ModIds.block(id) + "\t" + world.getBlockMeta(x, Y, z));
			BlockEntity be = world.getBlockEntity(x, Y, z);
			if (be != null) {
				NbtCompound nbt = new NbtCompound();
				be.writeNbt(nbt);
				for (String coordinate : new String[] {"x", "y", "z"}) nbt.putInt(coordinate, 0);
				lines.add("entity-block\t" + n + "\t" + ModIds.canon(nbt));
				if (nbt.getString("id").indexOf(':') > 0) numbers(nbt.getString("id"), nbt, "", lines);
				else scanFaithful(nbt, "", lines);
			}
		}
		Map<String, Integer> kinds = new TreeMap<>();
		for (Object o : world.entities) {
			Entity e = (Entity) o;
			if (e instanceof PlayerEntity) continue;
			if (e instanceof ItemEntity) {
				if (inside(e, cells)) {
					NbtCompound nbt = new NbtCompound();
					((ItemEntity) e).stack.writeNbt(nbt);
					kinds.merge("item\t" + ModIds.canon(nbt), 1, Integer::sum);
					scanFaithful(nbt, "", lines);
				}
			} else {
				String name = EntityRegistry.getId(e);
				if (name != null && name.indexOf(':') > 0) {
					kinds.merge("entity\t" + name, 1, Integer::sum);
					NbtCompound nbt = new NbtCompound();
					e.write(nbt);
					numbers(name, nbt, "", lines);
				}
			}
		}
		kinds.forEach((k, v) -> lines.add(k + "\t" + v));
		for (PlayerEntity p : org.studioquire.oldfaithful.core.api.Server.players()) {
			for (int i = 0; i < p.inventory.main.length; i++) {
				ItemStack s = p.inventory.main[i];
				if (s == null) continue;
				NbtCompound nbt = new NbtCompound();
				s.writeNbt(nbt);
				lines.add("player\t" + p.name + "\t" + i + "\t" + ModIds.canon(nbt));
			}
		}
		Collections.sort(lines);
		Files.write(file, lines);
	}

	private boolean inside(Entity e, int cells) {
		return e.y >= Y - 1 && e.y < Y + 8 && e.x >= x0 - 2 && e.x < x0 + WIDTH * STEP + 2 && e.z >= z0 - 2 && e.z < z(cells) + 4;
	}

	private static void scanFaithful(NbtCompound nbt, String path, List<String> lines) {
		for (Object o : nbt.values()) {
			NbtElement e = (NbtElement) o;
			String here = path + "/" + e.getKey();
			if (e instanceof NbtCompound) {
				if ("Faithful".equals(e.getKey())) numbers("Faithful", (NbtCompound) e, here, lines);
				else scanFaithful((NbtCompound) e, here, lines);
			} else if (e instanceof NbtList) {
				NbtList list = (NbtList) e;
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) instanceof NbtCompound) scanFaithful((NbtCompound) list.get(i), here + "[" + i + "]", lines);
				}
			}
		}
	}

	/** Every whole number under a compound a mod wrote, for the task to weigh against the numbering: a mod that saved a block's number. */
	private static void numbers(String owner, NbtElement element, String path, List<String> lines) {
		if (element instanceof NbtCompound) {
			for (Object o : ((NbtCompound) element).values()) {
				NbtElement e = (NbtElement) o;
				if (path.isEmpty() && e.getKey().matches("[xyz]")) continue;
				numbers(owner, e, path + "/" + e.getKey(), lines);
			}
		} else if (element instanceof NbtList) {
			NbtList list = (NbtList) element;
			for (int i = 0; i < list.size(); i++) numbers(owner, list.get(i), path + "[" + i + "]", lines);
		} else if (element.getType() >= 2 && element.getType() <= 4) {
			long value = Long.parseLong(element.toString());
			if (value >= ContentRegistry.FIRST_BLOCK) lines.add("number\t" + owner + "\t" + path + "\t" + value);
		}
	}
}
