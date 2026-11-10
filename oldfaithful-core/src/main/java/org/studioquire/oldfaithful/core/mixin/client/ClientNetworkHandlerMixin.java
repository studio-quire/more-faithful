package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.packet.login.LoginHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;

/** Which server this game is on, if any. */
@Mixin(ClientNetworkHandler.class)
abstract class ClientNetworkHandlerMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void faithful$connected(CallbackInfo ci) {
		ClientNet.connected((ClientNetworkHandler) (Object) this);
	}

	@Inject(method = "onHello", at = @At("HEAD"))
	private void faithful$onServer(LoginHelloPacket packet, CallbackInfo ci) {
		Rules.openOnServer();
	}

	@Inject(method = "onDisconnected", at = @At("HEAD"))
	private void faithful$disconnected(CallbackInfo ci) {
		ClientNet.disconnected();
	}
}
