package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/**
 * The atlas gate, per block and outside Still Faithful's dusk wrap (the high priority makes this the outer one): dusk takes any draw
 * during a block's real pass as a mistake. When a block's atlas differs from the one bound, the tessellator flushes as it does when full, and
 * the bind compiles into the chunk's display list. {@code terrain.png} is bound back before the list ends, so nothing leaks to the next chunk.
 */
@Mixin(value = ChunkBuilder.class, priority = 2000)
abstract class ChunkBuilderMixin {
	@Unique
	private int faithful$atlas;

	@Inject(method = "rebuild", at = @At("HEAD"))
	private void faithful$start(CallbackInfo ci) {
		faithful$atlas = 0;
	}

	@WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockRenderManager;render(Lnet/minecraft/block/Block;III)Z"))
	private boolean faithful$gate(BlockRenderManager renderer, Block block, int x, int y, int z, Operation<Boolean> render) {
		int atlas = ContentRegistry.BLOCK_ATLAS[block.id];
		if (atlas != faithful$atlas) {
			Tessellator t = Tessellator.INSTANCE;
			t.draw();
			((TessellatorAccessor) t).faithful$setDrawing(true);
			if (atlas == 0) ClientAtlases.bindTerrain();
			else ClientAtlases.bind(atlas);
			faithful$atlas = atlas;
		}
		return render.call(renderer, block, x, y, z);
	}

	@Inject(method = "rebuild", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V"))
	private void faithful$end(CallbackInfo ci) {
		if (faithful$atlas != 0) ClientAtlases.bindTerrain();
		faithful$atlas = 0;
	}
}
