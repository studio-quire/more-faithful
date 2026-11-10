package org.studioquire.faithful.explosivearrows.mixin;

import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArrowEntity.class)
public interface ArrowEntityAccessor {
	@Accessor("inGround")
	boolean explosivearrows$inGround();
}
