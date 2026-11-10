package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.gen.chunk.NetherChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.SkyChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.impl.content.Features;

/** Where a chunk's cells are final, and where a chunk has been decorated, in each of the three generators. */
final class ChunkGeneratorMixins {
	private ChunkGeneratorMixins() {
	}

	@Mixin(OverworldChunkGenerator.class)
	abstract static class Overworld {
		@Shadow
		private World world;

		@Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;populateHeightMap()V"))
		private void faithful$built(int x, int z, CallbackInfoReturnable<Chunk> cir, @Local Chunk chunk) {
			Features.built(world, chunk);
		}

		@Inject(method = "decorate", at = @At("TAIL"))
		private void faithful$decorate(ChunkSource source, int x, int z, CallbackInfo ci) {
			Features.decorate(world, x, z);
		}
	}

	@Mixin(SkyChunkGenerator.class)
	abstract static class Sky {
		@Shadow
		private World world;

		@Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;populateHeightMap()V"))
		private void faithful$built(int x, int z, CallbackInfoReturnable<Chunk> cir, @Local Chunk chunk) {
			Features.built(world, chunk);
		}

		@Inject(method = "decorate", at = @At("TAIL"))
		private void faithful$decorate(ChunkSource source, int x, int z, CallbackInfo ci) {
			Features.decorate(world, x, z);
		}
	}

	/** The Nether makes its chunk last, with no height map to wait for. */
	@Mixin(NetherChunkGenerator.class)
	abstract static class Nether {
		@Shadow
		private World world;

		@ModifyReturnValue(method = "getChunk", at = @At("RETURN"))
		private Chunk faithful$built(Chunk chunk) {
			Features.built(world, chunk);
			return chunk;
		}

		@Inject(method = "decorate", at = @At("TAIL"))
		private void faithful$decorate(ChunkSource source, int x, int z, CallbackInfo ci) {
			Features.decorate(world, x, z);
		}
	}
}
