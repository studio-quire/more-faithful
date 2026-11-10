package org.studioquire.faithful.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import org.studioquire.faithful.harness.mixin.EntityAccessor;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * What a fixed list of acts comes to, a line each, with the world's and the entity's random seeded for each act: every block broken by hand and with a pick,
 * every mod item used on stone, every kind of mob killed. The same list under an order salt shows what leans on the order listeners run in. Explosions aren't
 * here: their rays have a random of their own, which no seed reaches.
 */
final class Outcomes {
	private static final int Y = 100, OFFSET = -24;
	private static final String[] MOBS = {"Pig", "Sheep", "Cow", "Chicken", "Zombie", "Skeleton", "Spider", "Creeper"};

	private final ServerWorld world;
	private final ServerPlayerEntity player;
	private final int x, z;
	private final List<String> lines = new ArrayList<>();

	Outcomes(ServerWorld world, ServerPlayerEntity player) {
		this.world = world;
		this.player = player;
		Vec3i spawn = world.getSpawnPos();
		x = spawn.x + OFFSET;
		z = spawn.z + OFFSET;
	}

	private void hold(ItemStack stack) {
		player.inventory.selectedSlot = 0;
		player.inventory.main[0] = stack;
	}

	private void seed(String what) {
		world.random.setSeed(what.hashCode());
	}

	/** The drops near the bench, by name, sorted, and gone. */
	private String drops() {
		List<String> found = new ArrayList<>();
		for (Object o : new ArrayList<Object>(world.entities)) {
			if (!(o instanceof ItemEntity)) continue;
			ItemEntity item = (ItemEntity) o;
			if (Math.abs(item.x - x) > 12 || Math.abs(item.z - z) > 12 || Math.abs(item.y - Y) > 12) continue;
			found.add(ModIds.item(item.stack.itemId) + "x" + item.stack.count + ":" + item.stack.getDamage());
			item.markDead();
		}
		Collections.sort(found);
		return found.toString();
	}

	private void bench() {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				world.setBlock(x + dx, Y - 1, z + dz, Block.STONE.id);
				for (int dy = 0; dy < 3; dy++) world.setBlock(x + dx, Y + dy, z + dz, 0);
			}
		}
		drops();
	}

	private void breakBlock(int id, String tool, ItemStack held) {
		String name = ModIds.block(id);
		bench();
		hold(held);
		world.setBlock(x, Y, z, id);
		if (world.getBlockId(x, Y, z) != id) return;
		seed(name);
		boolean broke = player.interactionManager.tryBreakBlock(x, Y, z);
		lines.add("break\t" + name + "\t" + tool + "\t" + broke + "\t" + ModIds.block(world.getBlockId(x, Y, z)) + "\t" + drops());
	}

	void run(Path file) throws IOException {
		List<Integer> ids = new ArrayList<>();
		for (int id = 1; id <= 96; id++) {
			if (Block.BLOCKS[id] != null) ids.add(id);
		}
		for (String name : Arena.blockNames()) ids.add(ContentRegistry.blockId(name));
		for (int id : ids) {
			breakBlock(id, "hand", null);
			breakBlock(id, "pick", new ItemStack(Item.DIAMOND_PICKAXE));
		}
		for (String name : Arena.itemNames()) {
			bench();
			ItemStack stack = new ItemStack(ContentRegistry.itemId(name), 1, 0);
			hold(stack);
			world.setBlock(x, Y, z, Block.STONE.id);
			seed(name);
			boolean used = player.interactionManager.interactBlock(player, world, stack, x, Y, z, 1);
			lines.add("use\t" + name + "\t" + used + "\t" + ModIds.block(world.getBlockId(x, Y, z)) + "\t" + ModIds.block(world.getBlockId(x, Y + 1, z)) + "\t" + drops());
		}
		List<String> mobs = new ArrayList<>(Arena.entityNames());
		Collections.addAll(mobs, MOBS);
		for (String name : mobs) {
			bench();
			Entity e = EntityRegistry.create(name, world);
			if (!(e instanceof LivingEntity)) continue;
			e.setPositionAndAnglesKeepPrevAngles(x + 0.5, Y, z + 0.5, 0, 0);
			world.spawnEntity(e);
			((EntityAccessor) e).random().setSeed(name.hashCode());
			seed(name);
			e.damage(null, 1000);
			e.markDead();
			lines.add("kill\t" + name + "\t" + drops());
		}
		hold(null);
		Files.write(file, lines);
	}
}
