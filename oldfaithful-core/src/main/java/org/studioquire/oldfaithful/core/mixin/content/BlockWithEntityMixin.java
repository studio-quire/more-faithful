package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

@Mixin(BlockWithEntity.class)
abstract class BlockWithEntityMixin implements Overlay.BlockEntityMaker {
	@Shadow
	protected abstract BlockEntity createBlockEntity();

	@Override
	public BlockEntity faithful$makeBlockEntity() {
		return createBlockEntity();
	}
}
