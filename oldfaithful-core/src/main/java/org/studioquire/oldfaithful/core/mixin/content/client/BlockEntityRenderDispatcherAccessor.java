package org.studioquire.oldfaithful.core.mixin.content.client;

import java.util.Map;

import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockEntityRenderDispatcher.class)
public interface BlockEntityRenderDispatcherAccessor {
	@Accessor("renderers")
	Map faithful$renderers();
}
