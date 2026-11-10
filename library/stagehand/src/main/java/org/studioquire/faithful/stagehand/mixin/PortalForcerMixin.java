package org.studioquire.faithful.stagehand.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.stagehand.Move;

/**
 * Moving a player between dimensions goes through the game's own way, so everything hooked on it hears, and that way ends by finding or building a portal where they arrive. The core has no way
 * between dimensions without one, so a mixin it is. Nothing else about a portal changes: this is off but for the moment Stagehand is moving someone.
 */
@Mixin(PortalForcer.class)
abstract class PortalForcerMixin {
	@Inject(method = "moveToPortal", at = @At("HEAD"), cancellable = true)
	private void stagehand$noPortal(World world, Entity entity, CallbackInfo ci) {
		if (Move.crossing) ci.cancel();
	}
}
