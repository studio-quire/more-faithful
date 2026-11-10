package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Tools;

/** Vanilla compares held items with {@code Item.SHEARS.id}, so a mod's shears would do nothing: anything that is shears reads as itself here. */
final class ShearsMixins {
	private ShearsMixins() {
	}

	@Mixin(LeavesBlock.class)
	abstract static class Leaves {
		@ModifyExpressionValue(method = "afterBreak", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ShearsItem;id:I"))
		private int faithful$shears(int shears, @Local(argsOnly = true) PlayerEntity player) {
			return Tools.shearsId(player.getHand(), shears);
		}
	}

	/** Vanilla's shears wear on its leaves and cobwebs alone. */
	@Mixin(ShearsItem.class)
	abstract static class Wear {
		@ModifyExpressionValue(method = "postMine", at = @At(value = "FIELD", target = "Lnet/minecraft/block/LeavesBlock;id:I"))
		private int faithful$wears(int leaves, @Local(argsOnly = true, ordinal = 0) int blockId) {
			return Tools.shearsWearOn(blockId) ? blockId : leaves;
		}
	}

	@Mixin(SheepEntity.class)
	abstract static class Sheep {
		@ModifyExpressionValue(method = "interact", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ShearsItem;id:I"))
		private int faithful$shears(int shears, @Local(argsOnly = true) PlayerEntity player) {
			return Tools.shearsId(player.inventory.getSelectedItem(), shears);
		}
	}
}
