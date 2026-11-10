package org.studioquire.oldfaithful.core.mixin.content;

import java.util.Map;

import net.minecraft.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockEntity.class)
public interface BlockEntityInvoker {
	@Invoker("create")
	static void faithful$create(Class type, String id) {
		throw new AssertionError();
	}

	@Accessor("idToClass")
	static Map faithful$idToClass() {
		throw new AssertionError();
	}
}
