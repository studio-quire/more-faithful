package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** On a server with content the buffer has room for the overlay; {@code inflate} takes as much as came, and the rest stays zero. */
@Mixin(ChunkDataS2CPacket.class)
abstract class ChunkDataPacketMixin {
	@Shadow
	public int sizeX;
	@Shadow
	public int sizeY;
	@Shadow
	public int sizeZ;

	@WrapOperation(method = "read", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;chunkData:[B"))
	private void faithful$room(ChunkDataS2CPacket packet, byte[] buffer, Operation<Void> set) {
		set.call(packet, Wire.serverContent ? new byte[Wire.vanillaLength(sizeX, sizeY, sizeZ) + Wire.tailLength(sizeX, sizeY, sizeZ)] : buffer);
	}
}
