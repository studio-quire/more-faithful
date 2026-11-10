package org.studioquire.faithful.alchemy.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.alchemy.client.Bottles;

/**
 * The timed potions' bottles on the inventory's panel. A mixin because the core has no hook for drawing on a vanilla screen. At the end of its background, after the figure, so the slots, a held
 * stack and a tooltip all come over them, as they come over the panel.
 */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenMixin extends HandledScreen {
	private InventoryScreenMixin(ScreenHandler handler) {
		super(handler);
	}

	@Inject(method = "drawBackground", at = @At("TAIL"))
	private void alchemy$timedBottles(float tickDelta, CallbackInfo info) {
		Bottles.draw(minecraft, this, (width - backgroundWidth) / 2, (height - backgroundHeight) / 2);
	}
}
