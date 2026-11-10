package org.studioquire.oldfaithful.core.mixin.content.client;

import java.util.Map;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/**
 * The overlay goes in before vanilla's load, which runs the height map pass itself. Vanilla's load leaves block entities alone, so a cell whose block changed by this packet
 * (the server sends a region when many change at once) would keep the old block's, and a chest's {@code onUse} casts what it finds even on a client.
 */
@Mixin(World.class)
abstract class ClientWorldChunkMixin {
	/** Where the next chunk's part of the tail starts; -1 with no tail. */
	@Unique
	private int faithful$tail;

	@Inject(method = "handleChunkDataUpdate", at = @At("HEAD"))
	private void faithful$start(int x, int y, int z, int sizeX, int sizeY, int sizeZ, byte[] data, CallbackInfo ci) {
		int vanilla = Wire.vanillaLength(sizeX, sizeY, sizeZ);
		faithful$tail = data.length >= vanilla + Wire.tailLength(sizeX, sizeY, sizeZ) ? vanilla : -1;
	}

	@WrapOperation(method = "handleChunkDataUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;loadFromPacket([BIIIIIII)I"))
	private int faithful$overlay(Chunk chunk, byte[] data, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int offset, Operation<Integer> load) {
		Map<BlockEntity, Integer> held = Wire.blockEntitiesIn(chunk, minX, minY, minZ, maxX, maxY, maxZ);
		if (faithful$tail >= 0) faithful$tail = Wire.applyTail(chunk, data, faithful$tail, minX, minY, minZ, maxX, maxY, maxZ);
		int end = load.call(chunk, data, minX, minY, minZ, maxX, maxY, maxZ, offset);
		Wire.dropReplaced(chunk, held);
		return end;
	}
}
