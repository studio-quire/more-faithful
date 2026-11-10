package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;

/**
 * {@code item < 256} means "is a block", and a mod's block item is one too. Sprites from a mod's atlas are bound after vanilla's choice; their
 * UVs wrap past 1.0 by the index's top bits, which the atlas's GL_REPEAT takes care of.
 */
@Mixin(ItemRenderer.class)
abstract class ItemRendererMixin {
	@ModifyConstant(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", constant = @Constant(intValue = 256))
	private int faithful$droppedIsBlock(int limit, @Local ItemStack stack) {
		return ContentRegistry.isBlockItem(stack.itemId) ? Integer.MAX_VALUE : limit;
	}

	@Inject(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/Tessellator;INSTANCE:Lnet/minecraft/client/render/Tessellator;"))
	private void faithful$droppedSprite(ItemEntity entity, double x, double y, double z, float yaw, float delta, CallbackInfo ci, @Local ItemStack stack) {
		ClientAtlases.bind(stack.getTextureId() >> 8);
	}

	@ModifyConstant(method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V", constant = @Constant(intValue = 256))
	private int faithful$slotIsBlock(int limit, @Local(argsOnly = true, ordinal = 0) int item) {
		return ContentRegistry.isBlockItem(item) ? Integer.MAX_VALUE : limit;
	}

	@ModifyArg(method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(I)V"))
	private int faithful$slotSprite(int name, @Local(argsOnly = true, ordinal = 2) int sprite) {
		return ClientAtlases.name(sprite >> 8, name);
	}
}
