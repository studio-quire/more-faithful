package org.studioquire.faithful.golems.selftest;

import java.io.PrintWriter;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.Area;
import org.studioquire.faithful.golems.GolemEntity;

/** For a long run's report: each thing left lying round a golem's area, where it is against the area and what it's in and on, to tell what the golem missed from what it never would take. */
final class Lying {
	private Lying() {
	}

	static int report(PrintWriter w, World world, GolemEntity g, int margin) {
		int[] left = g.leftLying;
		w.printf("its gathers left %d with nowhere in touch to stand, %d with no way there, %d gone out of touch, %d not got into in its steps; %d ran out of time with something left%n", left[0],
				left[1], left[2], left[3], left[4]);
		Area area = g.area();
		int n = 0;
		StringBuilder each = new StringBuilder();
		Box box = Box.createCached(area.minX - margin, 0, area.minZ - margin, area.maxX + 1 + margin, 128, area.maxZ + 1 + margin);
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, box)) {
			ItemEntity item = (ItemEntity) o;
			if (item.dead || item.stack.count <= 0) continue;
			int x = MathHelper.floor(item.x), y = MathHelper.floor(item.boundingBox.minY + 0.05), z = MathHelper.floor(item.z);
			int out = Math.max(Math.max(area.minX - x, x - area.maxX), Math.max(area.minZ - z, z - area.maxZ));
			each.append(String.format("  %s x%d at %d %d %d, %s, in %s, on %s, age %d%n", item.stack.getItem().getTranslationKey(), item.stack.count, x, y, z, out <= 0 ? "inside" : out + " out",
					name(world.getBlockId(x, y, z)), name(world.getBlockId(x, y - 1, z)), item.itemAge));
			n += item.stack.count;
		}
		w.println("left lying round its area: " + n + ", carrying " + g.carriedCount());
		w.print(each);
		return n;
	}

	/**
	 * What lies round a golem in a long run, which ticks the golem many times a game tick: the wait before a thing can be picked up counts down at the golem's pace too, called every
	 * {@code ticks} of the golem's. Left at the game's pace, a block's drop could be taken only 10 game ticks after it fell, 800 of the golem's at a speed of 80, and the golem's
	 * gathers (15 s of its time) ran out waiting beside things a golem in real time would have taken at once: 483 times in one level run, which left 77 lying; paced, 2 times and none.
	 */
	static void hurry(World world, GolemEntity g, int margin, int ticks) {
		Area area = g.area();
		Box box = Box.createCached(area.minX - margin, 0, area.minZ - margin, area.maxX + 1 + margin, 128, area.maxZ + 1 + margin);
		for (Object o : world.collectEntitiesByClass(ItemEntity.class, box)) {
			ItemEntity item = (ItemEntity) o;
			item.pickupDelay = Math.max(0, item.pickupDelay - ticks);
		}
	}

	private static String name(int id) {
		return id == 0 ? "air" : Block.BLOCKS[id] == null ? "#" + id : Block.BLOCKS[id].getTranslationKey();
	}
}
