package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.PlayerRender;

@Mixin(BipedEntityModel.class)
abstract class BipedEntityModelMixin {
	@Inject(method = "setAngles", at = @At("TAIL"))
	private void faithful$posed(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
		if (PlayerRender.hasListeners()) PlayerRender.posed((BipedEntityModel) (Object) this);
	}
}
