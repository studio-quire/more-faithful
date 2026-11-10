package org.studioquire.faithful.explosivearrows.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;

/**
 * A mixin because the core's damage hook is for living things and can't say who did it. An explosive arrow's blast hurts each thing as from whoever
 * {@link ExplosiveArrow#hurting} says, and at half what beta's rule gives a blast its size: (amount + 1) / 2 is exactly that rule with its 8 made 4.
 * Everything else about the hit, the throw and the reach is beta's.
 *
 * And its puffs: beta's {@code WorldRenderer.addParticle} leaves out any particle more than 16 blocks from the camera, so a burst at bow range showed none.
 * A puff of this blast past 16 and within {@link ExplosiveArrows#SEEN} is drawn by the client's {@link ExplosiveArrows.Puffs} instead of being handed to
 * beta's; the rest go to beta's as ever, so nothing is drawn twice, whatever another mod lets beta's draw, and other blasts are untouched.
 */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
	@Shadow
	public Entity source;

	@WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
	private boolean explosivearrows$asTheShooter(Entity hit, Entity source, int amount, Operation<Boolean> damage) {
		if (!(source instanceof ExplosiveArrow)) return damage.call(hit, source, amount);
		return damage.call(hit, ((ExplosiveArrow) source).hurting(hit), (amount + 1) / 2);
	}

	@WrapOperation(method = "playExplosionSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Ljava/lang/String;DDDDDD)V"))
	private void explosivearrows$seenFar(World world, String name, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Operation<Void> add) {
		ExplosiveArrows.Puffs puffs = ExplosiveArrows.puffs;
		if (puffs != null && (source instanceof ExplosiveArrow || ExplosiveArrows.drawing) && puffs.far(world, name, x, y, z, velocityX, velocityY, velocityZ)) return;
		add.call(world, name, x, y, z, velocityX, velocityY, velocityZ);
	}
}
