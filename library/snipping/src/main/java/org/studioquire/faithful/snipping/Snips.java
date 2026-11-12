package org.studioquire.faithful.snipping;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.snipping.mixin.PlantBlockInvoker;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Mod;

/**
 * Which plants are snipped: a mark on the cell, kept with its chunk (an attachment, saved and sent to the players who have the chunk), never in the block or an item. So nothing
 * looks different, a world opened without the mod only grows as beta's does, and whatever a snipped plant drops is the plain item. A cell's mark goes as soon as its block
 * becomes anything else, so a plant broken and planted again grows.
 * <p>
 * Snipped means it gets no growth from beta's random ticks, which are the only way any of them grows by itself: a sapling never becomes a tree, wheat stays at its stage,
 * a mushroom doesn't spread, and with Pumpkins a pumpkin doesn't either. A sugar cane or cactus is snipped as a whole column, every cell of it marked, and it counts as snipped
 * if any cell is: snip any part and it stays as tall as it is, breaking its top off doesn't start it again, and one put on top doesn't grow either.
 */
public final class Snips {
	/** A chunk's snipped cells, as {@code x << 11 | z << 7 | y} within it, sorted. */
	static Attachment<Chunk, short[]> marks;
	private static final boolean[] GROWS = new boolean[256];

	private static final Codec<short[]> CELLS = Codec.of(cells -> {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) {
			for (short c : cells) out.writeShort(c);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		NbtCompound n = new NbtCompound();
		n.putByteArray("cells", bytes.toByteArray());
		return n;
	}, n -> {
		byte[] b = n.getByteArray("cells");
		short[] cells = new short[b.length / 2];
		for (int i = 0; i < cells.length; i++) cells[i] = (short) ((b[2 * i] & 0xFF) << 8 | b[2 * i + 1] & 0xFF);
		Arrays.sort(cells);
		return cells;
	});

	private Snips() {
	}

	static void declare(Mod mod) {
		marks = mod.attachment("snipped", Attachment.chunk(CELLS)).persist().sync(Attachment.Sync.TRACKING);
		for (Block b : new Block[] {Block.SAPLING, Block.WHEAT, Block.BROWN_MUSHROOM, Block.RED_MUSHROOM, Block.SUGAR_CANE, Block.CACTUS}) GROWS[b.id] = true;
		// Beta's pumpkins don't grow; Pumpkins' spread.
		if (FabricLoader.getInstance().isModLoaded("pumpkins")) GROWS[Block.PUMPKIN.id] = true;
	}

	/** Whether shears can snip this block: one of the things that grow by themselves. */
	public static boolean grows(int id) {
		return id >= 0 && id < GROWS.length && GROWS[id];
	}

	private static boolean column(int id) {
		return id == Block.SUGAR_CANE.id || id == Block.CACTUS.id;
	}

	private static short cell(int x, int y, int z) {
		return (short) ((x & 15) << 11 | (z & 15) << 7 | y);
	}

	private static boolean marked(World world, int x, int y, int z) {
		short[] cells = marks.get(world.getChunk(x >> 4, z >> 4));
		return cells != null && Arrays.binarySearch(cells, cell(x, y, z)) >= 0;
	}

	/** The lowest cell of the column of {@code id} that {@code y} is in. */
	private static int bottom(World world, int x, int y, int z, int id) {
		while (y > 0 && world.getBlockId(x, y - 1, z) == id) y--;
		return y;
	}

	/** Whether the plant here is snipped; false for anything that isn't one of those that grow. */
	public static boolean snipped(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (!grows(id)) return false;
		if (!column(id)) return marked(world, x, y, z);
		for (int ty = bottom(world, x, y, z, id); ty < 128 && world.getBlockId(x, ty, z) == id; ty++) {
			if (marked(world, x, ty, z)) return true;
		}
		return false;
	}

	/** Snips the plant here, the whole column for a cane or cactus. Where the world runs. */
	public static void snip(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		short[] cells = marks.get(chunk);
		cells = cells == null ? new short[0] : cells.clone();
		int top = y, from = y;
		if (column(id)) {
			from = bottom(world, x, y, z, id);
			for (top = from; top < 127 && world.getBlockId(x, top + 1, z) == id; top++) {
			}
		}
		for (int ty = from; ty <= top; ty++) {
			short c = cell(x, ty, z);
			if (Arrays.binarySearch(cells, c) >= 0) continue;
			cells = Arrays.copyOf(cells, cells.length + 1);
			cells[cells.length - 1] = c;
			Arrays.sort(cells);
		}
		marks.set(chunk, cells);
	}

	/**
	 * Shears used on a block. Unsnipped, the plant is snipped at a use of the shears, with its own sound as beta's hoe makes the farmland's, and the arm swings; snipped
	 * already, or not a plant that grows, nothing at all happens, which is how a player tells. A client on a server knows the marks of the chunks it has, so it swings
	 * and sounds only when the server will snip.
	 */
	public static boolean useShears(ItemStack shears, PlayerEntity player, World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (!grows(id) || snipped(world, x, y, z)) return false;
		Block block = Block.BLOCKS[id];
		world.playSound(x + 0.5F, y + 0.5F, z + 0.5F, block.soundGroup.getSound(), (block.soundGroup.getVolume() + 1.0F) / 2.0F, block.soundGroup.getPitch() * 0.8F);
		if (world.isRemote) return true;
		snip(world, x, y, z);
		shears.damage(1, player);
		return true;
	}

	/** Whether a random tick of the block here should pass it by: it's a plant that grows, and snipped. */
	public static boolean stops(Block block, World world, int x, int y, int z) {
		return grows(block.id) && snipped(world, x, y, z);
	}

	/**
	 * What a snipped plant's random tick still does: a sapling's and wheat's begin with beta's check that pops them off ground they can't stand on, as a flower's is, and
	 * that stays. The others' do nothing but grow.
	 */
	public static void stillTick(Block block, World world, int x, int y, int z) {
		if (block instanceof SaplingBlock || block instanceof CropBlock) ((PlantBlockInvoker) block).snipping$breakIfCannotGrow(world, x, y, z);
	}

	/**
	 * A chunk's cell about to hold {@code id}: if it's marked and its block changes, the mark goes. Where the world runs; a client hears from the server.
	 * <p>
	 * Whatever the cell held: with the mod here a mark only ever sits on a plant that grows, but while it was out (or Pumpkins was, for a pumpkin's) the core kept the
	 * marks and the world went on, so a snipped sapling may be a tree trunk by now and a snipped field harvested. Such a mark goes at the cell's next change, before
	 * anything planted there could take it. A plant planted again while the mod was out is the one case that stays snipped, as nothing tells it from the one that was.
	 */
	public static void changing(Chunk chunk, int x, int y, int z, int id) {
		if (chunk.world == null || chunk.world.isRemote) return;
		short[] cells = marks.get(chunk);
		if (cells == null || chunk.getBlockId(x, y, z) == id) return;
		int at = Arrays.binarySearch(cells, cell(x, y, z));
		if (at < 0) return;
		short[] left = new short[cells.length - 1];
		System.arraycopy(cells, 0, left, 0, at);
		System.arraycopy(cells, at + 1, left, at, left.length - at);
		marks.set(chunk, left.length == 0 ? null : left);
	}
}
