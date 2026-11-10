package org.studioquire.oldfaithful.core.api;

import net.minecraft.nbt.NbtCompound;

/**
 * A block entity whose clients need some of its data: implement this on any {@code BlockEntity} (a subclass of a vanilla one too) and
 * it goes out with its chunk, and after each {@link BlockEntities#sync}. Write only what a client draws or shows, not everything
 * {@code writeNbt} saves: 32 KB at most, and past that the update is left out and logged.
 * <p>
 * In single player there's nothing to do: the client's world is the world, so {@link #readSync} isn't called.
 */
public interface SyncedBlockEntity {
	/** Where the world runs. */
	void writeSync(NbtCompound nbt);

	/** On a client, from what {@link #writeSync} wrote. The block is redrawn after. */
	void readSync(NbtCompound nbt);
}
