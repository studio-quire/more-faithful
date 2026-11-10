package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.ChannelPacket;

/** Teaches the game packet 250, both ways. It's sent only to a peer that has declared itself. */
@Mixin(Packet.class)
abstract class PacketMixin {
	@Shadow
	static void register(int rawId, boolean clientBound, boolean serverBound, Class type) {
		throw new AssertionError();
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void faithful$registerChannelPacket(CallbackInfo ci) {
		register(ChannelPacket.ID, true, true, ChannelPacket.class);
	}
}
