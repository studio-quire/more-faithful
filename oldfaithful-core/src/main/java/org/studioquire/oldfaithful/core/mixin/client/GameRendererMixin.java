package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.Camera;
import org.studioquire.oldfaithful.core.api.client.WorldRender;

/** The frame's stages, from outside the world renderer's own methods, so a mod's drawing stays clear of Still Faithful's dusk wrap. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Shadow
	private Minecraft client;

	@Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(F)V", shift = At.Shift.AFTER))
	private void faithful$sky(float tickDelta, long time, CallbackInfo ci) {
		WorldRender.sky(client, tickDelta);
	}

	@Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderEntities(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/Culler;F)V", shift = At.Shift.AFTER))
	private void faithful$entities(float tickDelta, long time, CallbackInfo ci) {
		WorldRender.entities(client, tickDelta);
	}

	@Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(F)V", shift = At.Shift.AFTER))
	private void faithful$world(float tickDelta, long time, CallbackInfo ci) {
		WorldRender.world(client, tickDelta);
	}

	@ModifyReturnValue(method = "getFov", at = @At("RETURN"))
	private float faithful$fov(float fov, float tickDelta) {
		return Camera.fov(tickDelta, fov);
	}

	@Inject(method = "applyCameraTransform", at = @At("HEAD"))
	private void faithful$roll(float tickDelta, CallbackInfo ci) {
		float roll = Camera.roll(tickDelta);
		if (roll != 0.0F) GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
	}
}
