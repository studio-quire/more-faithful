package org.studioquire.faithful.worldrules.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.faithful.worldrules.WorldRules;

/**
 * Bows Wear Out. Beta's most damage is the item's, one for the whole game, and the rule is a world's, so the bow gets later versions' in its field, which
 * only the game's start reads (through {@code Item.isDamageable}: the statistics' broken-bow count, Item Repair's recipes), and {@code getMaxDamage}, which
 * everything after asks (the bar, the wear, the break, a stack's {@code isDamageable}), answers by the world's rule: here, or as the server said. Off,
 * the bow is beta's, a worn one's wear kept unshown, and each world or server a client moves to is read afresh.
 * <p>
 * The wear is beta's own {@code ItemStack.damage}, called as the fishing rod calls it, on both sides: a client's guess wears its bow as the world does, so
 * the server's answer matches and beta doesn't lower and raise the bow in the hand, as it does for a held stack that comes back changed.
 */
@Mixin(BowItem.class)
abstract class BowItemMixin extends Item {
	BowItemMixin(int id) {
		super(id);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void worldrules$mostDamage(int id, CallbackInfo ci) {
		setMaxDamage(WorldRules.BOW_MOST_DAMAGE);
	}

	@Override
	public int getMaxDamage() {
		return WorldRules.bowsWear() ? super.getMaxDamage() : 0;
	}

	/** Beta plays the bow's sound only once an arrow is taken, whichever kind (Explosive Arrows' too), so that's a shot. Off, the damage does nothing. */
	@Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
	private void worldrules$wear(ItemStack stack, World world, PlayerEntity user, CallbackInfoReturnable<ItemStack> ci) {
		stack.damage(1, user);
	}
}
