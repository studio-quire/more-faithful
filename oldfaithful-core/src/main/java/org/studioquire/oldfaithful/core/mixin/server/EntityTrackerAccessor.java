package org.studioquire.oldfaithful.core.mixin.server;

import net.minecraft.server.entity.EntityTracker;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTracker.class)
public interface EntityTrackerAccessor {
	@Accessor("entriesById")
	IntHashMap faithful$entriesById();
}
