package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Fire;

/** A refused fire doesn't wear the flint out. Runs on a client on a server too, where the server's word comes after. */
@Mixin(FlintAndSteelItem.class)
abstract class FlintAndSteelItemMixin {
	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$ignite(World world, int x, int y, int z, int block, Operation<Boolean> set, @Local(argsOnly = true) PlayerEntity player, @Share("refused") LocalBooleanRef refused) {
		if (Fire.ignites(world, x, y, z, Fire.Cause.PLAYER, player)) return set.call(world, x, y, z, block);
		refused.set(true);
		return false;
	}

	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/Entity;)V"))
	private void faithful$wear(ItemStack stack, int amount, Entity user, Operation<Void> damage, @Share("refused") LocalBooleanRef refused) {
		if (!refused.get()) damage.call(stack, amount, user);
	}
}
