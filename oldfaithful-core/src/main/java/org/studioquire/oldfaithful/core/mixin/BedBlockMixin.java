package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SleepAttemptResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Beds;
import org.studioquire.oldfaithful.core.api.Explosions;
import org.studioquire.oldfaithful.core.impl.ExplosionCause;

/** {@code onUse} only goes past its first line where the world runs, so this holds for a vanilla client on a server. */
@Mixin(BedBlock.class)
abstract class BedBlockMixin {
	@WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;trySleep(III)Lnet/minecraft/entity/player/SleepAttemptResult;"))
	private SleepAttemptResult faithful$sleep(PlayerEntity player, int x, int y, int z, Operation<SleepAttemptResult> trySleep) {
		if (!Beds.fire(player, x, y, z).spawnOnly()) return trySleep.call(player, x, y, z);
		player.setSpawnPos(new Vec3i(x, y, z));
		player.sendMessage("Respawn point set");
		return SleepAttemptResult.OTHER_PROBLEM; // beta's onUse says nothing for this one
	}

	@WrapOperation(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZ)Lnet/minecraft/world/explosion/Explosion;"))
	private Explosion faithful$cause(World world, Entity source, double x, double y, double z, float power, boolean fire, Operation<Explosion> create) {
		ExplosionCause.set(Explosions.Cause.BED);
		try {
			return create.call(world, source, x, y, z, power, fire);
		} finally {
			ExplosionCause.set(Explosions.Cause.OTHER);
		}
	}
}
