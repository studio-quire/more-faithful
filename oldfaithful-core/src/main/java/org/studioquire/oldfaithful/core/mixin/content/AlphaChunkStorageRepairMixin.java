package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/** Once a chunk's block entities are in: any of a mod's blocks without one gets a fresh one. */
@Mixin(AlphaChunkStorage.class)
abstract class AlphaChunkStorageRepairMixin {
	@ModifyReturnValue(method = "loadChunkFromNbt", at = @At("RETURN"))
	private static Chunk faithful$repair(Chunk chunk) {
		Overlay.restoreBlockEntities(chunk);
		return chunk;
	}
}
