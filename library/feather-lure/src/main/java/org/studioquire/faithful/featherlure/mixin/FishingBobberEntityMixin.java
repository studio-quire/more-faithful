package org.studioquire.faithful.featherlure.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.studioquire.faithful.featherlure.FeatherLure;

/** Beta's bobber on a lured line. The core has no hook for fishing. */
@Mixin(FishingBobberEntity.class)
abstract class FishingBobberEntityMixin {
	@Shadow
	public PlayerEntity owner;

	/**
	 * Where the world runs, beta's bobber goes as soon as its owner's hand holds anything but a fishing rod: a lured one is a fishing rod too. It's the hand this tick,
	 * which the bite's roll goes by, so the bobber's look follows it here.
	 */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
	private Item featherlure$aRodToo(Item held) {
		boolean lured = held == FeatherLure.luredRod.get();
		FeatherLure.markLine((Entity) (Object) this, lured);
		return lured ? Item.FISHING_ROD : held;
	}

	/**
	 * The roll for a bite, 1 in 500 or in rain 1 in 300, against half of either on a lured line. A client on a server rolls its own copy of the bobber for splashes of
	 * its own, which beta never ties to the server's bites, and knows no owner there: those stay beta's.
	 */
	@ModifyArg(method = "tick", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isRaining(III)Z")),
			at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 0))
	private int featherlure$biteOdds(int odds) {
		return FeatherLure.lured(owner) ? Math.max(1, odds / FeatherLure.BITE_DIVISOR) : odds;
	}
}
