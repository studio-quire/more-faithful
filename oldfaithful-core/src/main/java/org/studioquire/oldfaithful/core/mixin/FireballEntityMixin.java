package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Explosions;
import org.studioquire.oldfaithful.core.impl.ExplosionCause;

/** A ghast fireball passes no source; its explosion is named here. */
@Mixin(FireballEntity.class)
abstract class FireballEntityMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZ)Lnet/minecraft/world/explosion/Explosion;"))
	private Explosion faithful$cause(World world, Entity source, double x, double y, double z, float power, boolean fire, Operation<Explosion> create) {
		ExplosionCause.set(Explosions.Cause.FIREBALL);
		try {
			return create.call(world, source, x, y, z, power, fire);
		} finally {
			ExplosionCause.set(Explosions.Cause.OTHER);
		}
	}
}
