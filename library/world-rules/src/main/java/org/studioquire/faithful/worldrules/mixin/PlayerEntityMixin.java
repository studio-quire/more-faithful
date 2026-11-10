package org.studioquire.faithful.worldrules.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.worldrules.WorldRules;

/**
 * A mixin because one line of {@code attack} is all that's changed, and no other mod would want a hook for it. A hit while velocityY is below zero does one more damage, and gravity keeps it below
 * zero even standing still, so in beta that's nearly every hit. Read as zero, there's no bonus. Fixed reads it as zero unless really falling, by the conditions later versions gave critical hits.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
	@ModifyExpressionValue(method = "attack", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;velocityY:D"))
	private double worldrules$fallingBonus(double velocityY) {
		String rule = WorldRules.fallingBonus.get();
		if (rule.equals("off")) return 0.0;
		if (rule.equals("beta")) return velocityY;
		PlayerEntity self = (PlayerEntity) (Object) this;
		return self.onGround || self.isOnLadder() || self.checkWaterCollisions() || self.vehicle != null ? 0.0 : velocityY;
	}
}
