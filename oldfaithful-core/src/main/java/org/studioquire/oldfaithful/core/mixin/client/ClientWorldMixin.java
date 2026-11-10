package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.DayClock;

/** Between the server's corrections a client runs its own sun: at the rule's pace, if it knows the rule. */
@Mixin(ClientWorld.class)
abstract class ClientWorldMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ClientWorld;setTime(J)V"))
	private void faithful$dayLength(ClientWorld world, long next, Operation<Void> set) {
		set.call(world, DayClock.clientTicked(world.getProperties(), next));
	}
}
