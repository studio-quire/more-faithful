package org.studioquire.faithful.explosivearrows.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;

/**
 * A mixin because the core has no hook on a projectile striking something, and beta's arrow keeps what it struck to one call inside its tick. An explosive
 * arrow's own hit comes first, an arrow's as ever; then it bursts on what it struck, landed or not, so it never bounces off something that can't be hurt
 * just then. Where the world runs only: a client on a server flies its copy as beta's.
 */
@Mixin(ArrowEntity.class)
abstract class ArrowEntityMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/Entity;I)Z"))
	private boolean explosivearrows$burst(Entity hit, Entity shooter, int amount, Operation<Boolean> damage) {
		boolean landed = damage.call(hit, shooter, amount);
		Object self = this;
		return self instanceof ExplosiveArrow && ((ExplosiveArrow) self).burstOn(hit) || landed;
	}
}
