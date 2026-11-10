package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Whether its fuse is lit (1) or going out (-1): beta keeps it private. */
@Mixin(CreeperEntity.class)
public interface CreeperEntityAccessor {
	@Invoker("getFuseSpeed")
	int faithful$fuseSpeed();
}
