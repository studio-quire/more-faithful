package org.studioquire.faithful.snipping.mixin;

import java.util.Random;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.snipping.Snips;

/** Beta's random tick, the one way its plants grow by themselves, passes a snipped one by. */
@Mixin(World.class)
abstract class WorldMixin {
	@WrapOperation(method = "manageChunkUpdatesAndEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onTick(Lnet/minecraft/world/World;IIILjava/util/Random;)V"))
	private void snipping$snipped(Block block, World world, int x, int y, int z, Random random, Operation<Void> tick) {
		if (Snips.stops(block, world, x, y, z)) Snips.stillTick(block, world, x, y, z);
		else tick.call(block, world, x, y, z, random);
	}
}
