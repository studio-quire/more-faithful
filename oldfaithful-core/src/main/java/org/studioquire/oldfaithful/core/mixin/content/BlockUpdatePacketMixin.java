package org.studioquire.oldfaithful.core.mixin.content;

import java.io.DataInputStream;

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** A block update's id: the meta byte's top nibble holds its high bits. */
@Mixin(BlockUpdateS2CPacket.class)
abstract class BlockUpdatePacketMixin {
	@Shadow
	public int blockRawId;
	@Shadow
	public int blockMetadata;

	@Inject(method = "read", at = @At("TAIL"))
	private void faithful$whole(DataInputStream stream, CallbackInfo ci) {
		if (!Wire.serverContent) return;
		blockRawId |= (blockMetadata >> 4 & 15) << 8;
		blockMetadata &= 15;
	}
}
