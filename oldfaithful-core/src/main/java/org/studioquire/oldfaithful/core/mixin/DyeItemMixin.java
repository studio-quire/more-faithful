package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Growth;

/** Bonemeal, before vanilla's list of saplings, wheat and grass. */
@Mixin(DyeItem.class)
abstract class DyeItemMixin {
	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void faithful$bonemeal(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (stack.getDamage() != 15) return;
		Growth.Bonemeal event = Growth.bonemeal(world, player, stack, x, y, z);
		if (event == null) return;
		if (event.cancelled()) {
			cir.setReturnValue(false);
		} else if (event.isHandled()) {
			stack.count--;
			cir.setReturnValue(true);
		}
	}
}
