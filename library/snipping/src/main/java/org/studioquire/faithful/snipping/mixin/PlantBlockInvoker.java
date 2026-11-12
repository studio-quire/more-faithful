package org.studioquire.faithful.snipping.mixin;

import net.minecraft.block.PlantBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlantBlock.class)
public interface PlantBlockInvoker {
	@Invoker("breakIfCannotGrow")
	void snipping$breakIfCannotGrow(World world, int x, int y, int z);
}
