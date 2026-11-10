package org.studioquire.faithful.plainf3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A mixin because what's taken out is single lines of beta's debug screen, which no mod but this one would want a hook for. Position and facing go, and the two lines that count entities, which
 * tell you when something's near. The frame rate, chunk counts and memory stay, with the chunk cache's line moved up into the gap.
 */
@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Shadow
	private Minecraft minecraft;

	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
	private boolean plainf3$noPosition(InGameHud hud, TextRenderer font, String text, int x, int y, int color) {
		return !(text.startsWith("x: ") || text.startsWith("y: ") || text.startsWith("z: ") || text.startsWith("f: "));
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderEntityDebugInfo()Ljava/lang/String;"))
	private String plainf3$chunkCacheInstead(String entities) {
		return minecraft.getChunkSourceDebugInfo();
	}

	@ModifyExpressionValue(method = "render", at = {
		@At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getWorldDebugInfo()Ljava/lang/String;"),
		@At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getChunkSourceDebugInfo()Ljava/lang/String;")
	})
	private String plainf3$blank(String line) {
		return "";
	}
}
