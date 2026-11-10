package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.SyncedBlockEntity;
import org.studioquire.oldfaithful.core.impl.content.ContentNet;

/** Every path that sends block entities asks for this packet: a chunk going out, and the three sizes of block change. */
@Mixin(BlockEntity.class)
abstract class BlockEntityMixin {
	@Inject(method = "createUpdatePacket", at = @At("HEAD"), cancellable = true)
	private void faithful$sync(CallbackInfoReturnable<Packet> cir) {
		if ((Object) this instanceof SyncedBlockEntity) cir.setReturnValue(ContentNet.blockEntityPacket((BlockEntity) (Object) this));
	}
}
