package org.studioquire.faithful.alchemy.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.alchemy.Alchemy;

/**
 * Haste's walking and the feather's falls. A mixin because the core has hooks for neither: walking speed is one number in {@code travel}, and a fall's damage goes into {@code damage} with no
 * source, as fire's and drowning's do, so the damage hook can't tell it apart.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/**
	 * The third of {@code travel}'s pushes is the one on land and in the air, after water's and lava's; on the ground it's walking. Movement is the client's own in beta, and a server only checks a
	 * move is no longer than ten blocks, so a client walks by what it heard of its haste.
	 */
	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;moveNonSolid(FFF)V", ordinal = 2))
	private void alchemy$hasteWalks(LivingEntity self, float sideways, float forward, float speed, Operation<Void> push) {
		if (self.onGround && self instanceof PlayerEntity) speed *= Alchemy.haste((PlayerEntity) self);
		push.call(self, sideways, forward, speed);
	}

	/** A fall's damage, beta's {@code ceil(distance - 3)}, a third as much to the nearest whole: a four-block drop does nothing, a fatal 23 does seven. Its thud goes with the damage, as in beta. */
	@ModifyExpressionValue(method = "onLanding", at = @At(value = "INVOKE", target = "Ljava/lang/Math;ceil(D)D"))
	private double alchemy$featherFalls(double damage) {
		Object self = this;
		if (damage <= 0 || !(self instanceof PlayerEntity) || Alchemy.feathered == null || !Alchemy.feathered.running((PlayerEntity) self)) return damage;
		return Math.round(damage / Alchemy.FEATHER);
	}
}
