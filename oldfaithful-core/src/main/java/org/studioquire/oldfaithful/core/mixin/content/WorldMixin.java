package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/**
 * The two places a world reads a block by a byte: the random tick, straight from the chunk, and a masked id in setBlockMeta. Local
 * slots differ between the client's and the server's class here, so the tick notes the id as it reads the array.
 */
@Mixin(World.class)
abstract class WorldMixin {
	@Shadow
	public abstract int getBlockId(int x, int y, int z);

	/** The mod block the random tick just read, or -1. */
	@Unique
	private int faithful$tickId = -1;

	@Redirect(method = "manageChunkUpdatesAndEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", args = "array=get"))
	private byte faithful$tickRead(byte[] blocks, int cell, @Local Chunk chunk) {
		char[] ext = Overlay.of(chunk).faithful$ext();
		int c = ext == null ? 0 : ext[cell];
		faithful$tickId = c != 0 ? c & 0xFFF : -1;
		return blocks[cell];
	}

	@Redirect(method = "manageChunkUpdatesAndEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS_RANDOM_TICK:[Z", args = "array=get"))
	private boolean faithful$ticks(boolean[] table, int id) {
		return table[faithful$tickId >= 0 ? faithful$tickId : id];
	}

	@Redirect(method = "manageChunkUpdatesAndEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS:[Lnet/minecraft/block/Block;", args = "array=get", ordinal = 1))
	private Block faithful$ticked(Block[] table, int id) {
		return table[faithful$tickId >= 0 ? faithful$tickId : id];
	}

	@Redirect(method = "setBlockMeta", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS_IGNORE_META_UPDATE:[Z", args = "array=get"))
	private boolean faithful$ignoresMeta(boolean[] table, int masked, int x, int y, int z, int meta) {
		return table[getBlockId(x, y, z)];
	}
}
