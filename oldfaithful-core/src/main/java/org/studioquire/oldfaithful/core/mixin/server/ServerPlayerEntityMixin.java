package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Entities;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.impl.Attachments;

/** Death on a server: its own override, separate code from single player's, which doesn't call up to {@code LivingEntity}. */
@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityMixin {
	@Inject(method = "onKilledBy", at = @At("HEAD"))
	private void faithful$died(Entity adversary, CallbackInfo ci) {
		Entities.died((ServerPlayerEntity) (Object) this, adversary);
	}

	@WrapOperation(method = "onKilledBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;dropInventory()V"))
	private void faithful$drops(PlayerInventory inventory, Operation<Void> drop) {
		Players.death((ServerPlayerEntity) (Object) this, () -> drop.call(inventory));
	}

	/** A chunk has just gone out: its attachments follow it, in order. */
	@Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorld;getBlockEntities(IIIIII)Ljava/util/List;"))
	private void faithful$chunkSent(boolean sendChunks, CallbackInfo ci, @Local ChunkPos pos, @Local ServerWorld world) {
		Attachments.sendChunk(world.getChunk(pos.x, pos.z), (ServerPlayerEntity) (Object) this);
	}
}
