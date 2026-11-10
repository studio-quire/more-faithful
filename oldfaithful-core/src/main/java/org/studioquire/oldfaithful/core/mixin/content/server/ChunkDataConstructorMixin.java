package org.studioquire.oldfaithful.core.mixin.content.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** {@code deflate} runs once into this buffer: too small and the overlay is cut off silently. */
@Mixin(ChunkDataS2CPacket.class)
abstract class ChunkDataConstructorMixin {
	@WrapOperation(method = "<init>(IIIIIILnet/minecraft/world/World;)V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;chunkData:[B"))
	private void faithful$room(ChunkDataS2CPacket packet, byte[] buffer, Operation<Void> set) {
		set.call(packet, new byte[buffer.length + buffer.length / 5 * 4 + 1024]);
	}
}
