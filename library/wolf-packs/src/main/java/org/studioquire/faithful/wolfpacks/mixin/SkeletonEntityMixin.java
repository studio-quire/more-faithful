package org.studioquire.faithful.wolfpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.wolfpacks.Packs;

/** A mixin because the core has no say in what a monster shoots: a skeleton loosing an arrow at what it's after tells the packs (`Packs.shotAt`). */
@Mixin(SkeletonEntity.class)
abstract class SkeletonEntityMixin {
	@WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean wolfpacks$shoots(World world, Entity arrow, Operation<Boolean> spawn, @Local(argsOnly = true) Entity target) {
		boolean spawned = spawn.call(world, arrow);
		if (spawned && !world.isRemote) Packs.shotAt((SkeletonEntity) (Object) this, target);
		return spawned;
	}
}
