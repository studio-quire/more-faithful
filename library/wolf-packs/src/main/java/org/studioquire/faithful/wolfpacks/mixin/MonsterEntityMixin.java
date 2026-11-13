package org.studioquire.faithful.wolfpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MonsterEntity;
import net.minecraft.entity.passive.WolfEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.wolfpacks.Packs;
import org.studioquire.faithful.wolfpacks.Watch;

/**
 * A mixin because the core has no say in what a monster goes for. Beta's monster turns on whatever hurt it; a creeper bitten by a tamed wolf keeps going for
 * whoever it was after (with Wolves Fight Creepers), so a blast is only ever where its own target is. The bite still pushes it
 * away from the wolf, as any hit does. Not when the wolf is one on watch ({@link Watch}): that creeper is beta's, turns on the
 * wolf and goes off beside it, whoever it was after before, so a watch costs wolves.
 */
@Mixin(MonsterEntity.class)
abstract class MonsterEntityMixin {
	@WrapOperation(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/mob/MonsterEntity;target:Lnet/minecraft/entity/Entity;", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
	private void wolfpacks$notTheWolf(MonsterEntity self, Entity source, Operation<Void> set) {
		if (self instanceof CreeperEntity && source instanceof WolfEntity && ((WolfEntity) source).isTamed() && Packs.fightCreepers() && !Watch.onDuty((WolfEntity) source)) return;
		set.call(self, source);
	}
}
