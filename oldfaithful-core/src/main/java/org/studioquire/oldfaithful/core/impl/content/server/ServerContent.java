package org.studioquire.oldfaithful.core.impl.content.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.oldfaithful.core.impl.content.ContentNet;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.StackData;
import org.studioquire.oldfaithful.core.mixin.content.server.ServerPlayerEntityAccessor;

/** The content layer's server end. */
public final class ServerContent implements ContentNet.Side {
	@Override
	public void openContainer(PlayerEntity p, String name, Inventory inventory, NbtCompound data) {
		ServerPlayerEntity player = (ServerPlayerEntity) p;
		byte[] nbt = StackData.bytes(data);
		if (nbt.length > ContentNet.MAX_SYNC_BYTES) throw new IllegalArgumentException(nbt.length + " bytes of screen data for " + name);
		if (player.currentScreenHandler != player.playerScreenHandler) player.onHandledScreenClosed();
		ScreenHandler handler = ContentRegistry.screen(name).create(player, inventory, data);
		ServerPlayerEntityAccessor sync = (ServerPlayerEntityAccessor) player;
		sync.faithful$nextSyncId();
		int syncId = sync.faithful$syncId();
		ContentNet.openScreen.toPlayer(player, out -> {
			out.writeUTF(name);
			out.writeByte(syncId);
			out.writeUTF(inventory.getName());
			out.writeShort(inventory.size());
			out.writeShort(handler.slots.size());
			out.writeShort(nbt.length);
			out.write(nbt);
		});
		player.currentScreenHandler = handler;
		handler.syncId = syncId;
		handler.addListener(player);
	}
}
