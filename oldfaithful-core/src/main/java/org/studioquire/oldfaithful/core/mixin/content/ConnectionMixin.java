package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.network.Connection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.impl.PeerHolder;
import org.studioquire.oldfaithful.core.impl.content.StackData;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** Each connection's reader and writer threads say whose packets they're handling, for stack data, and its queue says how much of that a packet may carry. */
@Mixin(Connection.class)
abstract class ConnectionMixin {
	@Shadow
	private int sendQueueSize;

	@Inject(method = "sendPacket", at = @At("HEAD"))
	private void faithful$room(Packet packet, CallbackInfo ci) {
		if (packet instanceof StackData.Roomy) ((StackData.Roomy) packet).faithful$room(StackData.MAX_QUEUE_BYTES - sendQueueSize);
	}

	@Inject(method = {"read", "write"}, at = @At("HEAD"))
	private void faithful$thread(CallbackInfoReturnable<Boolean> cir) {
		Wire.current(((PeerHolder) this).faithful$peer());
	}
}
