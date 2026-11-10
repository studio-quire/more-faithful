package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/** A pack switch: the atlases again, under the same GL names. */
@Mixin(TextureManager.class)
abstract class TextureManagerMixin {
	@Inject(method = "reload", at = @At("TAIL"))
	private void faithful$atlases(CallbackInfo ci) {
		ClientAtlases.reload((TextureManager) (Object) this);
	}
}
