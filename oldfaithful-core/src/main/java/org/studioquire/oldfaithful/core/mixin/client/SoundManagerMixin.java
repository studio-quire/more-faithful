package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.client.ModSounds;

/** Mods' sounds beside vanilla's, which arrive file by file from the resources folder. */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Shadow
	private SoundEntry sounds;
	@Shadow
	private SoundEntry streamingSounds;
	@Shadow
	private SoundEntry music;

	@Inject(method = "loadSounds", at = @At("TAIL"))
	private void faithful$modSounds(GameOptions options, CallbackInfo ci) {
		ModSounds.load(sounds, streamingSounds, music);
	}
}
