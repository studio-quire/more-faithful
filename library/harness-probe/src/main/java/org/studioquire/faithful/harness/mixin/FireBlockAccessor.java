package org.studioquire.faithful.harness.mixin;

import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireBlock.class)
public interface FireBlockAccessor {
	@Accessor("burnChances")
	int[] burn();

	@Accessor("spreadChances")
	int[] spread();
}
