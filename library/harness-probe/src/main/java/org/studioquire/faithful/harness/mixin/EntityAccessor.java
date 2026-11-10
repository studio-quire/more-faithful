package org.studioquire.faithful.harness.mixin;

import java.util.Random;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
	@Accessor("random")
	Random random();

	@Accessor("fallDistance")
	void fallDistance(float distance);
}
