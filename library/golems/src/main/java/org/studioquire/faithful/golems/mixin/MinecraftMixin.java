package org.studioquire.faithful.golems.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.client.HeldClick;

/**
 * A right click that hands a golem a tool, takes its tool back or tells it to follow does that and nothing more until the button is let go. Beta goes on after any right click on a
 * mob to use what's in the hand, and repeats the whole click every 5 ticks while the button is held, so the bow just taken back was loosed at the golem by the same click (single
 * player, where the golem's answer is in the hand by then) or the repeat handed it straight back or used it (anywhere). The core has no hook on beta's right click, so a mixin.
 */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Shadow
	public HitResult crosshairTarget;
	@Shadow
	public ClientPlayerEntity player;
	@Unique
	private boolean golems$exchange;

	@Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
	private void golems$heldAfterExchange(int button, CallbackInfo ci) {
		if (button == 1 && HeldClick.latched) ci.cancel();
	}

	@Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/InteractionManager;interactEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
	private void golems$beforeGolem(int button, CallbackInfo ci) {
		golems$exchange = crosshairTarget.entity instanceof GolemEntity && ((GolemEntity) crosshairTarget.entity).exchanges(player.getHand());
	}

	@Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/InteractionManager;interactEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER), cancellable = true)
	private void golems$afterGolem(int button, CallbackInfo ci) {
		if (!golems$exchange) return;
		golems$exchange = false;
		HeldClick.latched = true;
		ci.cancel();
	}
}
