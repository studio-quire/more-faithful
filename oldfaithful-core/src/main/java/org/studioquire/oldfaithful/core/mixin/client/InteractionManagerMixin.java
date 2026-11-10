package org.studioquire.oldfaithful.core.mixin.client;

import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Blocks;

/** Using a block in single player. On a server this is the client's guess, and {@code Blocks} leaves it alone. */
@Mixin(InteractionManager.class)
abstract class InteractionManagerMixin {
	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void faithful$use(PlayerEntity player, World world, ItemStack item, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (!Blocks.using(world, player, item, x, y, z, side)) cir.setReturnValue(false);
	}
}
