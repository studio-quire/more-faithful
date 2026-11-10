package org.studioquire.oldfaithful.core.mixin.content.client;

import net.minecraft.client.render.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Tessellator.class)
public interface TessellatorAccessor {
	@Accessor("drawing")
	void faithful$setDrawing(boolean drawing);
}
