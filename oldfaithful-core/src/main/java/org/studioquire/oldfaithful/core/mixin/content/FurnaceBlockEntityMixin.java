package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmeltingRecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.studioquire.oldfaithful.core.impl.RecipeTable;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.VanillaStorage;

/** {@code id < 256} means "is a block" here; a mod's block item is one too. Recipes go by id alone in vanilla's table, and fuels by a chain of tests, so mods' answers go in here. */
@Mixin(FurnaceBlockEntity.class)
abstract class FurnaceBlockEntityMixin implements VanillaStorage.FuelTable {
	@Shadow
	private ItemStack[] inventory;

	@Shadow
	private int getFuelTime(ItemStack stack) {
		throw new AssertionError();
	}

	@Override
	public int faithful$fuelTime(ItemStack stack) {
		return getFuelTime(stack);
	}

	@ModifyConstant(method = "getFuelTime", constant = @Constant(intValue = 256))
	private int faithful$blockLimit(int limit, @Local(ordinal = 0) int id) {
		return ContentRegistry.isBlockItem(id) ? Integer.MAX_VALUE : limit;
	}

	@ModifyReturnValue(method = "getFuelTime", at = @At("RETURN"))
	private int faithful$fuel(int ticks, ItemStack stack) {
		return stack == null ? ticks : RecipeTable.fuelTime(stack, ticks);
	}

	@WrapOperation(method = {"canAcceptRecipeOutput", "craftRecipe"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/recipe/SmeltingRecipeManager;craft(I)Lnet/minecraft/item/ItemStack;"))
	private ItemStack faithful$smelt(SmeltingRecipeManager recipes, int id, Operation<ItemStack> craft) {
		return RecipeTable.smelt(inventory[0], craft.call(recipes, id));
	}
}
