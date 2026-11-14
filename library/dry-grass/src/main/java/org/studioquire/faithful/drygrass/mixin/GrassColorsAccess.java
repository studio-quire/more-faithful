package org.studioquire.faithful.drygrass.mixin;

import net.minecraft.client.color.world.GrassColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The map the game looks grass up in: the texture pack's {@code /misc/grasscolor.png}, which choosing another pack rewrites in place. */
@Mixin(GrassColors.class)
public interface GrassColorsAccess {
	@Accessor("colorMap")
	static int[] drygrass$map() {
		throw new AssertionError();
	}
}
