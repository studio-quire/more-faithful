package org.studioquire.faithful.worldrules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.worldrules.WorldRules;

/**
 * Pickup Reach. A mixin because the box is made inside {@code tickMovement}, the one place beta picks up items (and
 * arrows lying about): the player's box grown a block each way across and not at all up or down. Later versions' is
 * half a block up and down too, and while riding it's the player's and the vehicle's boxes together, grown a block
 * across only. Where the world runs, as a pickup is.
 */
@Mixin(PlayerEntity.class)
abstract class PickupMixin {
	@WrapOperation(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;"))
	private Box worldrules$pickupReach(Box box, double x, double y, double z, Operation<Box> expand) {
		if (!WorldRules.pickupReach.get()) return expand.call(box, x, y, z);
		Entity vehicle = ((PlayerEntity) (Object) this).vehicle;
		if (vehicle == null || vehicle.dead) return expand.call(box, x, 0.5, z);
		Box v = vehicle.boundingBox;
		Box both = Box.createCached(Math.min(box.minX, v.minX), Math.min(box.minY, v.minY), Math.min(box.minZ, v.minZ),
				Math.max(box.maxX, v.maxX), Math.max(box.maxY, v.maxY), Math.max(box.maxZ, v.maxZ));
		return expand.call(both, x, y, z);
	}
}
