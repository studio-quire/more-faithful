package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.block.Block;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** Room for 4096 ids in the eight tables indexed by block id. */
@Mixin(Block.class)
abstract class BlockArraysMixin implements ContentRegistry.BlockInit {
	@ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 256),
			slice = @Slice(to = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/block/Block;BLOCKS_IGNORE_META_UPDATE:[Z")))
	private static int faithful$size(int size) {
		return ContentRegistry.BLOCK_LIMIT;
	}

	@Shadow
	protected abstract void init();

	@Override
	public void faithful$init() {
		init();
	}
}
