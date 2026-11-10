package org.studioquire.oldfaithful.core.impl.content;

import net.minecraft.nbt.NbtCompound;

/** Implemented by the game's ItemStack. */
public interface StackDataHolder {
	NbtCompound faithful$data();

	void faithful$setData(NbtCompound data);
}
