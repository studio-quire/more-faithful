package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.api.Tools;

/** Picking something up only joins a stack it could merge with. And whether what's held harvests a block, which is {@code PlayerEntity.canHarvest}'s answer. */
@Mixin(PlayerInventory.class)
abstract class PlayerInventoryMixin {
	@Shadow
	public ItemStack[] main;
	@Shadow
	public int selectedSlot;

	@ModifyReturnValue(method = "isUsingEffectiveTool", at = @At("RETURN"))
	private boolean faithful$tool(boolean usual, Block block) {
		return Tools.harvests(main[selectedSlot], block, usual);
	}

	/** Taking in a stack starts a new one by hand. */
	@ModifyExpressionValue(method = "combineStacks", at = @At(value = "NEW", target = "Lnet/minecraft/item/ItemStack;"))
	private ItemStack faithful$keepData(ItemStack made, ItemStack stack) {
		((org.studioquire.oldfaithful.core.impl.content.StackDataHolder) (Object) made).faithful$setData(org.studioquire.oldfaithful.core.impl.content.StackData.copy(Stacks.peek(stack)));
		return made;
	}

	@ModifyExpressionValue(method = "indexOf(Lnet/minecraft/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isStackable()Z"))
	private boolean faithful$sameData(boolean stackable, ItemStack stack, @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) int slot) {
		return stackable && Stacks.sameData(main[slot], stack);
	}
}
