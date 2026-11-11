package org.studioquire.faithful.wolfpacks.mixin;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.pathing.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The path a mob is on. Beta swaps it for a stroll now and then, so a wolf walking back to its spot has to see whether it's still on its own (`Watch`). */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {
	@Accessor("path")
	Path wolfpacks$path();
}
