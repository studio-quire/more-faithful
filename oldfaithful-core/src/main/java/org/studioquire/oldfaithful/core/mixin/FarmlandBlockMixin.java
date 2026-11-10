package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Growth;

/** What dry farmland stays farmland under. */
@Mixin(FarmlandBlock.class)
abstract class FarmlandBlockMixin {
	@ModifyReturnValue(method = "hasCrop", at = @At("RETURN"))
	private boolean faithful$holds(boolean wheat, @Local(argsOnly = true) World world, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		return wheat || Growth.hasFarmlandCrops() && Growth.holdsFarmland(world.getBlockId(x, y + 1, z));
	}
}
