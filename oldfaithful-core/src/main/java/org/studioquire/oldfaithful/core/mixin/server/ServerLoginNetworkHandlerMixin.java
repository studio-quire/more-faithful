package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.login.LoginHelloPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.impl.server.ServerNet;

/** The login is the one place a client says what it is. */
@Mixin(ServerLoginNetworkHandler.class)
abstract class ServerLoginNetworkHandlerMixin {
	@Inject(method = "accept", at = @At("HEAD"), cancellable = true)
	private void faithful$admit(LoginHelloPacket packet, CallbackInfo ci) {
		if (!ServerNet.admit((ServerLoginNetworkHandler) (Object) this, packet)) ci.cancel();
	}

	@WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;addPlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;)V"))
	private void faithful$joined(PlayerManager manager, ServerPlayerEntity player, Operation<Void> add) {
		add.call(manager, player);
		ServerNet.joined(player);
	}

	/** Beta's last step of a login: the inventory is on its way. */
	@Inject(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;initScreenHandler()V", shift = At.Shift.AFTER))
	private void faithful$notice(LoginHelloPacket packet, CallbackInfo ci, @Local ServerPlayerEntity player) {
		Players.joined(player);
	}
}
