package org.studioquire.faithful.snipping.mixin;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.faithful.snipping.Snips;

/** Every change of a cell's block goes through these two, whatever made it: a snipped plant's mark goes with the plant. */
@Mixin(Chunk.class)
abstract class ChunkMixin {
	@Inject(method = "setBlock(IIIII)Z", at = @At("HEAD"))
	private void snipping$change(int x, int y, int z, int id, int meta, CallbackInfoReturnable<Boolean> cir) {
		Snips.changing((Chunk) (Object) this, x, y, z, id);
	}

	@Inject(method = "setBlock(IIII)Z", at = @At("HEAD"))
	private void snipping$change(int x, int y, int z, int id, CallbackInfoReturnable<Boolean> cir) {
		Snips.changing((Chunk) (Object) this, x, y, z, id);
	}
}
