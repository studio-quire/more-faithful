package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** A falling block's id, for a mod's: the spawn packet's data, unused for falling blocks in vanilla. */
@Mixin(ClientNetworkHandler.class)
abstract class FallingSpawnMixin {
	@ModifyExpressionValue(method = "onEntitySpawn", at = @At(value = "NEW", target = "Lnet/minecraft/entity/FallingBlockEntity;", ordinal = 0))
	private FallingBlockEntity faithful$modBlock(FallingBlockEntity entity, EntitySpawnS2CPacket packet) {
		if (Wire.serverContent && ContentRegistry.modBlock(packet.entityData)) entity.blockId = packet.entityData;
		return entity;
	}
}
