package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/** A particle batch has one texture bound: a tile from a mod's atlas flushes, binds, and flushes back. */
@Mixin(ParticleManager.class)
abstract class ParticleManagerMixin {
	@Unique
	private int faithful$atlas;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lnet/minecraft/client/render/Tessellator;FFFFFF)V"))
	private void faithful$gate(Particle particle, Tessellator t, float delta, float a, float b, float c, float d, float e, Operation<Void> render) {
		int atlas = ((ParticleAccessor) particle).faithful$textureId() >> 8;
		if (atlas != faithful$atlas) {
			int bound = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			t.draw();
			((TessellatorAccessor) t).faithful$setDrawing(true);
			if (faithful$atlas == 0) faithful$vanilla = bound;
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, ClientAtlases.name(atlas, faithful$vanilla));
			faithful$atlas = atlas;
		}
		render.call(particle, t, delta, a, b, c, d, e);
	}

	@Unique
	private int faithful$vanilla;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V", shift = At.Shift.AFTER))
	private void faithful$end(Entity entity, float delta, CallbackInfo ci) {
		faithful$atlas = 0;
	}
}
