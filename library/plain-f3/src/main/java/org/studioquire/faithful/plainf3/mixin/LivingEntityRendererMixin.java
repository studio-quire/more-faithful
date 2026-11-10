package org.studioquire.faithful.plainf3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** With the debug screen up, beta tags every mob within 64 blocks with its number, drawn through walls. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
	@ModifyExpressionValue(method = "renderNameTag(Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isDebugProfilerEnabled()Z"))
	private boolean plainf3$noMobTags(boolean debug) {
		return false;
	}
}
