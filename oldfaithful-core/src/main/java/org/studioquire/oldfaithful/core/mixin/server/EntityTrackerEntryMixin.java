package org.studioquire.oldfaithful.core.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.entity.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.Attachments;

/** A viewer's been sent the entity: now what's attached to it. {@code getEquipment} is just past the spawn packet. */
@Mixin(EntityTrackerEntry.class)
abstract class EntityTrackerEntryMixin {
	@Shadow
	public Entity currentTrackedEntity;

	@Inject(method = "updateListener", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEquipment()[Lnet/minecraft/item/ItemStack;"))
	private void faithful$attachments(ServerPlayerEntity viewer, CallbackInfo ci) {
		Attachments.sendTracked(currentTrackedEntity, viewer);
	}
}
