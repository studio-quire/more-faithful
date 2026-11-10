package org.studioquire.oldfaithful.core.mixin.content;

import java.util.Map;

import net.minecraft.entity.EntityRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRegistry.class)
public interface EntityRegistryInvoker {
	@Invoker("register")
	static void faithful$register(Class type, String id, int rawId) {
		throw new AssertionError();
	}

	@Accessor("idToClass")
	static Map faithful$idToClass() {
		throw new AssertionError();
	}
}
