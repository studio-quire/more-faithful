package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Blocks;
import org.studioquire.oldfaithful.core.api.Drops;

/** A player's breaking and using on a server. A client has already guessed at a break, so a refused one is sent back. */
@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerMixin {
	@Shadow
	private ServerWorld world;
	@Shadow
	public PlayerEntity player;

	@Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
	private void faithful$break(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (Blocks.breaking(world, player, x, y, z)) return;
		ServerPlayerEntity to = (ServerPlayerEntity) player;
		to.networkHandler.sendPacket(new BlockUpdateS2CPacket(x, y, z, world));
		BlockEntity entity = world.getBlockEntity(x, y, z);
		Packet update = entity == null ? null : entity.createUpdatePacket();
		if (update != null) to.networkHandler.sendPacket(update);
		cir.setReturnValue(false);
	}

	@WrapOperation(method = "tryBreakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"))
	private void faithful$breaker(Block block, World world, PlayerEntity player, int x, int y, int z, int meta, Operation<Void> after) {
		Drops.brokenBy(player, () -> after.call(block, world, player, x, y, z, meta));
	}

	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void faithful$use(PlayerEntity player, World world, ItemStack stack, int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (!Blocks.using(world, player, stack, x, y, z, side)) cir.setReturnValue(false);
	}
}
