package org.studioquire.faithful.harness.mixin;

import java.util.Random;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The arena's worlds are compared cell by cell to see what saving does, and an arena is no place for a block to live: a crop on stone or a leaf with no log goes in its
 * next random tick. So there no block ticks, and a cell that's changed was changed by the save.
 */
@Mixin(World.class)
abstract class WorldFrozenMixin {
	private static final boolean FROZEN = Boolean.getBoolean("faithful.harness.frozen");

	@WrapOperation(method = {"manageChunkUpdatesAndEvents", "processScheduledTicks"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"))
	private void harness$blockTick(Block block, World world, int x, int y, int z, Random random, Operation<Void> tick) {
		if (!FROZEN) tick.call(block, world, x, y, z, random);
	}
}
