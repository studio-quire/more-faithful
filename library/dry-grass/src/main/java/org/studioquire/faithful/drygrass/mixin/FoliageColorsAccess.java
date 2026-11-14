package org.studioquire.faithful.drygrass.mixin;

import net.minecraft.client.color.world.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The map the game looks oak leaves up in, {@code /misc/foliagecolor.png}, as {@link GrassColorsAccess}. */
@Mixin(FoliageColors.class)
public interface FoliageColorsAccess {
	@Accessor("colorMap")
	static int[] drygrass$map() {
		throw new AssertionError();
	}
}
