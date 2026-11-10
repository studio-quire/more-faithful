package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.entity.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.impl.content.EntityContent;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/**
 * Anything but vanilla's classes throws here, on the server's main thread. A registered class goes by name, ahead of the {@code instanceof} chain,
 * which would send a mod's boat subclass as a boat; a mod's falling block goes as sand with its id in the data.
 */
@Mixin(EntityTrackerEntry.class)
abstract class EntityTrackerEntryMixin {
	@Shadow
	public Entity currentTrackedEntity;

	@Inject(method = "createAddEntityPacket", at = @At("HEAD"), cancellable = true)
	private void faithful$falling(CallbackInfoReturnable<Packet> cir) {
		EntityContent.Entry entry = EntityContent.of(currentTrackedEntity);
		if (entry != null) {
			cir.setReturnValue(EntityContent.spawnPacket(currentTrackedEntity, entry));
		} else if (currentTrackedEntity instanceof FallingBlockEntity) {
			int id = ((FallingBlockEntity) currentTrackedEntity).blockId;
			if (Overlay.modded(id)) cir.setReturnValue(new EntitySpawnS2CPacket(currentTrackedEntity, 70, id));
		}
	}
}
