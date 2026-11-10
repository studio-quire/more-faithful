package org.studioquire.oldfaithful.core.mixin.content;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.impl.content.ListenerAccess;

/**
 * Clicks and shift-clicks merge by hand here: id and damage. Where data differs they must see different items, so the second stack's
 * id reads as something else at those comparisons.
 */
@Mixin(ScreenHandler.class)
abstract class ScreenHandlerMixin implements ListenerAccess {
	private static final String ITEM_ID = "Lnet/minecraft/item/ItemStack;itemId:I";

	@Shadow
	protected List listeners;

	@Override
	public List<?> faithful$listeners() {
		return listeners;
	}

	/** Slots of the click: 5 what it returns, then the slot's stack and the cursor's. */
	@ModifyExpressionValue(method = "onSlotClick", at = {@At(value = "FIELD", target = ITEM_ID, ordinal = 1), @At(value = "FIELD", target = ITEM_ID, ordinal = 3)})
	private int faithful$click(int id, @Local(ordinal = 1) ItemStack slot, @Local(ordinal = 2) ItemStack cursor) {
		return Stacks.sameData(slot, cursor) ? id : -id - 1;
	}

	@ModifyExpressionValue(method = "insertItem", at = @At(value = "FIELD", target = ITEM_ID, ordinal = 1))
	private int faithful$shiftClick(int id, ItemStack stack, @Local(ordinal = 1) ItemStack there) {
		return Stacks.sameData(stack, there) ? id : -id - 1;
	}
}
