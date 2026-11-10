package org.studioquire.oldfaithful.core.impl.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.world.chunk.Chunk;

/**
 * The chunk overlay. {@code blocks} and {@code meta} are always a whole vanilla chunk, a mod's block being its stand-in there; the
 * overlay says what's really in such a cell: 0, or id | meta << 12. In a save it's names, cell by cell, in the chunk's {@code Faithful}
 * compound; a name with no block here stays dormant, its stand-ins showing, and goes back out for every cell that still holds one.
 */
public final class Overlay {
	public static final int CELLS = 32768;

	/** Implemented by the game's Chunk. */
	public interface Holder {
		char[] faithful$ext();

		void faithful$setExt(char[] ext);

		Dormant faithful$dormant();

		void faithful$setDormant(Dormant dormant);
	}

	/** Cells of mods that aren't here. */
	public static final class Dormant {
		final List<String> names = new ArrayList<>();
		final List<int[]> standIns = new ArrayList<>(); // id, meta
		/** Cell, name index, meta, in threes. */
		int[] cells = new int[0];
	}

	private Overlay() {
	}

	/** What a vanilla reader sees: the stand-ins' array, for a test of that very thing. Mods read through {@code getBlockId}. */
	public static byte[] vanillaBlocks(Chunk chunk) {
		return chunk.blocks;
	}

	public static boolean modded(int id) {
		return ContentRegistry.modBlock(id);
	}

	public static int standId(int id) {
		return modded(id) ? ContentRegistry.STAND_IN[id] & 0xFF : id;
	}

	public static int standMeta(int id, int meta) {
		return modded(id) ? ContentRegistry.STAND_IN[id] >> 8 : meta;
	}

	/** A vanilla slot nothing uses, for {@link #opacityCell}. */
	private static final int SPARE = 255;

	/** What a height map loop should read at a cell: the vanilla byte, or for a mod's block the spare id, carrying its opacity. */
	public static byte opacityCell(char[] ext, byte[] blocks, int cell) {
		int c = ext == null ? 0 : ext[cell];
		if (c == 0) return blocks[cell];
		net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY[SPARE] = net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY[c & 0xFFF];
		return (byte) SPARE;
	}

	/** The same, for a reader that wants the cell's light too. */
	public static byte lightCell(char[] ext, byte[] blocks, int cell) {
		int c = ext == null ? 0 : ext[cell];
		if (c == 0) return blocks[cell];
		net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY[SPARE] = net.minecraft.block.Block.BLOCKS_LIGHT_OPACITY[c & 0xFFF];
		net.minecraft.block.Block.BLOCKS_LIGHT_LUMINANCE[SPARE] = net.minecraft.block.Block.BLOCKS_LIGHT_LUMINANCE[c & 0xFFF];
		return (byte) SPARE;
	}

	/**
	 * A mod's block with a block entity that has none (a damaged save): breaking a chest-like one reads it unchecked. Vanilla's are seen to
	 * by the saves mod; this is the same for ours.
	 */
	public static void restoreBlockEntities(Chunk chunk) {
		char[] ext = of(chunk).faithful$ext();
		if (ext == null) return;
		for (int i = 0; i < CELLS; i++) {
			int c = ext[i];
			if (c == 0 || !net.minecraft.block.Block.BLOCKS_WITH_ENTITY[c & 0xFFF]) continue;
			int x = i >> 11, z = i >> 7 & 15, y = i & 127;
			if (chunk.blockEntities.containsKey(new net.minecraft.util.math.BlockPos(x, y, z))) continue;
			net.minecraft.block.entity.BlockEntity entity = ((BlockEntityMaker) net.minecraft.block.Block.BLOCKS[c & 0xFFF]).faithful$makeBlockEntity();
			if (entity != null) chunk.setBlockEntity(x, y, z, entity);
		}
	}

	/** Implemented by the game's BlockWithEntity. */
	public interface BlockEntityMaker {
		net.minecraft.block.entity.BlockEntity faithful$makeBlockEntity();
	}

	public static Holder of(Chunk chunk) {
		return (Holder) chunk;
	}

	/** The compound's overlay keys, written from what's in the chunk; removed if there's nothing to say. */
	public static void save(Chunk chunk, NbtCompound compound) {
		Holder h = of(chunk);
		char[] ext = h.faithful$ext();
		Dormant dormant = h.faithful$dormant();
		for (String key : new String[] {"V", "Names", "StandIns", "Cells", "CellsHigh", "Meta"}) removeKey(compound, key);
		if (ext == null && dormant == null) return;

		Map<Integer, Integer> ids = new HashMap<>();
		List<String> names = new ArrayList<>();
		List<int[]> standIns = new ArrayList<>();
		int[] cell = new int[CELLS];
		byte[] meta = new byte[CELLS / 2];
		boolean any = false;
		if (ext != null) {
			for (int i = 0; i < CELLS; i++) {
				int c = ext[i];
				if (c == 0) continue;
				int id = c & 0xFFF;
				Integer index = ids.get(id);
				if (index == null) {
					index = names.size();
					ids.put(id, index);
					names.add(ContentRegistry.blockName(id));
					standIns.add(new int[] {ContentRegistry.STAND_IN[id] & 0xFF, ContentRegistry.STAND_IN[id] >> 8});
				}
				cell[i] = index + 1;
				putNibble(meta, i, c >> 12);
				any = true;
			}
		}
		if (dormant != null) {
			int[] map = new int[dormant.names.size()];
			for (int k = 0; k + 2 < dormant.cells.length; k += 3) {
				int i = dormant.cells[k], name = dormant.cells[k + 1];
				int[] standIn = dormant.standIns.get(name);
				// Changed since it was loaded: the world moved on, and the mod's block is gone from here.
				if ((chunk.blocks[i] & 0xFF) != standIn[0] || (ext != null && ext[i] != 0)) continue;
				if (map[name] == 0) {
					names.add(dormant.names.get(name));
					standIns.add(standIn);
					map[name] = names.size();
				}
				cell[i] = map[name];
				putNibble(meta, i, dormant.cells[k + 2]);
				any = true;
			}
		}
		if (!any) return;

		compound.putByte("V", (byte) 1);
		NbtList list = new NbtList();
		for (String n : names) list.add(new NbtString(n));
		compound.put("Names", list);
		byte[] stand = new byte[names.size() * 2];
		for (int i = 0; i < names.size(); i++) {
			stand[2 * i] = (byte) standIns.get(i)[0];
			stand[2 * i + 1] = (byte) standIns.get(i)[1];
		}
		compound.putByteArray("StandIns", stand);
		byte[] low = new byte[CELLS];
		byte[] high = names.size() > 255 ? new byte[CELLS] : null;
		for (int i = 0; i < CELLS; i++) {
			low[i] = (byte) cell[i];
			if (high != null) high[i] = (byte) (cell[i] >> 8);
		}
		compound.putByteArray("Cells", low);
		if (high != null) compound.putByteArray("CellsHigh", high);
		compound.putByteArray("Meta", meta);
	}

	private static void removeKey(NbtCompound compound, String key) {
		((java.util.Collection<?>) compound.values()).removeIf(e -> key.equals(((NbtElement) e).getKey()));
	}

	private static void putNibble(byte[] nibbles, int i, int value) {
		int at = i >> 1;
		if ((i & 1) == 0) nibbles[at] = (byte) (nibbles[at] & 0xF0 | value & 15);
		else nibbles[at] = (byte) (nibbles[at] & 15 | (value & 15) << 4);
	}

	private static int nibble(byte[] nibbles, int i) {
		return (i & 1) == 0 ? nibbles[i >> 1] & 15 : nibbles[i >> 1] >> 4 & 15;
	}

	/** Back into the chunk, cell by cell, where the vanilla arrays still hold what was written. */
	public static void load(Chunk chunk, NbtCompound compound) {
		if (!compound.contains("Cells")) return;
		try {
			read(chunk, compound);
		} catch (RuntimeException e) {
			System.out.println("[faithful-core] WARN A damaged Faithful compound in chunk " + chunk.x + ", " + chunk.z + ": it plays as its stand-ins: " + e);
		}
	}

	private static void read(Chunk chunk, NbtCompound compound) {
		NbtList names = compound.getList("Names");
		byte[] stand = compound.getByteArray("StandIns");
		byte[] cells = compound.getByteArray("Cells");
		byte[] high = compound.contains("CellsHigh") ? compound.getByteArray("CellsHigh") : null;
		byte[] meta = compound.getByteArray("Meta");
		int n = names.size();
		if (cells.length != CELLS || meta.length != CELLS / 2 || stand.length != n * 2 || (high != null && high.length != CELLS)) {
			throw new IllegalArgumentException("arrays of the wrong size");
		}
		int[] ids = new int[n];
		Dormant dormant = null;
		int[] dormantIndex = new int[n];
		List<Integer> dormantCells = new ArrayList<>();
		for (int i = 0; i < n; i++) ids[i] = ContentRegistry.blockId(((NbtString) names.get(i)).value);
		char[] ext = null;
		for (int i = 0; i < CELLS; i++) {
			int c = (cells[i] & 0xFF) | (high == null ? 0 : (high[i] & 0xFF) << 8);
			if (c == 0) continue;
			if (c > n) throw new IllegalArgumentException("a cell past the names");
			int name = c - 1;
			if ((chunk.blocks[i] & 0xFF) != (stand[2 * name] & 0xFF)) continue; // something else edited this cell
			int m = nibble(meta, i);
			if (ids[name] >= 0) {
				if (ext == null) ext = new char[CELLS];
				ext[i] = (char) (ids[name] | m << 12);
			} else {
				if (dormant == null) dormant = new Dormant();
				if (dormantIndex[name] == 0) {
					dormant.names.add(((NbtString) names.get(name)).value);
					dormant.standIns.add(new int[] {stand[2 * name] & 0xFF, stand[2 * name + 1] & 0xFF});
					dormantIndex[name] = dormant.names.size();
				}
				dormantCells.add(i);
				dormantCells.add(dormantIndex[name] - 1);
				dormantCells.add(m);
			}
		}
		Holder h = of(chunk);
		h.faithful$setExt(ext);
		if (dormant != null) {
			dormant.cells = new int[dormantCells.size()];
			for (int i = 0; i < dormant.cells.length; i++) dormant.cells[i] = dormantCells.get(i);
			h.faithful$setDormant(dormant);
		}
	}
}
