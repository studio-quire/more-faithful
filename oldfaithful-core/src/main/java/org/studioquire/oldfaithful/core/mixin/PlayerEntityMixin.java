package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Players;

/** Death in single player, and a client's own view of it on a server. */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
	@WrapOperation(method = "onKilledBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;dropInventory()V"))
	private void faithful$drops(PlayerInventory inventory, Operation<Void> drop) {
		Players.death((PlayerEntity) (Object) this, () -> drop.call(inventory));
	}
}
