package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.client.Keys;

/** Mods' keys join the list before {@code options.txt} is read, so what the player chose is read into them. */
@Mixin(GameOptions.class)
abstract class GameOptionsMixin {
	@Shadow
	public KeyBinding[] allKeys;

	@Inject(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;load()V"))
	private void faithful$keys(CallbackInfo ci) {
		allKeys = Keys.extend(allKeys);
	}
}
