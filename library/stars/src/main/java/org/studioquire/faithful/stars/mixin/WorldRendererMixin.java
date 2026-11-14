package org.studioquire.faithful.stars.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.stars.client.StarList;
import org.studioquire.faithful.stars.client.Stars;

/**
 * A mixin, as the stars are beta's own display list, recorded once as the world renderer is made and drawn inside renderSky, where no hook reaches: the core's sky hook
 * comes after the sky is drawn. Each change is to a number beta's own code goes on using (how many tries at placing a star, the half-width each is drawn with, the light
 * it's drawn in), so beta's stars stay where they are, and with every setting at Beta each number is beta's own, to the bit. A changed size or number records the list
 * again, by beta's own method, as the sky is next drawn: on the game's thread, before anything of the frame's sky is begun.
 */
@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin implements StarList {
	@Shadow
	private int starsGlList;

	/** The shares of beta's half-width and tries the list was recorded with. */
	@Unique
	private float stars$size, stars$number;

	@Shadow
	private void renderStars() {
	}

	@Inject(method = "renderStars", at = @At("HEAD"))
	private void stars$recording(CallbackInfo ci) {
		stars$size = Stars.size;
		stars$number = Stars.number;
	}

	@ModifyExpressionValue(method = "renderStars", at = @At(value = "CONSTANT", args = "intValue=1500"))
	private int stars$tries(int tries) {
		return Math.round(tries * stars$number);
	}

	/** Both quarters of {@code 0.25F + random.nextFloat() * 0.25F}, so the half-width scales and the draw is beta's. */
	@ModifyExpressionValue(method = "renderStars", at = @At(value = "CONSTANT", args = "floatValue=0.25F"), require = 2, allow = 2)
	private float stars$halfWidth(float quarter) {
		return quarter * stars$size;
	}

	@Inject(method = "renderSky", at = @At("HEAD"))
	private void stars$current(float tickDelta, CallbackInfo ci) {
		if (stars$size == Stars.size && stars$number == Stars.number) return;
		GL11.glNewList(starsGlList, GL11.GL_COMPILE);
		renderStars();
		GL11.glEndList();
	}

	/** The sky's darkness, which beta draws the stars in (colour and alpha) and skips them at when it's none. */
	@ModifyExpressionValue(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;calculateSkyLightIntensity(F)F"))
	private float stars$light(float darkness) {
		return darkness * Stars.light;
	}

	@Override
	public float stars$drawnSize() {
		return stars$size;
	}

	@Override
	public float stars$drawnNumber() {
		return stars$number;
	}
}
