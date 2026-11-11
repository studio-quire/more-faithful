package org.studioquire.faithful.featherlure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.studioquire.faithful.featherlure.FeatherLure;
import org.studioquire.faithful.featherlure.client.FeatherLureClient;

/**
 * Client: a bobber on a lured line is drawn with a small feather over its hook, the mod's own picture of beta's bobber on the quad beta draws. The core has no hook for how
 * a vanilla entity looks.
 */
@Mixin(FishingBobberEntityRenderer.class)
abstract class FishingBobberEntityRendererMixin {
	@ModifyArg(method = "render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/FishingBobberEntityRenderer;bindTexture(Ljava/lang/String;)V"))
	private String featherlure$sheet(String path, @Local(argsOnly = true) FishingBobberEntity bobber) {
		return FeatherLure.looksLured(bobber) ? FeatherLureClient.LURED_BOBBER : path;
	}

	/** Beta's corners are tile (1, 2)'s on {@code particles.png}, 16 tiles across; the feathered bobber is that tile alone. */
	@WrapOperation(method = "render(Lnet/minecraft/entity/projectile/FishingBobberEntity;DDDFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;vertex(DDDDD)V"))
	private void featherlure$corner(Tessellator tessellator, double x, double y, double z, double u, double v, Operation<Void> vertex,
			@Local(argsOnly = true) FishingBobberEntity bobber) {
		if (FeatherLure.looksLured(bobber)) {
			u = u * 16.0 - 1.0;
			v = v * 16.0 - 2.0;
		}
		vertex.call(tessellator, x, y, z, u, v);
	}
}
