package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A path's cells, to see where a way to flee leads before taking it: beta keeps them private and shows only the next. */
@Mixin(Path.class)
public interface PathAccessor {
	@Accessor("nodes")
	PathNode[] faithful$nodes();
}
