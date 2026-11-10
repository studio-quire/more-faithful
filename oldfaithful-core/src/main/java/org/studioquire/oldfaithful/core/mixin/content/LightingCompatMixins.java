package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/**
 * Old Faithful's lighting reads {@code blocks} directly, which is a stand-in under a mod's block: it gets the real block's light here, so the
 * edition's mod doesn't change. Applied only if that mod is installed.
 */
final class LightingCompatMixins {
	private LightingCompatMixins() {
	}

	@Pseudo
	@Mixin(targets = "org.studioquire.oldfaithful.lighting.Relight")
	abstract static class Rule {
		@Redirect(method = "wanted", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", args = "array=get"), require = 0)
		private static byte faithful$real(byte[] blocks, int cell, @Local(argsOnly = true, ordinal = 0) Chunk chunk) {
			return Overlay.lightCell(Overlay.of(chunk).faithful$ext(), blocks, cell);
		}
	}

	@Pseudo
	@Mixin(targets = "org.studioquire.oldfaithful.lighting.Spread")
	abstract static class Step {
		@Redirect(method = "step", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", args = "array=get"), require = 0)
		private byte faithful$real(byte[] blocks, int cell, @Local(argsOnly = true) Chunk chunk) {
			return Overlay.lightCell(Overlay.of(chunk).faithful$ext(), blocks, cell);
		}
	}
}
