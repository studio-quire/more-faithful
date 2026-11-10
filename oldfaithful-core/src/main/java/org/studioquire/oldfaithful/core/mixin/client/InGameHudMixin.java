package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.Hud;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Shadow
	private Minecraft minecraft;

	@Inject(method = "render", at = @At("TAIL"))
	private void faithful$layers(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		Hud.render(minecraft, tickDelta);
	}
}
