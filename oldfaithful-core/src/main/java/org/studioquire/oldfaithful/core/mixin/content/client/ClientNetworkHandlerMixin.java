package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** Multi-block updates set the whole id: the handler's own is the byte's. */
@Mixin(ClientNetworkHandler.class)
abstract class ClientNetworkHandlerMixin {
	@WrapOperation(method = "onChunkDeltaUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;setBlock(IIIII)Z"))
	private boolean faithful$wholeId(Chunk chunk, int x, int y, int z, int id, int meta, Operation<Boolean> set, ChunkDeltaUpdateS2CPacket packet) {
		return set.call(chunk, x, y, z, ((Wire.DeltaIds) packet).faithful$next(id), meta);
	}
}
