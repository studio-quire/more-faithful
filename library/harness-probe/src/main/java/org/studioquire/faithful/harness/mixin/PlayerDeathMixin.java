package org.studioquire.faithful.harness.mixin;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What killed a test player and what stood near, in the log: one player goes through every mod's test, and `ClientTests` notes a test that left them dead
 * without knowing why (2026-11-10: two such deaths in one full run, at Blue Flowers and Stagehand, didn't come again in the full run after).
 */
@Mixin(PlayerEntity.class)
abstract class PlayerDeathMixin {
	@Inject(method = "onKilledBy", at = @At("HEAD"))
	private void harness$died(Entity adversary, CallbackInfo ci) {
		PlayerEntity p = (PlayerEntity) (Object) this;
		StringBuilder s = new StringBuilder("[harness-probe] death: " + p.name + " remote=" + p.world.isRemote + " by "
				+ (adversary == null ? "no entity" : adversary.getClass().getSimpleName() + " at " + (int) adversary.x + " " + (int) adversary.y + " " + (int) adversary.z)
				+ "; at " + p.x + " " + p.y + " " + p.z + " fire " + p.fireTicks + " air " + p.air + " time " + p.world.getTime() + "; near:");
		List<?> near = p.world.getEntities(p, Box.createCached(p.x - 16, p.y - 16, p.z - 16, p.x + 16, p.y + 16, p.z + 16));
		for (Object o : near) {
			Entity e = (Entity) o;
			s.append(' ').append(e.getClass().getSimpleName()).append('@').append(String.format("%.1f", Math.sqrt(e.getSquaredDistance(p))));
		}
		System.out.println(s);
	}
}
