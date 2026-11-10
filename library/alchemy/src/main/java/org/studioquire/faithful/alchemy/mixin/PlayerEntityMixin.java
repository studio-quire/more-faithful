package org.studioquire.faithful.alchemy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.alchemy.Ward;

/**
 * The ward. A mixin because armour lives in these two lines of {@code applyDamage} and nowhere else: what protects a hit, and what it wears. The core's damage hook comes before them, with the
 * whole amount, and has no say in either. {@code applyDamage} runs only where the world runs, as {@code damage} stops first on a client.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
	@WrapOperation(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getTotalArmorDurability()I"))
	private int alchemy$blueProtects(PlayerInventory inventory, Operation<Integer> armour) {
		return Ward.protection((PlayerEntity) (Object) this, armour.call(inventory));
	}

	@WrapOperation(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;damageArmor(I)V"))
	private void alchemy$blueWearsFirst(PlayerInventory inventory, int amount, Operation<Void> wear) {
		int rest = Ward.absorb((PlayerEntity) (Object) this, amount);
		if (rest > 0) wear.call(inventory, rest);
	}
}
