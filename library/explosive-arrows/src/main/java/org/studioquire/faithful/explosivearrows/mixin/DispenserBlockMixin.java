package org.studioquire.faithful.explosivearrows.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;

/**
 * A mixin because beta's dispenser shoots only its own arrow, egg and snowball, by id, and throws anything else out as an item. An explosive arrow takes the
 * arrow's way: the same aim, spread and sound.
 */
@Mixin(DispenserBlock.class)
abstract class DispenserBlockMixin {
	// The first item id it reads is the one it holds against the arrow's.
	@ModifyExpressionValue(method = "dispense", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack;itemId:I", ordinal = 0))
	private int explosivearrows$asArrow(int id, @Share("explosive") LocalBooleanRef explosive) {
		if (id != ExplosiveArrows.explosiveArrow.get().id) return id;
		explosive.set(true);
		return Item.ARROW.id;
	}

	@WrapOperation(method = "dispense", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDD)Lnet/minecraft/entity/projectile/ArrowEntity;"))
	private ArrowEntity explosivearrows$shoot(World world, double x, double y, double z, Operation<ArrowEntity> make, @Share("explosive") LocalBooleanRef explosive) {
		return explosive.get() ? new ExplosiveArrow(world, x, y, z) : make.call(world, x, y, z);
	}
}
