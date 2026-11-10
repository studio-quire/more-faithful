package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A new world's spawn search runs inside {@code World}'s constructor, before this class's fields exist, and can decorate chunks. A spring's water can then break a plant, and
 * the drop finds the entity map still null: beta's own crash on starting a new server (docs/bugs.md #78). Nothing could be tracking such an entity, as the server adds its
 * tracker's listener after the constructor, so it dies at the world's first tick, unsaved.
 * Pure Faithful's saves mod carries the same wrap; here it's for the library on a server without it. With both, the wraps chain: whichever is outer marks the drop dead and
 * doesn't call through, so the other never sees the null map (libraryHooksTest's {@code beingbuilt} scene runs both ways).
 */
@Mixin(ServerWorld.class)
abstract class ServerWorldMixin {
	@WrapOperation(method = "notifyEntityAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IntHashMap;put(ILjava/lang/Object;)V"))
	private void faithful$whileBeingBuilt(IntHashMap entities, int id, Object entity, Operation<Void> put) {
		if (entities == null) ((Entity) entity).markDead();
		else put.call(entities, id, entity);
	}
}
