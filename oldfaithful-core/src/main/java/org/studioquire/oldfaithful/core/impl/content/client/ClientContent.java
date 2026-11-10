package org.studioquire.oldfaithful.core.impl.content.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.ClientWorld;
import org.studioquire.oldfaithful.core.api.EntitySpawnData;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.api.client.ContainerScreens;
import org.studioquire.oldfaithful.core.api.client.Screens;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;
import org.studioquire.oldfaithful.core.impl.content.ContentNet;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.EntityContent;
import org.studioquire.oldfaithful.core.impl.content.SafeNbt;

/** The content layer's client end. Everything read here came from a server: a name must be one we declared, and every size is bounded. */
public final class ClientContent implements ContentNet.Side {
	private static final int MAX_INVENTORY = 1024;
	private static final int MAX_TITLE = 256;
	private static final Map<String, ContainerScreens.Factory> SCREENS = new HashMap<>();

	/** Once the channels exist. */
	public static void start() {
		ContentNet.spawn.onClient(ClientContent::spawn);
		ContentNet.openScreen.onClient(ClientContent::openScreen);
		Screens.onTooltip(t -> {
			if (Stacks.withheld(t.stack)) t.lines.add("\u00a77Too much data to show here");
		});
	}

	public static void screen(String name, ContainerScreens.Factory factory) {
		if (SCREENS.putIfAbsent(name, factory) != null) throw new IllegalStateException("The screen " + name + " has two client screens");
	}

	private static NbtCompound nbt(DataInputStream in) throws IOException {
		int length = in.readUnsignedShort();
		if (length > ContentNet.MAX_SYNC_BYTES) throw new IOException(length + " bytes of data");
		if (length == 0) return new NbtCompound();
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return SafeNbt.parse(bytes);
	}

	private static void spawn(PlayerEntity player, DataInputStream in) throws IOException {
		int id = in.readInt();
		String name = in.readUTF();
		int x = in.readInt(), y = in.readInt(), z = in.readInt();
		float yaw = in.readByte() * 360 / 256.0F, pitch = in.readByte() * 360 / 256.0F;
		int vx = in.readShort(), vy = in.readShort(), vz = in.readShort();
		List<?> tracked = DataTracker.readEntries(in);
		NbtCompound data = nbt(in);
		if (!EntityContent.declared(name)) throw new IOException("The server spawned an entity this game doesn't have: " + name);
		Minecraft mc = ClientNet.minecraft;
		if (!(mc.world instanceof ClientWorld)) return;
		Entity entity = EntityRegistry.create(name, mc.world);
		if (entity == null) throw new IOException("Couldn't make the entity " + name);
		entity.trackedPosX = x;
		entity.trackedPosY = y;
		entity.trackedPosZ = z;
		entity.id = id;
		entity.setPositionAndAngles(x / 32.0, y / 32.0, z / 32.0, yaw, pitch);
		if (entity instanceof LivingEntity) ((LivingEntity) entity).interpolateOnly = true;
		if (entity instanceof EntitySpawnData) ((EntitySpawnData) entity).readSpawnData(data);
		((ClientWorld) mc.world).forceEntity(id, entity);
		if (tracked != null) entity.getDataTracker().writeUpdatedEntries(tracked);
		if (vx != 0 || vy != 0 || vz != 0) entity.setVelocityClient(vx / 8000.0, vy / 8000.0, vz / 8000.0);
	}

	private static void openScreen(PlayerEntity player, DataInputStream in) throws IOException {
		String name = in.readUTF();
		int syncId = in.readUnsignedByte();
		String title = in.readUTF();
		int size = in.readUnsignedShort(), slots = in.readUnsignedShort();
		NbtCompound data = nbt(in);
		if (title.length() > MAX_TITLE || size > MAX_INVENTORY) throw new IOException("A screen too big to be honest");
		if (ContentRegistry.screen(name) == null) throw new IOException("The server opened a screen this game doesn't have: " + name);
		show(player, name, new SimpleInventory(title, size), data, slots).syncId = syncId;
	}

	private static ScreenHandler show(PlayerEntity player, String name, Inventory inventory, NbtCompound data, int slots) throws IOException {
		ContainerScreens.Factory client = SCREENS.get(name);
		if (client == null) throw new IOException("No client screen for " + name);
		ScreenHandler handler = ContentRegistry.screen(name).create(player, inventory, data);
		// updateSlotStacks throws on a count that differs, and a slot past the inventory's end throws in the next frame: here it's a plain message and a disconnect.
		if (slots >= 0 && handler.slots.size() != slots) throw new IOException("The screen " + name + " has " + handler.slots.size() + " slots here and " + slots + " on the server");
		for (Object slot : handler.slots) ((Slot) slot).getStack();
		ClientNet.minecraft.setScreen(client.create(player, handler, inventory, data));
		player.currentScreenHandler = handler;
		return handler;
	}

	@Override
	public void openContainer(PlayerEntity player, String name, Inventory inventory, NbtCompound data) {
		if (player.world.isRemote) return;
		try {
			show(player, name, inventory, data, -1);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
