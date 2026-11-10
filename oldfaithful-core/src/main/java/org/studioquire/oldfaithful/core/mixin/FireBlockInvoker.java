package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface FireBlockInvoker {
	@Invoker("registerFlammableBlock")
	void faithful$register(int block, int burnChance, int spreadChance);
}
