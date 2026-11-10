package org.studioquire.oldfaithful.core.impl.content;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.impl.Peer;

/**
 * Ids past 255 in vanilla's packets, with no change of length: the high bits ride where vanilla leaves room. Only on a connection whose
 * hello said the server has content, so a third-party server's odd byte is read as vanilla would.
 */
public final class Wire {
	/** On a client: the server joined has content. Set before any world packet arrives. */
	public static volatile boolean serverContent;
	/** The connection this thread reads or writes for. */
	private static final ThreadLocal<Peer> CURRENT = new ThreadLocal<>();

	private Wire() {
	}

	/** Implemented by the multi-block packet: its ids in order, whole. */
	public interface DeltaIds {
		int faithful$next(int fallback);
	}

	public static void current(Peer peer) {
		CURRENT.set(peer);
	}

	/** Whether the stacks in packets on this thread's connection carry their data. */
	public static boolean stackData() {
		Peer peer = CURRENT.get();
		return peer != null && peer.content;
	}

	/** Bytes the chunk data's overlay takes per cell, after the vanilla section. */
	public static final int TAIL_PER_CELL = 2;

	/** The vanilla section's length, whatever part of it the world filled: the region as declared may run past y 128. */
	public static int vanillaLength(int sizeX, int sizeY, int sizeZ) {
		return sizeX * sizeY * sizeZ * 5 / 2;
	}

	public static int tailLength(int sizeX, int sizeY, int sizeZ) {
		return sizeX * sizeY * sizeZ * TAIL_PER_CELL;
	}

	/** Packet 61's block break: id's low byte, meta, then the id's high bits. */
	public static int breakEvent(int id, int meta) {
		return id & 0xFF | meta << 8 | id >> 8 << 16;
	}

	public static int breakEventId(int data) {
		return data & 0xFF | (data >> 16 & 0xF) << 8;
	}

	/** Appends the overlay to a chunk packet's data, in the order vanilla's sections go. No overlay anywhere: nothing appended. */
	public static byte[] appendTail(World world, int x, int y, int z, int sizeX, int sizeY, int sizeZ, byte[] data) {
		int cx0 = x >> 4, cz0 = z >> 4, cx1 = x + sizeX - 1 >> 4, cz1 = z + sizeZ - 1 >> 4;
		boolean any = false;
		for (int cx = cx0; cx <= cx1 && !any; cx++) {
			for (int cz = cz0; cz <= cz1 && !any; cz++) any = Overlay.of(world.getChunk(cx, cz)).faithful$ext() != null;
		}
		if (!any) return data;
		int base = vanillaLength(sizeX, sizeY, sizeZ);
		byte[] out = java.util.Arrays.copyOf(data, base + tailLength(sizeX, sizeY, sizeZ));
		int minY = Math.max(y, 0), maxY = Math.min(y + sizeY, 128);
		int at = base;
		for (int cx = cx0; cx <= cx1; cx++) {
			int minX = Math.max(x - cx * 16, 0), maxX = Math.min(x + sizeX - cx * 16, 16);
			for (int cz = cz0; cz <= cz1; cz++) {
				int minZ = Math.max(z - cz * 16, 0), maxZ = Math.min(z + sizeZ - cz * 16, 16);
				char[] ext = Overlay.of(world.getChunk(cx, cz)).faithful$ext();
				for (int lx = minX; lx < maxX; lx++) {
					for (int lz = minZ; lz < maxZ; lz++) {
						for (int ly = minY; ly < maxY; ly++) {
							int c = ext == null ? 0 : ext[lx << 11 | lz << 7 | ly];
							out[at++] = (byte) (c >> 8);
							out[at++] = (byte) c;
						}
					}
				}
			}
		}
		return out;
	}

	/**
	 * One chunk's part of a tail, into its overlay; returns where the next begins. All zeros where the server sent none, which
	 * clears the region. Ids here came from the server: one this game hasn't got ends the connection.
	 */
	/** The block entities in a region of a chunk, with the block each stands on. */
	public static Map<BlockEntity, Integer> blockEntitiesIn(Chunk chunk, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		Map<BlockEntity, Integer> held = new HashMap<>();
		for (Object o : chunk.blockEntities.values()) {
			BlockEntity entity = (BlockEntity) o;
			int x = entity.x & 15, z = entity.z & 15;
			if (x >= minX && x < maxX && entity.y >= minY && entity.y < maxY && z >= minZ && z < maxZ) held.put(entity, chunk.getBlockId(x, entity.y, z));
		}
		return held;
	}

	/** Those of {@link #blockEntitiesIn} whose block has since changed go, and the new block makes its own when it's asked for one. */
	public static void dropReplaced(Chunk chunk, Map<BlockEntity, Integer> held) {
		held.forEach((entity, id) -> {
			if (chunk.getBlockId(entity.x & 15, entity.y, entity.z & 15) != id) chunk.world.removeBlockEntity(entity.x, entity.y, entity.z);
		});
	}

	public static int applyTail(Chunk chunk, byte[] data, int at, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		Overlay.Holder h = Overlay.of(chunk);
		char[] ext = h.faithful$ext();
		for (int lx = minX; lx < maxX; lx++) {
			for (int lz = minZ; lz < maxZ; lz++) {
				for (int ly = minY; ly < maxY; ly++) {
					int c = (data[at] & 0xFF) << 8 | data[at + 1] & 0xFF;
					at += 2;
					if (c != 0 && !ContentRegistry.modBlock(c & 0xFFF)) throw new IllegalStateException("The server sent a block this game doesn't have (id " + (c & 0xFFF) + ")");
					if (c != 0 && ext == null) {
						ext = new char[Overlay.CELLS];
						h.faithful$setExt(ext);
					}
					if (ext != null) ext[lx << 11 | lz << 7 | ly] = (char) c;
				}
			}
		}
		return at;
	}
}
