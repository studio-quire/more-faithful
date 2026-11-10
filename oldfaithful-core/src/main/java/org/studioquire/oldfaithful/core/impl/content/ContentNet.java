package org.studioquire.oldfaithful.core.impl.content;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.SyncedBlockEntity;
import org.studioquire.oldfaithful.core.impl.ChannelPacket;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.CoreNet;
import org.studioquire.oldfaithful.core.impl.CoreNet.ChannelImpl;

/** The content layer's own channels. Registered only when it's woven, so a game with no content has none of them. */
public final class ContentNet {
	/** Block entity data, in the order of the chunks it belongs to. */
	public static final int MAX_SYNC_BYTES = 32 * 1024;

	/** The side-specific end of what the channels do; set at the start. */
	public interface Side {
		void openContainer(PlayerEntity player, String name, net.minecraft.inventory.Inventory inventory, NbtCompound data);
	}

	private static ChannelImpl blockEntity;
	public static ChannelImpl spawn;
	public static ChannelImpl openScreen;
	private static Side side;

	private ContentNet() {
	}

	public static Side side() {
		return side;
	}

	public static void start(Side contentSide) {
		side = contentSide;
		blockEntity = CoreNet.channel(Core.ID + ":block_entity", 1);
		blockEntity.world().onClient(ContentNet::blockEntityData);
		spawn = CoreNet.channel(Core.ID + ":spawn", 1);
		openScreen = CoreNet.channel(Core.ID + ":open_screen", 1);
	}

	/** A block entity's update packet, on a server: one packet for everyone in the chunk, who all have this channel on a content server. */
	public static Packet blockEntityPacket(BlockEntity be) {
		NbtCompound nbt = new NbtCompound();
		try {
			((SyncedBlockEntity) be).writeSync(nbt);
		} catch (RuntimeException e) {
			System.out.println("[faithful-core] WARN A block entity at " + be.x + ", " + be.y + ", " + be.z + " couldn't write its sync data: " + e);
			return null;
		}
		byte[] body = StackData.bytes(nbt);
		if (body.length > MAX_SYNC_BYTES) {
			System.out.println("[faithful-core] WARN " + body.length + " bytes of sync data from a block entity at " + be.x + ", " + be.y + ", " + be.z + ": left out");
			return null;
		}
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream(body.length + 10);
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeInt(be.x);
			out.writeShort(be.y);
			out.writeInt(be.z);
			out.write(body);
			return new ChannelPacket(blockEntity.number, bytes.toByteArray(), blockEntity.world);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void blockEntityData(PlayerEntity player, DataInputStream in) throws IOException {
		int x = in.readInt(), y = in.readShort(), z = in.readInt();
		if (in.available() > MAX_SYNC_BYTES) throw new IOException("Too much block entity data");
		byte[] body = new byte[in.available()];
		in.readFully(body);
		NbtCompound nbt = SafeNbt.parse(body);
		World world = player == null ? null : player.world;
		if (world == null || !world.isPosLoaded(x, y, z)) return;
		BlockEntity be = world.getBlockEntity(x, y, z);
		if (be instanceof SyncedBlockEntity) {
			((SyncedBlockEntity) be).readSync(nbt);
			world.setBlockDirty(x, y, z);
		}
	}
}
