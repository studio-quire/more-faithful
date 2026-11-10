package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The id's high bits go in the meta byte's top nibble, always 0 in vanilla. */
@Mixin(BlockUpdateS2CPacket.class)
abstract class BlockUpdateConstructorMixin {
	@Shadow
	public int blockRawId;
	@Shadow
	public int blockMetadata;

	@Inject(method = "<init>(IIILnet/minecraft/world/World;)V", at = @At("TAIL"))
	private void faithful$highBits(int x, int y, int z, World world, CallbackInfo ci) {
		blockMetadata |= (blockRawId >> 8 & 15) << 4;
	}
}
