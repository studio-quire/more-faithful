package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.block.Block;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.llamalad7.mixinextras.sugar.Local;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/** The hand, as the item renderer; and the screen overlay inside a mod's block. */
@Mixin(HeldItemRenderer.class)
abstract class HeldItemRendererMixin {
	@ModifyConstant(method = "renderItem", constant = @Constant(intValue = 256))
	private int faithful$isBlock(int limit, @Local(argsOnly = true) ItemStack stack) {
		return ContentRegistry.isBlockItem(stack.itemId) ? Integer.MAX_VALUE : limit;
	}

	/**
	 * The atlas of the tile the hand draws, which is the holder's choice (beta's fishing rod shows its cast tile while the line is out) and can be another atlas than the
	 * stack's own: a mod's rod cast is vanilla's cast rod.
	 */
	@ModifyArg(method = "renderItem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", ordinal = 1), index = 1)
	private int faithful$blockSprite(int name, @Local(argsOnly = true) LivingEntity holder, @Local(argsOnly = true) ItemStack stack) {
		return ClientAtlases.name(holder.getItemStackTextureId(stack) >> 8, name);
	}

	@ModifyArg(method = "renderItem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V", ordinal = 2), index = 1)
	private int faithful$itemSprite(int name, @Local(argsOnly = true) LivingEntity holder, @Local(argsOnly = true) ItemStack stack) {
		return ClientAtlases.name(holder.getItemStackTextureId(stack) >> 8, name);
	}

	/** Inside a mod's block: its stand-in's tile would be right for vanilla's sheet, and that's what's bound here. */
	@ModifyArg(method = "renderScreenOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderTexturedOverlay(FI)V", ordinal = 0), index = 1)
	private int faithful$wallOverlay(int texture) {
		return texture < 256 ? texture : Block.STONE.textureId;
	}
}
