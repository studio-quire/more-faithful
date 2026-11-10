package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/** The atlases, once the texture manager and pack exist. */
@Mixin(Minecraft.class)
abstract class MinecraftAtlasMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void faithful$atlases(CallbackInfo ci) {
		ClientAtlases.load((Minecraft) (Object) this);
	}
}
