package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code World.getWorldStorage} is the server's alone. */
@Mixin(World.class)
public interface WorldAccessor {
	@Accessor("storage")
	WorldStorage faithful$storage();
}
