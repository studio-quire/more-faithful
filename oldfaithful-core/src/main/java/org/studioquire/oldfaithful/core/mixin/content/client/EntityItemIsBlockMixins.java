package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.UndeadEntityRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** {@code id < 256} means "is a block" in what players and zombies hold and wear. */
final class EntityItemIsBlockMixins {
	private EntityItemIsBlockMixins() {
	}

	@Mixin(PlayerEntityRenderer.class)
	abstract static class Player {
		private static final String RENDER_MORE = "renderMore(Lnet/minecraft/entity/player/PlayerEntity;F)V";

		@ModifyConstant(method = RENDER_MORE, constant = @Constant(intValue = 256, ordinal = 0))
		private int faithful$wornIsBlock(int limit, @Local(ordinal = 0) ItemStack worn) {
			return ContentRegistry.isBlockItem(worn.itemId) ? Integer.MAX_VALUE : limit;
		}

		@ModifyConstant(method = RENDER_MORE, constant = @Constant(intValue = 256, ordinal = 1))
		private int faithful$heldIsBlock(int limit, @Local(ordinal = 1) ItemStack held) {
			return ContentRegistry.isBlockItem(held.itemId) ? Integer.MAX_VALUE : limit;
		}
	}

	@Mixin(UndeadEntityRenderer.class)
	abstract static class Undead {
		@ModifyConstant(method = "renderMore", constant = @Constant(intValue = 256))
		private int faithful$isBlock(int limit, @Local ItemStack stack) {
			return ContentRegistry.isBlockItem(stack.itemId) ? Integer.MAX_VALUE : limit;
		}
	}
}
