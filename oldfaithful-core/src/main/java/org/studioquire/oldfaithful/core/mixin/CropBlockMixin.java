package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Drops;

/** Wheat spawns its seeds by hand. They join the rest of its drop. */
@Mixin(CropBlock.class)
abstract class CropBlockMixin {
	@WrapOperation(method = "dropStacks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean faithful$seeds(World world, Entity entity, Operation<Boolean> spawn) {
		if (!Drops.hasBlockListeners() || !Drops.gathering()) return spawn.call(world, entity);
		Drops.addBlockStack(((ItemEntity) entity).stack);
		return true;
	}
}
