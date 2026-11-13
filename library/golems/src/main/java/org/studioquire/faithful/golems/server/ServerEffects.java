package org.studioquire.faithful.golems.server;

import java.util.function.BiConsumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;

/** What a dedicated server sends for a golem that a client shows by itself: an item flying into it as it's picked up, which beta's client plays for any living collector. */
public final class ServerEffects {
	private ServerEffects() {
	}

	public static BiConsumer<ItemEntity, Entity> pickup() {
		return (item, golem) -> {
			for (Object o : item.world.players) {
				if (!(o instanceof ServerPlayerEntity)) continue;
				((ServerPlayerEntity) o).server.getEntityTracker(item.world.dimension.id).sendToListeners(item, new ItemPickupAnimationS2CPacket(item.id, golem.id));
				return;
			}
		};
	}
}
