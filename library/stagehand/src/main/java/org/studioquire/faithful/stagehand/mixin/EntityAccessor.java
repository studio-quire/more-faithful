package org.studioquire.faithful.stagehand.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A fall begun before a teleport would land with the teleport: the game only ever clears this by landing. */
@Mixin(Entity.class)
public interface EntityAccessor {
	@Accessor("fallDistance")
	void stagehand$fallDistance(float distance);
}
