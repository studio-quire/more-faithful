package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.network.packet.login.LoginHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.CoreNet;

/** The client's login packet says it has the core, in a field vanilla's server never reads. */
@Mixin(LoginHelloPacket.class)
abstract class LoginHelloPacketMixin {
	@Shadow
	public long worldSeed;

	@Inject(method = "<init>(Ljava/lang/String;I)V", at = @At("RETURN"))
	private void faithful$declare(String username, int protocolVersion, CallbackInfo ci) {
		this.worldSeed = CoreNet.MAGIC;
	}
}
