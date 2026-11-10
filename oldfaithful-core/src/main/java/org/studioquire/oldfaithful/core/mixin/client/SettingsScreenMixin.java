package org.studioquire.oldfaithful.core.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.client.Pack;

/** The settings screen is titled with the pack's name ({@link Pack}), after an edition's mod titles it; its group pages keep their titles. */
@Pseudo
@Mixin(targets = "org.studioquire.oldfaithful.options.settings.SettingsScreen", priority = 1100)
abstract class SettingsScreenMixin {
	@Shadow(remap = false)
	protected String title;

	@Inject(method = "<init>", at = @At("RETURN"), require = 0, remap = false)
	private void faithful$packTitle(CallbackInfo ci) {
		if (Pack.name() != null) this.title = Pack.name();
	}
}
