package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Block.class)
public interface BlockInvoker {
	@Invoker("dropStack")
	void faithful$dropStack(World world, int x, int y, int z, ItemStack stack);
}
