package org.studioquire.faithful.explosivearrows.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.BowItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;

/**
 * A mixin because beta's bow asks the inventory for its own arrow by id, and the core has nothing for what a bow shoots. It takes the first arrows of either
 * kind, in slot order, so the player chooses by where they keep them. It runs on both sides, a client's draw being its guess, so both take from the same stack.
 */
@Mixin(BowItem.class)
abstract class BowItemMixin {
	@WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;remove(I)Z"))
	private boolean explosivearrows$firstArrows(PlayerInventory inventory, int arrowId, Operation<Boolean> remove, @Share("explosive") LocalBooleanRef explosive) {
		if (ExplosiveArrows.takeExplosive(inventory)) {
			explosive.set(true);
			return true;
		}
		return remove.call(inventory, arrowId);
	}

	@WrapOperation(method = "use", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/projectile/ArrowEntity;"))
	private ArrowEntity explosivearrows$shoot(World world, LivingEntity user, Operation<ArrowEntity> make, @Share("explosive") LocalBooleanRef explosive) {
		return explosive.get() ? new ExplosiveArrow(world, user) : make.call(world, user);
	}
}
