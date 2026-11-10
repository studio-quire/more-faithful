package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;
import org.studioquire.oldfaithful.core.impl.content.client.ClientShapes;
import org.studioquire.oldfaithful.core.impl.content.client.ShapeAccess;

/**
 * Blocks drawn one at a time, outside a chunk: in hands, on the ground, in slots, falling. What's bound can't be tracked here (others call
 * glBindTexture directly), so a mod block always binds before it draws and puts {@code terrain.png} back. And mods' render types, which vanilla's
 * chains of numbers pass through having drawn nothing.
 */
@Mixin(BlockRenderManager.class)
abstract class BlockRenderManagerMixin implements ShapeAccess {
	@Shadow
	private BlockView blockView;
	@Shadow
	private int textureOverride;

	@Override
	public int faithful$textureOverride() {
		return textureOverride;
	}

	@ModifyReturnValue(method = "render(Lnet/minecraft/block/Block;III)Z", at = @At("RETURN"))
	private boolean faithful$shape(boolean drawn, Block block, int x, int y, int z) {
		int type = block.getRenderType();
		return ClientShapes.ours(type) ? ClientShapes.render((BlockRenderManager) (Object) this, blockView, block, type, x, y, z) : drawn;
	}

	@ModifyReturnValue(method = "isSideLit", at = @At("RETURN"))
	private static boolean faithful$item3d(boolean in3d, int type) {
		return ClientShapes.ours(type) ? ClientShapes.inventoryIn3d(type) : in3d;
	}

	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At("HEAD"))
	private void faithful$bind(Block block, int meta, float brightness, CallbackInfo ci) {
		ClientAtlases.bind(ContentRegistry.BLOCK_ATLAS[block.id]);
	}

	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At("TAIL"))
	private void faithful$restore(Block block, int meta, float brightness, CallbackInfo ci) {
		int type = block.getRenderType();
		if (ClientShapes.ours(type)) ClientShapes.renderItem((BlockRenderManager) (Object) this, block, type, meta, brightness);
		if (ContentRegistry.BLOCK_ATLAS[block.id] != 0) ClientAtlases.bindTerrain();
	}

	@Inject(method = "renderFallingBlockEntity", at = @At("HEAD"))
	private void faithful$bindFalling(Block block, World world, int x, int y, int z, CallbackInfo ci) {
		ClientAtlases.bind(ContentRegistry.BLOCK_ATLAS[block.id]);
	}

	@Inject(method = "renderFallingBlockEntity", at = @At("TAIL"))
	private void faithful$restoreFalling(Block block, World world, int x, int y, int z, CallbackInfo ci) {
		if (ContentRegistry.BLOCK_ATLAS[block.id] != 0) ClientAtlases.bindTerrain();
	}
}
