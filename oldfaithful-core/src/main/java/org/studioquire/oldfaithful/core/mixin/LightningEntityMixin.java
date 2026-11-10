package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LightningEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Fire;

/** The fires a strike lights, at once and on each flash after. A client's copy of a strike makes the same calls and asks nobody. */
@Mixin(LightningEntity.class)
abstract class LightningEntityMixin {
	@WrapOperation(method = {"<init>", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$ignite(World world, int x, int y, int z, int block, Operation<Boolean> set) {
		return Fire.ignites(world, x, y, z, Fire.Cause.LIGHTNING, null) && set.call(world, x, y, z, block);
	}
}
