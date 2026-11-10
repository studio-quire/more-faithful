package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/** The client-only height map pass, as ChunkMixin's for the others. */
@Mixin(Chunk.class)
abstract class ChunkHeightMixin {
	@Redirect(method = "populateHeightMapOnly", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", args = "array=get"))
	private byte faithful$opacityCell(byte[] array, int index) {
		return Overlay.opacityCell(((Overlay.Holder) this).faithful$ext(), array, index);
	}
}
