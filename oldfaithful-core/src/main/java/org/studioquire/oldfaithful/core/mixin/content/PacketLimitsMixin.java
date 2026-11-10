package org.studioquire.oldfaithful.core.mixin.content;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/**
 * Hardening's sanity bound on a chunk packet's compressed length comes from vanilla's layout; with the overlay there's more in it. Reached
 * from here, so Pure Faithful's released mod works as it is. Applied only if it's installed.
 */
@Pseudo
@Mixin(targets = "org.studioquire.oldfaithful.hardening.packet.PacketLimits")
abstract class PacketLimitsMixin {
	@ModifyVariable(method = "checkChunkData", at = @At("STORE"), ordinal = 0, require = 0, remap = false)
	private static long faithful$overlay(long raw) {
		return Wire.serverContent ? raw + raw / 5 * 4 : raw;
	}
}
