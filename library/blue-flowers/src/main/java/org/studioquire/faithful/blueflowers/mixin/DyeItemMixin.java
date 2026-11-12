package org.studioquire.faithful.blueflowers.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.item.DyeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.blueflowers.BlueFlowers;

/**
 * Beta's blue dye drawn as the lupin's, from this mod's own sprite. Every dye is the one item, its sprite worked out from its damage in this method, and the core gives a sprite to a mod's own
 * items only, so this is where one damage of a vanilla item gets another.
 */
@Mixin(DyeItem.class)
abstract class DyeItemMixin {
	@ModifyReturnValue(method = "getTextureId(I)I", at = @At("RETURN"))
	private int blueflowers$lupinBlue(int sprite, int damage) {
		return damage == BlueFlowers.BLUE ? BlueFlowers.dyeSprite : sprite;
	}
}
