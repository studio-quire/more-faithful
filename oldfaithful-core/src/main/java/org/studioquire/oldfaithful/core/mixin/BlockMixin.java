package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Drops;

/**
 * A block's drops: the five-argument {@code dropStacks} is every cause but explosions, and calls the overridable six-argument one.
 * While that runs, {@code dropStack}'s stacks are gathered; one that comes outside it (shears on leaves) is a drop of its own.
 */
@Mixin(Block.class)
abstract class BlockMixin {
	@WrapOperation(method = "dropStacks(Lnet/minecraft/world/World;IIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;dropStacks(Lnet/minecraft/world/World;IIIIF)V"))
	private void faithful$gather(Block block, World world, int x, int y, int z, int meta, float luck, Operation<Void> drop) {
		if (!Drops.hasBlockListeners() || world.isRemote) {
			drop.call(block, world, x, y, z, meta, luck);
			return;
		}
		Drops.gatherBlock(world, x, y, z, block, meta, s -> ((BlockInvoker) block).faithful$dropStack(world, x, y, z, s), () -> drop.call(block, world, x, y, z, meta, luck));
	}

	@Inject(method = "dropStack", at = @At("HEAD"), cancellable = true)
	private void faithful$take(World world, int x, int y, int z, ItemStack stack, CallbackInfo ci) {
		Block block = (Block) (Object) this;
		if (!world.isRemote && Drops.takeBlockStack(world, x, y, z, block, stack, s -> ((BlockInvoker) block).faithful$dropStack(world, x, y, z, s))) ci.cancel();
	}
}
