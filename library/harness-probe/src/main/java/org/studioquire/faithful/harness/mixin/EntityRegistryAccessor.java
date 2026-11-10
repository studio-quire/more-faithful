package org.studioquire.faithful.harness.mixin;

import java.util.Map;

import net.minecraft.entity.EntityRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRegistry.class)
public interface EntityRegistryAccessor {
	@Accessor("idToClass")
	static Map<String, Class<?>> ids() {
		throw new AssertionError();
	}
}
