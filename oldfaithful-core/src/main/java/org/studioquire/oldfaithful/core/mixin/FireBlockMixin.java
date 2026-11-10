package org.studioquire.oldfaithful.core.mixin;

import java.util.Random;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Fire;

/**
 * {@code onTick}'s own set is the ignition loop, into air beside something flammable. {@code trySpreadingFire} is a block being
 * eaten: it becomes fire, or air, and TNT goes off whichever. A veto of that leaves the block, and the TNT, alone.
 */
@Mixin(FireBlock.class)
abstract class FireBlockMixin {
	@Inject(method = "onTick", at = @At("HEAD"))
	private void faithful$ticking(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		Fire.burningAt(x, y, z);
	}

	@WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIIII)Z"))
	private boolean faithful$ignite(World world, int x, int y, int z, int block, int meta, Operation<Boolean> set) {
		return Fire.ignites(world, x, y, z, Fire.Cause.SPREAD, null) && set.call(world, x, y, z, block, meta);
	}

	@WrapOperation(method = "trySpreadingFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIIII)Z"))
	private boolean faithful$burnToFire(World world, int x, int y, int z, int block, int meta, Operation<Boolean> set, @Share("spared") LocalBooleanRef spared) {
		if (Fire.burns(world, x, y, z) && Fire.ignites(world, x, y, z, Fire.Cause.SPREAD, null)) return set.call(world, x, y, z, block, meta);
		spared.set(true);
		return false;
	}

	@WrapOperation(method = "trySpreadingFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$burnToAir(World world, int x, int y, int z, int block, Operation<Boolean> set, @Share("spared") LocalBooleanRef spared) {
		if (Fire.burns(world, x, y, z)) return set.call(world, x, y, z, block);
		spared.set(true);
		return false;
	}

	@WrapOperation(method = "trySpreadingFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onMetadataChange(Lnet/minecraft/world/World;IIII)V"))
	private void faithful$tnt(Block tnt, World world, int x, int y, int z, int meta, Operation<Void> primed, @Share("spared") LocalBooleanRef spared) {
		if (!spared.get()) primed.call(tnt, world, x, y, z, meta);
	}
}
