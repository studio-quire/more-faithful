package org.studioquire.faithful.wolfpacks.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Whether this player may be fought by other players: the server's PvP setting, never in single player. Beta keeps it protected. */
@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
	@Invoker("isPvpEnabled")
	boolean wolfpacks$pvp();
}
