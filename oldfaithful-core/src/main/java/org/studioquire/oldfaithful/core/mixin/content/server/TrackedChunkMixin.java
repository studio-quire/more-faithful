package org.studioquire.oldfaithful.core.mixin.content.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.io.PrintStream;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * When 2 to 9 blocks of a chunk change in a tick, the loop looking for their block entities reads each position from the change count,
 * not the change, so it finds nothing. Here the three reads get the change. {@code oldfaithful-fixes} has its own answer to the same
 * bug (#48), which empties this loop, so with it installed these never run and nothing is sent twice.
 */
@Mixin(targets = "net.minecraft.server.ChunkMap$TrackedChunk")
abstract class TrackedChunkMixin {
	@Shadow
	private short[] dirtyBlocks;

	@ModifyExpressionValue(method = "updateChunk",
			at = {@At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/ChunkMap$TrackedChunk;dirtyBlockCount:I", ordinal = 1),
					@At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/ChunkMap$TrackedChunk;dirtyBlockCount:I", ordinal = 2),
					@At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/ChunkMap$TrackedChunk;dirtyBlockCount:I", ordinal = 3)},
			slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkDeltaUpdateS2CPacket;<init>(II[SILnet/minecraft/world/World;)V")))
	private int faithful$theChange(int count, @Local(ordinal = 0) int i) {
		return dirtyBlocks[i];
	}

	/** The loop's leftover print, now that it runs. */
	@WrapOperation(method = "updateChunk", at = @At(value = "INVOKE", target = "Ljava/io/PrintStream;println(Ljava/lang/String;)V"))
	private void faithful$quiet(PrintStream out, String line, Operation<Void> println) {
	}
}
