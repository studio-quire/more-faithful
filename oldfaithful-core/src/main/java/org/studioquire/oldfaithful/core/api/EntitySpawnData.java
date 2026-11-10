package org.studioquire.oldfaithful.core.api;

import net.minecraft.nbt.NbtCompound;

/**
 * Data a client needs before it can show an entity, past what its {@code DataTracker} carries (a size, a variant, a target): implement
 * this on the entity. Read once, on a client, before the entity joins the world. For data that changes later, use the tracker or a
 * channel. 32 KB at most.
 */
public interface EntitySpawnData {
	/** Where the world runs, each time a client starts tracking it. */
	void writeSpawnData(NbtCompound nbt);

	void readSpawnData(NbtCompound nbt);
}
