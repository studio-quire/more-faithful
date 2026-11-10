package org.studioquire.oldfaithful.core.mixin.content.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** A block break's event, sent to the players around: {@code id + meta * 256} has no room for a big id. */
@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerMixin {
	@WrapOperation(method = "tryBreakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorld;worldEvent(Lnet/minecraft/entity/player/PlayerEntity;IIIII)V"))
	private void faithful$breakEvent(ServerWorld world, PlayerEntity player, int event, int x, int y, int z, int data, Operation<Void> send) {
		send.call(world, player, event, x, y, z, Wire.breakEvent(world.getBlockId(x, y, z), world.getBlockMeta(x, y, z)));
	}
}
