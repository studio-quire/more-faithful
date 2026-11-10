package org.studioquire.oldfaithful.core.mixin;

import java.io.File;

import net.minecraft.world.chunk.storage.RegionChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a dimension's regions are. */
@Mixin(RegionChunkStorage.class)
public interface RegionChunkStorageAccessor {
	@Accessor("dir")
	File faithful$dir();
}
