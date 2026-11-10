package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Creepers;
import org.studioquire.oldfaithful.core.impl.Flight;

/**
 * Creepers fleeing what frightens them (`Creepers`): beta's {@code tickLiving} is the whole of a monster's mind, with no goals to add one to. A creeper
 * looks round at its start (CreeperEntity doesn't override it); while it's running, beta's asking for a path, to its target or to wander, gets its flight's
 * instead, and it doesn't turn toward its target, which beta does after steering along a path and would undo a path leading away.
 */
@Mixin(MobEntity.class)
abstract class MobEntityMixin {
	@Inject(method = "tickLiving", at = @At("HEAD"))
	private void faithful$lookRound(CallbackInfo ci) {
		Object self = this;
		if (self instanceof CreeperEntity && !((CreeperEntity) self).world.isRemote) Creepers.tick((CreeperEntity) self);
	}

	@WrapOperation(method = "tickLiving", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;findPath(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;F)Lnet/minecraft/entity/ai/pathing/Path;"))
	private Path faithful$toTarget(World world, Entity mob, Entity target, float range, Operation<Path> findPath) {
		Flight flight = Creepers.flight(this);
		return flight == null ? findPath.call(world, mob, target, range) : flight.walking();
	}

	@WrapOperation(method = "pathingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;findPath(Lnet/minecraft/entity/Entity;IIIF)Lnet/minecraft/entity/ai/pathing/Path;"))
	private Path faithful$wander(World world, Entity mob, int x, int y, int z, float range, Operation<Path> findPath) {
		Flight flight = Creepers.flight(this);
		return flight == null ? findPath.call(world, mob, x, y, z, range) : flight.walking();
	}

	@WrapOperation(method = "tickLiving", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;lookAt(Lnet/minecraft/entity/Entity;FF)V"))
	private void faithful$notAtTarget(MobEntity mob, Entity target, float maxYaw, float maxPitch, Operation<Void> lookAt) {
		if (Creepers.flight(mob) == null) lookAt.call(mob, target, maxYaw, maxPitch);
	}
}
