package org.studioquire.faithful.heartyfood.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.heartyfood.HeartyFood;

/**
 * A mixin because this is the one place that knows a heal is food: {@code heal} is also a wolf's dinner, Peaceful's slow healing and anyone's potion. Stew, cookies and any mod's food that
 * calls {@code super.use} all come through this line. On a client on a server {@code heal} does nothing anyway, and the server has the last word.
 */
@Mixin(FoodItem.class)
abstract class FoodItemMixin {
	@WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;heal(I)V"))
	private void heartyfood$overTime(PlayerEntity user, int restored, Operation<Void> heal) {
		if (user.world.isRemote) heal.call(user, restored);
		else HeartyFood.eat(user, restored);
	}
}
