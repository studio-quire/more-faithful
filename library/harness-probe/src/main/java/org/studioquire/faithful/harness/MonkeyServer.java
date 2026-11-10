package org.studioquire.faithful.harness;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Channel;

/**
 * The server's end of the monkey: it sets the bench, and says how things stand from here. The client does all the acting, through the game's own paths.
 * Only ever runs on a test server: this is {@code /give} and more for anyone with the probe.
 */
public final class MonkeyServer {
	public static final int OP_SETUP = 1, OP_STATE = 2, ANSWER_READY = 1, ANSWER_STATE = 2;
	/** Bench cells around the player: two apart, so a neighbour doesn't confuse which block did what. */
	public static final int[][] CELLS = cells();

	private static int benchX, benchY = -1, benchZ;
	private static Channel channel;

	private MonkeyServer() {
	}

	private static int[][] cells() {
		java.util.List<int[]> cells = new ArrayList<>();
		for (int dx = -4; dx <= 4; dx += 2) {
			for (int dz = -4; dz <= 4; dz += 2) {
				if (dx != 0 || dz != 0) cells.add(new int[] {dx, dz});
			}
		}
		return cells.toArray(new int[0][]);
	}

	static void register(Channel state) {
		channel = state;
		state.onServer(MonkeyServer::receive);
	}

	private static void receive(PlayerEntity player, DataInputStream in) throws IOException {
		if (!Boolean.getBoolean("faithful.harness.monkey")) return;
		int op = in.readUnsignedByte();
		if (op == OP_SETUP) {
			int item = in.readInt(), count = in.readUnsignedByte(), blocks = in.readUnsignedByte();
			int[] ids = new int[blocks];
			for (int i = 0; i < blocks; i++) ids[i] = in.readInt();
			setUp((ServerPlayerEntity) player, item, count, ids);
			channel.toPlayer(player, out -> {
				out.writeByte(ANSWER_READY);
				out.writeInt(benchX);
				out.writeInt(benchY);
				out.writeInt(benchZ);
			});
		} else if (op == OP_STATE) {
			MonkeyState state = MonkeyState.of(player.world, player, benchX, benchY, benchZ);
			channel.toPlayer(player, out -> {
				out.writeByte(ANSWER_STATE);
				state.write(out);
			});
		}
	}

	/** Round the bench, once: a server's spawn can be on a lake, whose water would go on flooding it. */
	private static void wall(World world) {
		int r = MonkeyState.RADIUS + 2;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
				for (int dy = -1; dy <= MonkeyState.HEIGHT + 1; dy++) world.setBlock(benchX + dx, benchY + dy, benchZ + dz, Block.STONE.id);
			}
		}
	}

	private static void setUp(ServerPlayerEntity player, int item, int count, int[] ids) {
		World world = player.world;
		if (benchY < 0) {
			benchX = (int) Math.floor(player.x);
			benchY = (int) Math.floor(player.boundingBox.minY + 0.01);
			benchZ = (int) Math.floor(player.z);
			wall(world);
		}
		int r = MonkeyState.RADIUS;
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				for (int dy = MonkeyState.HEIGHT + 1; dy >= 0; dy--) world.setBlock(benchX + dx, benchY + dy, benchZ + dz, 0);
				world.setBlock(benchX + dx, benchY - 1, benchZ + dz, Block.STONE.id);
			}
		}
		for (Object o : new ArrayList<Object>(world.entities)) {
			Entity e = (Entity) o;
			if (!(e instanceof PlayerEntity) && Math.abs(e.x - benchX) < 16 && Math.abs(e.z - benchZ) < 16 && Math.abs(e.y - benchY) < 16) e.markDead();
		}
		for (int i = 0; i < ids.length && i < CELLS.length; i++) {
			world.setBlock(benchX + CELLS[i][0], benchY, benchZ + CELLS[i][1], ids[i]);
		}
		ItemStack[] main = player.inventory.main;
		java.util.Arrays.fill(main, null);
		player.inventory.setCursorStack(null);
		if (item > 0) main[0] = new ItemStack(item, count, 0);
		main[9] = new ItemStack(Block.COBBLESTONE, 32);
		main[10] = new ItemStack(Block.PLANKS, 16);
		main[11] = new ItemStack(Item.STICK, 16);
		main[12] = new ItemStack(Item.COAL, 8);
		main[13] = new ItemStack(Item.DIAMOND_PICKAXE);
		player.health = 20;
		player.fireTicks = 0;
		player.currentScreenHandler.sendContentUpdates();
	}
}
