package org.studioquire.faithful.drygrass.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.color.world.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.studioquire.faithful.drygrass.client.DryLand;

/** As {@link GrassColorsMixin}, for beta's leaf map, which only oak leaves read: birch and spruce have a fixed colour each. */
@Mixin(FoliageColors.class)
abstract class FoliageColorsMixin {
	@WrapMethod(method = "getColor")
	private static int drygrass$tint(double temperature, double humidity, Operation<Integer> original) {
		return DryLand.foliage(temperature, humidity, original.call(temperature, humidity));
	}
}
