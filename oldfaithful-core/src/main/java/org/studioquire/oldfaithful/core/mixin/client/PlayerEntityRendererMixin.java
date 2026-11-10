package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.studioquire.oldfaithful.core.api.client.PlayerRender;

/** The model doesn't know whose it is. Zombies share it, so only a player's draw says. */
@Mixin(PlayerEntityRenderer.class)
abstract class PlayerEntityRendererMixin {
	@WrapMethod(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V")
	private void faithful$drawing(PlayerEntity player, double x, double y, double z, float yaw, float tickDelta, Operation<Void> render) {
		if (PlayerRender.hasListeners()) PlayerRender.drawing(player, () -> render.call(player, x, y, z, yaw, tickDelta));
		else render.call(player, x, y, z, yaw, tickDelta);
	}
}
