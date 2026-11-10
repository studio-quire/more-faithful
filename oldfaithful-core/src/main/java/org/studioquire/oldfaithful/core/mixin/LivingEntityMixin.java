package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.Entities;

/** Every living thing's damage and death; {@code ServerPlayerEntity} has its own {@code onKilledBy} and is hooked apart. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	@WrapMethod(method = "damage")
	private boolean faithful$damage(Entity source, int amount, Operation<Boolean> damage) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!Entities.hasDamageListeners() || self.world.isRemote) return damage.call(source, amount);
		Entities.Damage event = Entities.damaged(self, source, amount);
		return !event.cancelled() && damage.call(source, event.amount());
	}

	@Inject(method = "onKilledBy", at = @At("HEAD"))
	private void faithful$died(Entity adversary, CallbackInfo ci) {
		Entities.died((LivingEntity) (Object) this, adversary);
	}

	@WrapOperation(method = "onKilledBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropItems()V"))
	private void faithful$drops(LivingEntity self, Operation<Void> drop, Entity adversary) {
		if (!Drops.hasMobListeners() || self instanceof PlayerEntity) {
			drop.call(self);
			return;
		}
		Drops.gatherMob(self, adversary, s -> self.dropItem(s, 0.0F), () -> drop.call(self));
	}
}
