package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.block.Block;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.entity.PistonBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/** A mod's block being pushed draws from its atlas. */
@Mixin(PistonBlockEntityRenderer.class)
abstract class PistonBlockEntityRendererMixin {
	@Inject(method = "render(Lnet/minecraft/block/entity/PistonBlockEntity;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;startQuads()V"))
	private void faithful$bind(PistonBlockEntity entity, double x, double y, double z, float delta, CallbackInfo ci) {
		ClientAtlases.bind(ContentRegistry.BLOCK_ATLAS[entity.getPushedBlockId()]);
	}

	@Inject(method = "render(Lnet/minecraft/block/entity/PistonBlockEntity;DDDF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V", shift = At.Shift.AFTER))
	private void faithful$restore(PistonBlockEntity entity, double x, double y, double z, float delta, CallbackInfo ci) {
		if (ContentRegistry.BLOCK_ATLAS[entity.getPushedBlockId()] != 0) ClientAtlases.bindTerrain();
	}
}
