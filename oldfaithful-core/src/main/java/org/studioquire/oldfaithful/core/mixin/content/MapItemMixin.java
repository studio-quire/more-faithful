package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.item.MapItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/** Maps count blocks in an {@code int[256]}: a mod's block counts as its stand-in, and a map shows the stand-in's colour. */
@Mixin(MapItem.class)
abstract class MapItemMixin {
	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getBlockId(III)I"))
	private int faithful$standIn(int id) {
		return Overlay.standId(id);
	}
}
