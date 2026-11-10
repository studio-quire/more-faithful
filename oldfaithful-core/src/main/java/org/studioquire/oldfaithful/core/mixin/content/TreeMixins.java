package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LogBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Trees;

/** Leaf decay goes by {@code Block.LEAVES.id} and {@code Block.LOG.id}: any mod's leaves and logs read as those here, so vanilla's rules are theirs. */
final class TreeMixins {
	private static final String GET_BLOCK_ID = "Lnet/minecraft/world/World;getBlockId(III)I";

	private TreeMixins() {
	}

	@Mixin(LeavesBlock.class)
	abstract static class Leaves {
		@ModifyExpressionValue(method = {"onBreak", "onTick"}, at = @At(value = "INVOKE", target = GET_BLOCK_ID))
		private int faithful$kind(int id) {
			return Trees.kind(id);
		}
	}

	@Mixin(LogBlock.class)
	abstract static class Log {
		@ModifyExpressionValue(method = "onBreak", at = @At(value = "INVOKE", target = GET_BLOCK_ID))
		private int faithful$kind(int id) {
			return Trees.kind(id);
		}
	}
}
