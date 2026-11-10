package org.studioquire.faithful.wolfpacks.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How far its fuse has burned, in ticks: at 30 it goes off, and is marked dead just after its blast has hurt everyone. */
@Mixin(CreeperEntity.class)
public interface CreeperEntityAccessor {
	@Accessor("fuseTime")
	int wolfpacks$fuseTime();
}
