package org.studioquire.faithful.wildsaplings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Random;
import net.minecraft.block.SaplingBlock;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.wildsaplings.WildSaplings;

/**
 * A mixin because the core has no say in what a sapling grows. Beta has chosen its tree and cleared the sapling by this call, so the tree it chose is
 * how the sapling's kind is known. Random ticks and bonemeal both grow through here.
 */
@Mixin(SaplingBlock.class)
abstract class SaplingBlockMixin {
	@WrapOperation(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/Feature;generate(Lnet/minecraft/world/World;Ljava/util/Random;III)Z"))
	private boolean wildsaplings$growWild(Feature tree, World world, Random random, int x, int y, int z, Operation<Boolean> generate) {
		return generate.call(WildSaplings.wild(tree, world, random, x, z), world, random, x, y, z);
	}
}
