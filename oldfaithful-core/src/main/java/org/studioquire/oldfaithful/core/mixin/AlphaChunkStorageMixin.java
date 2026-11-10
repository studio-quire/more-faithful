package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.impl.ChunkData;

/** Chunks' {@code Faithful} compound. The first read of the height map is the first line after the block arrays are in place. */
@Mixin(AlphaChunkStorage.class)
abstract class AlphaChunkStorageMixin {
	@Inject(method = "loadChunkFromNbt", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/chunk/Chunk;heightmap:[B", ordinal = 0))
	private static void faithful$load(World world, NbtCompound nbt, CallbackInfoReturnable<Chunk> cir, @Local Chunk chunk) {
		ChunkData.load(chunk, nbt);
	}

	@Inject(method = "saveChunkToNbt", at = @At("TAIL"))
	private static void faithful$save(Chunk chunk, World world, NbtCompound nbt, CallbackInfo ci) {
		ChunkData.save(chunk, nbt);
	}
}
