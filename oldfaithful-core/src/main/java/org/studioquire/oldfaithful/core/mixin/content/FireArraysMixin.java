package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** Fire's burn and spread tables, by block id: a mod block beside a fire would run off vanilla's. */
@Mixin(FireBlock.class)
abstract class FireArraysMixin {
	@ModifyConstant(method = "<init>", constant = @Constant(intValue = 256))
	private int faithful$size(int size) {
		return ContentRegistry.BLOCK_LIMIT;
	}
}
