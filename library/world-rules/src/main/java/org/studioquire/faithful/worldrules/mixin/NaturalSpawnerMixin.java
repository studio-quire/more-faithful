package org.studioquire.faithful.worldrules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.worldrules.WorldRules;

/**
 * A mixin because {@code canSpawn} is shared with spawners, which keep beta's rule, so it's natural spawning's own two calls that are wrapped: the world's, and the monster that wakes a sleeper.
 * Beta's check still runs first, so it makes the same random draws.
 */
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
	@WrapOperation(method = {"tick", "spawnMonstersAndWakePlayers"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;canSpawn()Z"))
	private static boolean worldrules$monsterLight(LivingEntity entity, Operation<Boolean> canSpawn) {
		return canSpawn.call(entity) && !WorldRules.lightStops(entity);
	}
}
