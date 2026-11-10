package org.studioquire.faithful.wolfpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.wolfpacks.Packs;

/**
 * A mixin because the core has no say in what wolves go for. Beta's call to arms has already picked the wolves it sends (its own, free, within 16 blocks,
 * sitting ones only when the player was hurt) and stood them up; this changes only what each is set on. For a creeper, which beta's call drops at its first
 * line, Wolves Fight Creepers makes the same call itself.
 */
@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {
	@Inject(method = "commandWolvesToAttack", at = @At("HEAD"), cancellable = true)
	private void wolfpacks$creepersToo(LivingEntity called, boolean keepSitting, CallbackInfo ci) {
		PlayerEntity owner = (PlayerEntity) (Object) this;
		if (!(called instanceof CreeperEntity) || owner.world.isRemote || !Packs.fightCreepers()) return;
		Packs.callAboutCreeper(owner, (CreeperEntity) called, keepSitting);
		ci.cancel();
	}

	@WrapOperation(method = "commandWolvesToAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setTarget(Lnet/minecraft/entity/Entity;)V"))
	private void wolfpacks$spread(WolfEntity wolf, Entity called, Operation<Void> setTarget) {
		PlayerEntity owner = (PlayerEntity) (Object) this;
		if (owner.world.isRemote || !Packs.on()) {
			setTarget.call(wolf, called);
			return;
		}
		Entity enemy = Packs.call(owner, wolf, called);
		setTarget.call(wolf, enemy);
		Packs.took(wolf, enemy);
	}
}
