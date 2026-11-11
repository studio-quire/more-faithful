package org.studioquire.faithful.featherlure.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Beta's bobber's own state: ticks left in which a bite can be reeled in, and whether it's lying on the ground. The setters are for the mod's test. */
@Mixin(FishingBobberEntity.class)
public interface FishingBobberAccess {
	@Accessor("hookCountdown")
	int featherlure$hookCountdown();

	@Accessor("hookCountdown")
	void featherlure$setHookCountdown(int ticks);

	@Accessor("inGround")
	void featherlure$setInGround(boolean inGround);
}
