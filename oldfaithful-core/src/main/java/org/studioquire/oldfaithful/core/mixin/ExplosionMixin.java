package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.Explosions;
import org.studioquire.oldfaithful.core.api.Fire;
import org.studioquire.oldfaithful.core.impl.ExplosionCause;

/** After the rays are cast, before entities are hurt and fires set: {@code power} doubling is the first thing after them. */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
	@Shadow
	private World world;
	@Shadow
	public Entity source;

	@Inject(method = "explode", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/explosion/Explosion;power:F", ordinal = 0))
	private void faithful$blocks(CallbackInfo ci) {
		if (!Explosions.hasListeners() || world.isRemote) return;
		Explosions.Cause cause = source instanceof CreeperEntity ? Explosions.Cause.CREEPER : ExplosionCause.current();
		Explosions.fire(new Explosions.BlockEvent((Explosion) (Object) this, world, cause));
	}

	@WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$ignite(World world, int x, int y, int z, int block, Operation<Boolean> set) {
		return Fire.ignites(world, x, y, z, Fire.Cause.EXPLOSION, null) && set.call(world, x, y, z, block);
	}

	@WrapOperation(method = "playExplosionSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;dropStacks(Lnet/minecraft/world/World;IIIIF)V"))
	private void faithful$drops(Block block, World world, int x, int y, int z, int meta, float luck, Operation<Void> drop) {
		if (!Drops.hasBlockListeners() || world.isRemote) {
			drop.call(block, world, x, y, z, meta, luck);
			return;
		}
		Drops.gatherBlock(world, x, y, z, block, meta, s -> ((BlockInvoker) block).faithful$dropStack(world, x, y, z, s), () -> drop.call(block, world, x, y, z, meta, luck));
	}
}
