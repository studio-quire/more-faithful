package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Armor;

/** Vanilla's five armor names, in a table with room for mods' numbers, whose real path goes in at the bind. */
@Mixin(PlayerEntityRenderer.class)
abstract class PlayerArmorMixin {
	@Shadow
	@Final
	@Mutable
	private static String[] armorTextureNames;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void faithful$room(CallbackInfo ci) {
		armorTextureNames = Arrays.copyOf(armorTextureNames, 4096);
	}

	@ModifyArg(method = "bindTexture(Lnet/minecraft/entity/player/PlayerEntity;IF)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;bindTexture(Ljava/lang/String;)V"))
	private String faithful$modArmor(String path, @Local(argsOnly = true, ordinal = 0) int slot, @Local ArmorItem item) {
		String mod = Armor.path(item.textureIndex, slot == 2 ? 2 : 1);
		return mod != null ? mod : path;
	}
}
