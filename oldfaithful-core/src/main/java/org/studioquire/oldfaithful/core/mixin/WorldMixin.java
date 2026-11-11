package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Entities;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Lightning;
import org.studioquire.oldfaithful.core.api.Ticks;

/** A client on a server has a {@code ClientWorld}, whose {@code tick} doesn't call this one. */
@Mixin(World.class)
abstract class WorldMixin {
	@Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
	private void faithful$spawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!Entities.spawning((World) (Object) this, entity)) cir.setReturnValue(false);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void faithful$tick(CallbackInfo ci) {
		Ticks.world((World) (Object) this);
	}

	/** Both readings of the flag: natural spawning, and the monster that wakes a sleeper. */
	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;allowMonsterSpawning:Z"))
	private boolean faithful$monsters(boolean allowed) {
		return allowed && Entities.monsterSpawning((World) (Object) this);
	}

	@Inject(method = "saveWithLoadingDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;save()V"))
	private void faithful$save(CallbackInfo ci) {
		Lifecycle.worldSaving((World) (Object) this);
	}

	/** The weather's strike, before the entity's constructor lights its fires. Null is no strike. */
	@WrapOperation(method = "manageChunkUpdatesAndEvents", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDD)Lnet/minecraft/entity/LightningEntity;"))
	private LightningEntity faithful$strike(World world, double x, double y, double z, Operation<LightningEntity> create) {
		int[] at = Lightning.strike(world, MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
		return at == null ? null : create.call(world, (double) at[0], (double) at[1], (double) at[2]);
	}

	@WrapOperation(method = "manageChunkUpdatesAndEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnGlobalEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean faithful$strikes(World world, Entity entity, Operation<Boolean> spawn) {
		if (entity == null || !spawn.call(world, entity)) return false;
		Lightning.struck(world, MathHelper.floor(entity.x), MathHelper.floor(entity.y), MathHelper.floor(entity.z));
		return true;
	}
}
