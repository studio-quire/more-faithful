package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Blocks;
import org.studioquire.oldfaithful.core.api.Drops;

/** Breaking in single player: its own code, not the server's. */
@Mixin(SingleplayerInteractionManager.class)
abstract class SingleplayerInteractionManagerMixin extends InteractionManager {
	SingleplayerInteractionManagerMixin(Minecraft minecraft) {
		super(minecraft);
	}

	@Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
	private void faithful$break(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
		if (!Blocks.breaking(minecraft.world, minecraft.player, x, y, z)) cir.setReturnValue(false);
	}

	@WrapOperation(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"))
	private void faithful$breaker(Block block, World world, PlayerEntity player, int x, int y, int z, int meta, Operation<Void> after) {
		Drops.brokenBy(player, () -> after.call(block, world, player, x, y, z, meta));
	}
}
