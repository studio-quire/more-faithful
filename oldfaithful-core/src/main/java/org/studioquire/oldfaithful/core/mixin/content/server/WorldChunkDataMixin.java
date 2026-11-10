package org.studioquire.oldfaithful.core.mixin.content.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** Chunk data: vanilla's sections, then the overlay. */
@Mixin(World.class)
abstract class WorldChunkDataMixin {
	@ModifyReturnValue(method = "getChunkData", at = @At("RETURN"))
	private byte[] faithful$tail(byte[] data, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		return Wire.appendTail((World) (Object) this, x, y, z, sizeX, sizeY, sizeZ, data);
	}
}
