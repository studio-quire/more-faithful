package org.studioquire.faithful.featherlure.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.featherlure.FeatherLure;

/**
 * Client: beta draws a fishing rod in the hand as its cast rod, the tile below the rod's in {@code items.png}, while the player's line is out. The feather goes out with
 * the line, so a lured rod cast is beta's cast rod too, from the player's own sheet.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
	@ModifyReturnValue(method = "getItemStackTextureId", at = @At("RETURN"))
	private int featherlure$cast(int texture, ItemStack stack) {
		if (stack.itemId != FeatherLure.luredRod.get().id || ((PlayerEntity) (Object) this).fishHook == null) return texture;
		return Item.FISHING_ROD.getTextureId(0) + 16;
	}
}
