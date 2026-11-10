package org.studioquire.oldfaithful.core.impl.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.studioquire.oldfaithful.core.api.Stacks;

/** Stack data on disk and on the wire. */
public final class StackData {
	private static final int MAX_WIRE_BYTES = Stacks.MAX_WIRE_BYTES;
	/** What one packet's stacks may carry between them: {@code Connection} drops a peer whose send queue passes 1 MB, and a double chest of big stacks would, or a few sent at once. */
	public static final int MAX_PACKET_BYTES = 256 * 1024;
	/** How full a send queue may be and stacks still take their data: a window opening is one packet, then every slot again on its own, and what else is queued needs room too. */
	public static final int MAX_QUEUE_BYTES = 512 * 1024;
	/** In place of a length: this stack has data, which stayed behind. */
	private static final int WITHHELD_LENGTH = 0xFFFF;
	/** On a stack a client received without its data: its size. */
	private static final String WITHHELD_KEY = "faithful-core:withheld";
	private static final byte[] NOTHING = new byte[0];
	private static final Set<Integer> WARNED = ConcurrentHashMap.newKeySet();

	private StackData() {
	}

	/** A packet with stacks in it, told as it joins the queue what its stacks may carry, and holding to that for its size and its bytes. */
	public interface Roomy {
		void faithful$room(int room);
	}

	public static NbtCompound copy(NbtCompound data) {
		if (data == null) return null;
		byte[] bytes = bytes(data);
		return bytes == null ? null : read(bytes);
	}

	/** Compares by what would be saved: b1.7.3's compounds have no equals of their own that can be trusted across types. */
	public static boolean same(NbtCompound a, NbtCompound b) {
		if (withheld(a) || withheld(b)) return true; // a client's copy, and the data it stands for can't be compared
		boolean emptyA = a == null || a.values().isEmpty(), emptyB = b == null || b.values().isEmpty();
		if (emptyA || emptyB) return emptyA == emptyB;
		return Arrays.equals(body(a), body(b));
	}

	/** Without the name of the compound itself, which is only where it last sat in a tree. */
	private static byte[] body(NbtCompound data) {
		byte[] all = bytes(data);
		int nameEnd = 3 + ((all[1] & 0xFF) << 8 | all[2] & 0xFF);
		return Arrays.copyOfRange(all, nameEnd, all.length);
	}

	public static byte[] bytes(NbtCompound data) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			NbtIo.write(data, out);
			out.close();
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static NbtCompound read(byte[] bytes) {
		try {
			return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean withheld(NbtCompound data) {
		return data != null && data.contains(WITHHELD_KEY);
	}

	public static int size(ItemStack stack) {
		NbtCompound data = Stacks.peek(stack);
		return data == null || data.values().isEmpty() ? 0 : bytes(data).length;
	}

	/** What goes on the wire for this stack, in a packet with {@code room} left; null if its data has to stay behind. */
	private static byte[] payload(ItemStack stack, int room) {
		NbtCompound data = Stacks.peek(stack);
		if (data == null || data.values().isEmpty()) return NOTHING;
		byte[] bytes = bytes(data);
		return bytes.length > Math.min(MAX_WIRE_BYTES, room) ? null : bytes;
	}

	/** What a packet's stacks' data add to its size, for the queue's count. */
	public static int wireSize(int room, ItemStack... stacks) {
		int size = 0;
		for (ItemStack s : stacks) {
			if (s == null) continue;
			byte[] bytes = payload(s, room);
			if (bytes == null) {
				size += 6;
				continue;
			}
			room -= bytes.length;
			size += 2 + bytes.length;
		}
		return size;
	}

	/** After a packet's stacks, for a peer that has the content layer: a length and bytes for each. */
	public static void writeTrailer(DataOutputStream out, int room, ItemStack... stacks) throws IOException {
		for (ItemStack stack : stacks) {
			if (stack == null) continue;
			byte[] bytes = payload(stack, room);
			if (bytes == null) {
				int size = size(stack);
				if (WARNED.add(stack.itemId)) {
					System.out.println("[faithful-core] WARN " + (ContentRegistry.itemName(stack.itemId) == null ? "" : ContentRegistry.itemName(stack.itemId) + " ") + "(item " + stack.itemId + ") has " + size + " bytes of stack data, more than a packet has room for: a client sees the item without it");
				}
				out.writeShort(WITHHELD_LENGTH);
				out.writeInt(size);
				continue;
			}
			room -= bytes.length;
			out.writeShort(bytes.length);
			out.write(bytes);
		}
	}

	public static void readTrailer(DataInputStream in, ItemStack... stacks) throws IOException {
		for (ItemStack stack : stacks) {
			if (stack == null) continue;
			int length = in.readUnsignedShort();
			if (length == WITHHELD_LENGTH) {
				NbtCompound marker = new NbtCompound();
				marker.putInt(WITHHELD_KEY, in.readInt());
				((StackDataHolder) (Object) stack).faithful$setData(marker);
				continue;
			}
			if (length > MAX_WIRE_BYTES) throw new IOException("Stack data of " + length + " bytes");
			if (length == 0) continue;
			byte[] bytes = new byte[length];
			in.readFully(bytes);
			if (!SafeNbt.check(bytes)) throw new IOException("Stack data that isn't sound");
			((StackDataHolder) (Object) stack).faithful$setData(read(bytes));
		}
	}
}
