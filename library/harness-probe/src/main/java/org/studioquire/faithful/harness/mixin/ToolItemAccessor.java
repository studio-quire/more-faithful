package org.studioquire.faithful.harness.mixin;

import net.minecraft.block.Block;
import net.minecraft.item.ToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ToolItem.class)
public interface ToolItemAccessor {
	@Accessor("effectiveOnBlocks")
	Block[] effective();

	@Accessor("miningSpeed")
	float speed();
}
