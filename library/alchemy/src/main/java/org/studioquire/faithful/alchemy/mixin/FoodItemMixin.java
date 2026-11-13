package org.studioquire.faithful.alchemy.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.faithful.alchemy.Ward;

/**
 * The golden apple's ward, a Strong Ward Potion's, beside its full heal. A mixin because eating is {@code FoodItem.use} and nothing hears of it. Eaten as beta eats it, whatever the bar and the
 * blue (it tops the blue up to the toughest as a potion does), so nothing about when it goes changes; its heal stays beta's, or Hearty Food's, which heals anything of a whole bar at once.
 */
@Mixin(FoodItem.class)
abstract class FoodItemMixin {
	@Inject(method = "use", at = @At("HEAD"))
	private void alchemy$goldenAppleWards(ItemStack stack, World world, PlayerEntity user, CallbackInfoReturnable<ItemStack> info) {
		if (!world.isRemote && (Object) this == Item.GOLDEN_APPLE && user.health > 0) Ward.put(user, Ward.toughest());
	}
}
