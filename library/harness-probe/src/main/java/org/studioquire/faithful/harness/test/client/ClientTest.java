package org.studioquire.faithful.harness.test.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.harness.test.Checks;
import org.studioquire.faithful.harness.test.Script;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.oldfaithful.selftest.ScreenCapture;
import org.studioquire.oldfaithful.selftest.SelfTest;
import org.studioquire.oldfaithful.selftest.WorldCheck;

/**
 * A mod's test on the client, in single player and joined to a real server, fresh and after a restart: entrypoint {@code faithful:test-client}. It runs after the mod's {@code WorldTest},
 * with the player still on its plot. What the client can only ask of whoever runs the world goes through {@link #scene}.
 */
public abstract class ClientTest extends Script implements WorldCheck {
	private static final int GROUND_RISE = 12, RISE_A_TICK = 6, GROUND_REACH = 2, HOVER = 10, SEARCH = 8, SERVER_WORD = 40, LIMIT = 100;

	protected Minecraft mc;
	/** Where {@link #ownGround} stood the player: the middle of its floor, the cell its feet are in. */
	protected int groundX, groundY, groundZ;

	@Override
	public final void start(Minecraft mc, SelfTest results) {
		this.mc = mc;
		begin(Checks.of(results.owner()));
	}

	@Override
	public final boolean tick(Minecraft mc) {
		return advance();
	}

	@Override
	public final boolean afterRestart() {
		return true;
	}

	/** On a real server, rather than single player. */
	protected final boolean remote() {
		return mc.world.isRemote;
	}

	protected final boolean restarted() {
		return TestHost.restarted();
	}

	protected final PlayerEntity player() {
		return mc.player;
	}

	/** A step: runs the mod's {@code WorldTest} scene of this name where the world runs, and waits for its steps to end. */
	protected final void scene(String name, Object... args) {
		int[] asked = {0};
		then(() -> {
			asked[0] = TestHost.answers + 1;
			TestHost.channel.toServer(out -> {
				out.writeByte(TestHost.OP_SCENE);
				out.writeUTF(checks().mod());
				out.writeUTF(name);
				out.writeByte(args.length);
				for (Object arg : args) out.writeUTF(String.valueOf(arg));
			});
		});
		await("the scene " + name + " to end", 3000, () -> TestHost.answers >= asked[0]);
	}

	/** The first hotbar slot holding this item, or -1. */
	protected final int hotbarSlot(int id) {
		for (int i = 0; i < 9; i++) {
			ItemStack s = mc.player.inventory.main[i];
			if (s != null && s.itemId == id) return i;
		}
		return -1;
	}

	/** Nothing there a player would bump into: air, water, flowers. */
	private boolean passable(int x, int y, int z) {
		return y > 127 || y >= 0 && !mc.world.getMaterial(x, y, z).blocksMovement();
	}

	/** Room for the floor, and the column from the player's feet up past it, as far as the test needs above the floor. */
	private boolean open(int x, int feet, int z, int headroom) {
		for (int y = feet; y < groundY + headroom; y++) if (!passable(x, y, z)) return false;
		for (int dx = -GROUND_REACH; dx <= GROUND_REACH; dx++) {
			for (int dz = -GROUND_REACH; dz <= GROUND_REACH; dz++) {
				for (int y = groundY - 1; y < groundY + 2; y++) if (!passable(x + dx, y, z + dz)) return false;
			}
		}
		return true;
	}

	/** The way, a cell at a time at the player's own height, to the nearest column it can rise in; null if there's none near. */
	private List<int[]> wayUp(int x, int feet, int z, int headroom) {
		Map<Long, int[]> from = new HashMap<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[] {x, z});
		from.put(key(x, z), null);
		while (!queue.isEmpty()) {
			int[] at = queue.poll();
			if (open(at[0], feet, at[1], headroom)) {
				LinkedList<int[]> way = new LinkedList<>();
				for (int[] c = at; c != null; c = from.get(key(c[0], c[1]))) way.addFirst(c);
				return way;
			}
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = at[0] + d[0], nz = at[1] + d[1];
				if (Math.abs(nx - x) > SEARCH || Math.abs(nz - z) > SEARCH || from.containsKey(key(nx, nz)) || !passable(nx, feet, nz) || !passable(nx, feet + 1, nz)) continue;
				from.put(key(nx, nz), at);
				queue.add(new int[] {nx, nz});
			}
		}
		return null;
	}

	private static long key(int x, int z) {
		return (long) x << 32 | z & 0xFFFFFFFFL;
	}

	/**
	 * Steps: ground of the test's own on a server without its mod, where there's no plot and the spawn can be in water, beside it, or under trees. Cobblestone from
	 * {@code /give}; the player led along to the nearest column with nothing over it for {@code headroom} cells above the floor, lifted up it, held there until the
	 * server has it, and a floor five cells square placed under its feet against nothing, as a player can, then stood on.
	 */
	protected final void ownGround(int headroom) {
		int side = 2 * GROUND_REACH + 1;
		then(() -> {
			// A gift is thrown the way the player looks: at their feet, it's picked up.
			mc.player.pitch = 90.0F;
			mc.player.sendChatMessage("/give " + mc.player.name + " " + Block.COBBLESTONE.id + " " + (side * side + 7));
		});
		await("cobblestone for a floor", LIMIT, () -> hotbarSlot(Block.COBBLESTONE.id) >= 0 && mc.player.inventory.main[hotbarSlot(Block.COBBLESTONE.id)].count > side * side);
		List<int[]> way = new ArrayList<>();
		int[] feet = {0};
		then(() -> {
			mc.player.inventory.selectedSlot = hotbarSlot(Block.COBBLESTONE.id);
			feet[0] = MathHelper.floor(mc.player.boundingBox.minY);
			groundY = feet[0] + GROUND_RISE;
			List<int[]> found = wayUp(MathHelper.floor(mc.player.x), feet[0], MathHelper.floor(mc.player.z), headroom);
			if (check("open sky within " + SEARCH + " cells of the spawn at " + (int) mc.player.x + ", " + feet[0] + ", " + (int) mc.player.z, found != null)) way.addAll(found);
		});
		// A cell a tick, at the height it stands, held up over any drop on the way.
		await("the player led to it", LIMIT, () -> {
			if (way.isEmpty()) return true;
			int[] next = way.remove(0);
			groundX = next[0];
			groundZ = next[1];
			mc.player.setPosition(groundX + 0.5, mc.player.y + feet[0] - mc.player.boundingBox.minY, groundZ + 0.5);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			return way.isEmpty();
		});
		// A step at a time, as a server takes a move of ten blocks for a cheat, and then held: placed before the server has the player there, the floor would be in its way.
		int[] held = {0};
		await("the player lifted clear", LIMIT, () -> {
			double up = groundY - mc.player.boundingBox.minY;
			mc.player.setPosition(groundX + 0.5, mc.player.y + Math.min(up, RISE_A_TICK), groundZ + 0.5);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			return up <= RISE_A_TICK && ++held[0] >= HOVER;
		});
		then(() -> {
			for (int dx = -GROUND_REACH; dx <= GROUND_REACH; dx++) {
				for (int dz = -GROUND_REACH; dz <= GROUND_REACH; dz++) {
					mc.interactionManager.interactBlock(mc.player, mc.world, mc.player.inventory.getSelectedItem(), groundX + dx, groundY - 2, groundZ + dz, 1);
				}
			}
		});
		// Long enough for the server's word on each cell, from a server that may still be busy with its first chunks: a refused one comes back as air.
		wait(SERVER_WORD);
		await("a floor of its own under the player", LIMIT, () -> {
			for (int dx = -GROUND_REACH; dx <= GROUND_REACH; dx++) {
				for (int dz = -GROUND_REACH; dz <= GROUND_REACH; dz++) {
					if (mc.world.getBlockId(groundX + dx, groundY - 1, groundZ + dz) != Block.COBBLESTONE.id) return false;
				}
			}
			return mc.player.onGround;
		});
	}

	/** A step: a picture of what's in the window now, world or screen, as {@code shots/<mod>/<name>.png} in the game's folder. */
	protected final void picture(String name) {
		ScreenCapture[] shot = {null};
		then(() -> shot[0] = ScreenCapture.request(new File("shots/" + checks().mod() + "/" + name + ".png"), -1, -1));
		await("the picture " + name, 100, () -> shot[0].done());
	}
}
