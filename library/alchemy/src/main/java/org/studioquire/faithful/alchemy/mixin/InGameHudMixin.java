package org.studioquire.faithful.alchemy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.alchemy.client.EffectsHud;

/**
 * The ward's blue pips. A mixin because they belong inside beta's own armour bar's loop, at the slots beta draws: the bar counts the blue, so beta draws it with no armour on too. {@code Hud} only
 * offers corners.
 */
@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	@Shadow
	private Minecraft minecraft;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;getTotalArmorDurability()I"))
	private int alchemy$barWithTheBlue(ClientPlayerEntity player, Operation<Integer> armour) {
		return EffectsHud.bar(minecraft, armour.call(player));
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V"))
	private void alchemy$blueSlots(InGameHud hud, int x, int y, int u, int v, int width, int height, Operation<Void> draw) {
		if (!EffectsHud.armourSlot(hud, x, y, u, v, width, height, draw)) draw.call(hud, x, y, u, v, width, height);
	}
}
