package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.client.Pack;

/**
 * The title screen's edition line names the pack ({@link Pack}): Pure Faithful's hardening mod draws it, and an edition's mod
 * may rename it (Still Faithful's does), so this one comes after theirs (priority) and has the last word.
 */
@Pseudo
@Mixin(targets = "org.studioquire.oldfaithful.hardening.Branding", priority = 1100)
abstract class BrandingMixin {
	@ModifyReturnValue(method = "text", at = @At("RETURN"), require = 0, remap = false)
	private static String faithful$pack(String line) {
		return Pack.titleLine(line);
	}
}
