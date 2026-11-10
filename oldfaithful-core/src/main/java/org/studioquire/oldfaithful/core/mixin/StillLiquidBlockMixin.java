package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.StillLiquidBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Fire;

/** Still lava's random tick, the one place lava lights a fire. */
@Mixin(StillLiquidBlock.class)
abstract class StillLiquidBlockMixin {
	@WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$ignite(World world, int x, int y, int z, int block, Operation<Boolean> set) {
		return Fire.ignites(world, x, y, z, Fire.Cause.LAVA, null) && set.call(world, x, y, z, block);
	}
}
