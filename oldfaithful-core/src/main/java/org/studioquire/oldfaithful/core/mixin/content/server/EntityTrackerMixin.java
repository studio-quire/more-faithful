package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.entity.Entity;
import net.minecraft.server.entity.EntityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.EntityContent;

/** Vanilla tracks only the classes it lists, so a mod's would never reach a client; and a subclass of one of them would get its numbers. */
@Mixin(EntityTracker.class)
abstract class EntityTrackerMixin {
	@Shadow
	public abstract void startTracking(Entity entity, int trackedDistance, int tracingFrequency, boolean alwaysUpdateVelocity);

	@Inject(method = "onEntityAdded", at = @At("HEAD"), cancellable = true)
	private void faithful$track(Entity entity, CallbackInfo ci) {
		EntityContent.Entry entry = EntityContent.of(entity);
		if (entry == null) return;
		startTracking(entity, entry.range, entry.rate, entry.velocity);
		ci.cancel();
	}
}
