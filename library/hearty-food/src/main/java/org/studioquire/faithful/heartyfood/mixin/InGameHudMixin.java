package org.studioquire.faithful.heartyfood.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.heartyfood.client.GhostHearts;

/**
 * A mixin because a ghost heart belongs inside one heart slot of beta's loop: right after that slot's empty heart, at exactly the x and y beta gave it (the jitter at low health is a random
 * number drawn for that slot, there), and under the flash and the red. {@code Hud} only offers corners.
 */
@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Shadow
	private Minecraft minecraft;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V"))
	private void heartyfood$afterTheEmptyHeart(InGameHud hud, int x, int y, int u, int v, int width, int height, Operation<Void> draw) {
		draw.call(hud, x, y, u, v, width, height);
		if (GhostHearts.isEmptyHeart(u, v, width)) GhostHearts.draw(minecraft, hud, x, y);
	}
}
