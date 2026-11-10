package org.studioquire.oldfaithful.core.api;

import java.util.function.Consumer;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.impl.SavedChunks;

/**
 * Chunks coming and going where the world runs. Never on a client on a server, where chunks are only a view. "Built" is the
 * worldgen pass's.
 */
public final class Chunks {
	private static final Listeners<Consumer<Chunk>> LOADED = new Listeners<>("Chunks.onLoaded");
	private static final Listeners<Consumer<Chunk>> UNLOADED = new Listeners<>("Chunks.onUnloaded");

	private Chunks() {
	}

	/**
	 * Notice: a chunk has joined the world, read from disk or newly made, with its entities and block entities added. A new one
	 * isn't populated yet. Nothing here may load another chunk.
	 */
	public static void onLoaded(Consumer<Chunk> listener) {
		LOADED.add(listener);
	}

	/**
	 * Notice: a chunk is leaving the world, with its entities still in it, before it's saved. Not heard for the chunks still in
	 * memory when a world closes: {@link Lifecycle#onWorldUnload} covers those.
	 */
	public static void onUnloaded(Consumer<Chunk> listener) {
		UNLOADED.add(listener);
	}

	/** Whether a chunk, by its chunk coordinates, is in memory. Asks without loading it, as a world's own {@code getChunk} wouldn't. Works on any side, for what that side has. */
	public static boolean isLoaded(World world, int chunkX, int chunkZ) {
		return world.isPosLoaded(chunkX << 4, 0, chunkZ << 4);
	}

	/** Whether a chunk has been saved to disk, whether or not it's also in memory, looking at the disk and loading nothing. A chunk made since its last save reads as not saved. */
	public static boolean isSaved(World world, int chunkX, int chunkZ) {
		return SavedChunks.exists(world, chunkX, chunkZ);
	}

	/** The core's. */
	public static void loaded(Chunk chunk) {
		if (!chunk.world.isRemote) LOADED.each(l -> l.accept(chunk));
	}

	/** The core's. */
	public static void unloaded(Chunk chunk) {
		if (!chunk.world.isRemote) UNLOADED.each(l -> l.accept(chunk));
	}
}
