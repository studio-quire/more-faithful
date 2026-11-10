package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.client.RuleSettings;

/** World Rules... below Cancel, a row beta leaves free at every GUI scale. What's chosen is what new worlds start with. */
@Mixin(CreateWorldScreen.class)
abstract class CreateWorldScreenMixin extends Screen {
	@Unique
	private static final int ID = 7072;

	@Inject(method = "init", at = @At("TAIL"))
	private void faithful$rulesButton(CallbackInfo ci) {
		if (!Rules.all().isEmpty()) buttons.add(new ButtonWidget(ID, width / 2 - 100, height / 4 + 144 + 12, "World Rules..."));
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
	private void faithful$openRules(ButtonWidget button, CallbackInfo ci) {
		if (button.id != ID || !button.active) return;
		Screen rules = RuleSettings.screen(this);
		if (rules != null) minecraft.setScreen(rules);
		ci.cancel();
	}
}
