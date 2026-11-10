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

/**
 * A mixin because the core has no say in what a monster goes for. Beta's monster turns on whatever hurt it; a creeper bitten by a tamed wolf keeps going for
 * whoever it was after (the user's call, 2026-11-10, with Wolves Fight Creepers), so a blast is only ever where its own target is. The bite still pushes it
 * away from the wolf, as any hit does.
 */
@Mixin(MonsterEntity.class)
abstract class MonsterEntityMixin {
	@WrapOperation(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/mob/MonsterEntity;target:Lnet/minecraft/entity/Entity;", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
	private void wolfpacks$notTheWolf(MonsterEntity self, Entity source, Operation<Void> set) {
		if (self instanceof CreeperEntity && source instanceof WolfEntity && ((WolfEntity) source).isTamed() && Packs.fightCreepers()) return;
		set.call(self, source);
	}
}
