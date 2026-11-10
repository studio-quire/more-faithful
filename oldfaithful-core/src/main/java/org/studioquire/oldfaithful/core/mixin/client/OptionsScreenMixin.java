package org.studioquire.oldfaithful.core.mixin.client;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.client.Pack;

/** The settings button in Options names the pack ({@link Pack}); after Faithful Settings adds it and an edition's mod renames it. */
@Mixin(value = OptionsScreen.class, priority = 1200)
abstract class OptionsScreenMixin extends Screen {
	@Inject(method = "init", at = @At("TAIL"))
	private void faithful$packButton(CallbackInfo ci) {
		if (Pack.name() == null) return;
		for (Object o : (List<?>) this.buttons) {
			ButtonWidget b = (ButtonWidget) o;
			if (b.id == Pack.SETTINGS_BUTTON) b.text = Pack.name() + "...";
		}
	}
}
