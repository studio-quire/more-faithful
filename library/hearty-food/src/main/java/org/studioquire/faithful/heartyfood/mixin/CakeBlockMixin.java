package org.studioquire.faithful.heartyfood.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.heartyfood.HeartyFood;

/** A mixin for the same reason as {@code FoodItemMixin}: cake is the one food that isn't an item. */
@Mixin(CakeBlock.class)
abstract class CakeBlockMixin {
	@WrapOperation(method = "tryEat", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;heal(I)V"))
	private void heartyfood$overTime(PlayerEntity player, int restored, Operation<Void> heal) {
		if (player.world.isRemote) heal.call(player, restored);
		else HeartyFood.eat(player, restored);
	}

	/**
	 * Beta won't start a slice at full health. A full bar is now also one on its way there, or holding the button would eat the whole cake for nothing. A client asks the same of what the server
	 * last told it; where the two disagree for a moment, or the client has no mod, the server sends the block back after any click on it.
	 */
	@ModifyExpressionValue(method = "tryEat", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;health:I"))
	private int heartyfood$countingWhatsPending(int health, @Local(argsOnly = true) PlayerEntity player) {
		return Math.max(health, HeartyFood.endsAt.get(player));
	}
}
