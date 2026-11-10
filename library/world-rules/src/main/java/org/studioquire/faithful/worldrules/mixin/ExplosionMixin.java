package org.studioquire.faithful.worldrules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.worldrules.WorldRules;

/**
 * A mixin because the core's damage hook is for living things. The hit still lands with no damage in it, since that's what marks the item's new velocity for
 * sending, so a spared item is thrown, and clients see it fly.
 */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
	@WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
	private boolean worldrules$spareItems(Entity entity, Entity source, int amount, Operation<Boolean> damage) {
		return damage.call(entity, source, entity instanceof ItemEntity && !WorldRules.blastsDestroyItems.get() ? 0 : amount);
	}
}
