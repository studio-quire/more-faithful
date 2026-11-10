package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The arrays are bytes: each id's high bits go in its meta's top nibble, always 0 in vanilla. */
@Mixin(ChunkDeltaUpdateS2CPacket.class)
abstract class ChunkDeltaConstructorMixin {
	@Shadow
	public byte[] blockMetadata;

	@Inject(method = "<init>(II[SILnet/minecraft/world/World;)V", at = @At("TAIL"))
	private void faithful$highBits(int x, int z, short[] positions, int size, World world, CallbackInfo ci) {
		Chunk chunk = world.getChunk(x, z);
		for (int i = 0; i < size; i++) {
			int id = chunk.getBlockId(positions[i] >> 12 & 15, positions[i] & 255, positions[i] >> 8 & 15);
			blockMetadata[i] |= (byte) ((id >> 8 & 15) << 4);
		}
	}
}
