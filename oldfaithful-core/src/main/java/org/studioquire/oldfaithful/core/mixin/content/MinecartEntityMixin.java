package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.StackData;
import org.studioquire.oldfaithful.core.impl.content.StackDataHolder;

/** A chest cart spilling makes new stacks by hand, so their data goes with them. */
@Mixin(MinecartEntity.class)
abstract class MinecartEntityMixin {
	@ModifyExpressionValue(method = {"damage", "markDead"}, at = @At(value = "NEW", target = "Lnet/minecraft/item/ItemStack;"), require = 0)
	private ItemStack faithful$keepData(ItemStack spilled, @Local(ordinal = 0) ItemStack from) {
		if (from != spilled) ((StackDataHolder) (Object) spilled).faithful$setData(StackData.copy(((StackDataHolder) (Object) from).faithful$data()));
		return spilled;
	}
}
