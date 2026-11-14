package org.studioquire.faithful.drygrass.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.color.world.GrassColors;
import org.spongepowered.asm.mixin.Mixin;
import org.studioquire.faithful.drygrass.client.DryLand;

/**
 * A mixin because the core has no hook for a block's tint in the world. At the colour map's lookup rather than on the blocks, so the
 * grass block, its fancy side, tall grass and ferns, and any mod's block that asks the map, all get the same colour. A mod wrapping a
 * block's {@code getColorMultiplier} receives this colour as beta's and works on from it.
 */
@Mixin(GrassColors.class)
abstract class GrassColorsMixin {
	@WrapMethod(method = "getColor")
	private static int drygrass$tint(double temperature, double humidity, Operation<Integer> original) {
		return DryLand.grass(temperature, humidity, original.call(temperature, humidity));
	}
}
