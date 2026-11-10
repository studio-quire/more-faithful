package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.InteractionManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** A block break's event, here for this game's own sound and particles. */
@Mixin(InteractionManager.class)
abstract class InteractionManagerMixin {
	@WrapOperation(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;worldEvent(IIIII)V"))
	private void faithful$breakEvent(World world, int event, int x, int y, int z, int data, Operation<Void> send) {
		send.call(world, event, x, y, z, Wire.breakEvent(world.getBlockId(x, y, z), world.getBlockMeta(x, y, z)));
	}
}
