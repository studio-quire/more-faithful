package org.studioquire.faithful.harness.mixin;

import java.util.Map;

import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {
	@Accessor("idToClass")
	static Map<String, Class<?>> ids() {
		throw new AssertionError();
	}
}
