package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.impl.Attachments;

/** A respawn makes a new player object; what the old one kept goes with it, before the client is sent its inventory. */
@Mixin(PlayerManager.class)
abstract class PlayerManagerMixin {
	@ModifyExpressionValue(method = "respawnPlayer", at = @At(value = "NEW", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;"))
	private ServerPlayerEntity faithful$carryOver(ServerPlayerEntity reborn, ServerPlayerEntity player, int dimensionId) {
		Players.carryOver(player, reborn);
		return reborn;
	}

	@Inject(method = "respawnPlayer", at = @At("RETURN"))
	private void faithful$respawned(ServerPlayerEntity player, int dimensionId, CallbackInfoReturnable<ServerPlayerEntity> cir) {
		Attachments.sendAllToSelf(cir.getReturnValue());
		Players.respawned(player, cir.getReturnValue());
	}

	/** Both of beta's ways out, and stopping is apart. */
	@Inject(method = "disconnect", at = @At("HEAD"))
	private void faithful$leaving(ServerPlayerEntity player, CallbackInfo ci) {
		Players.leaving(player);
	}

	/** Only two worlds, so the way back is known. A client makes a new player object for this, so it hears all again. */
	@Inject(method = "changePlayerDimension", at = @At("TAIL"))
	private void faithful$dimension(ServerPlayerEntity player, CallbackInfo ci) {
		Attachments.sendAllToSelf(player);
		Players.dimensionChanged(player, player.dimensionId == 0 ? -1 : 0, player.dimensionId);
	}
}
