package org.studioquire.oldfaithful.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Channel;

/**
 * Attachments' values, kept on the game's Entity and Chunk objects. What's on disk and not understood (a removed mod's values, one
 * that won't decode) is held as it came and written back. On disk:
 * {@code Faithful: {Attachments: {"mod:name": {...}}}} in an entity's or player's NBT, {@code Level.Faithful.Attachments} in a chunk's.
 */
public final class Attachments {
	private static final String KEY = "Attachments";
	/** A value on the wire, and well under what the channel may carry. */
	public static final int MAX_VALUE_BYTES = 16 * 1024;
	public static final byte TO_SELF = 0, TO_ENTITY = 1, TO_CHUNK = 2;

	/** Implemented by the game's Entity and Chunk. */
	public interface Holder {
		Data faithful$attachments();

		void faithful$setAttachments(Data data);
	}

	public static final class Data {
		final Map<Attachment<?, ?>, Object> values = new HashMap<>();
		/** The unread part of the {@code Attachments} compound. */
		NbtCompound kept;
		/** An entity's {@code Faithful} element, without {@code Attachments}: what the core doesn't read there. */
		NbtElement root;
	}

	private static final Map<String, Attachment<?, ?>> ALL = new TreeMap<>();
	private static Channel channel;
	private static Channel chunkChannel;

	private Attachments() {
	}

	/** A mod's declaration. */
	static synchronized <H, T> Attachment<H, T> declare(String id, Attachment.Spec<H, T> spec) {
		Attachment<H, T> a = new Attachment<>(id, spec);
		if (ALL.putIfAbsent(id, a) != null) throw new IllegalStateException("The attachment " + id + " is declared twice");
		return a;
	}

	static void start() {
		channel = CoreNet.channel(Core.ID + ":attachments", 1).maxBytes(MAX_VALUE_BYTES + 1024);
		chunkChannel = CoreNet.channel(Core.ID + ":chunk-attachments", 1).world().maxBytes(MAX_VALUE_BYTES + 1024);
		ChunkData.section("attachments", new ChunkData.Section() {
			@Override
			public void load(Chunk chunk, NbtCompound faithful) {
				NbtElement found = ChunkData.find(faithful, KEY);
				if (!(found instanceof NbtCompound)) return;
				ChunkData.remove(faithful, KEY);
				read(data((Holder) chunk), (NbtCompound) found);
			}

			@Override
			public void save(Chunk chunk, NbtCompound faithful) {
				Data data = ((Holder) chunk).faithful$attachments();
				if (data == null) return;
				NbtCompound written = write(data);
				if (written != null) faithful.put(KEY, written);
				else ChunkData.remove(faithful, KEY);
			}
		});
	}

	public static Channel channel() {
		return channel;
	}

	public static Channel chunkChannel() {
		return chunkChannel;
	}

	public static Attachment<?, ?> byId(String id) {
		return ALL.get(id);
	}

	private static Data data(Holder holder) {
		Data data = holder.faithful$attachments();
		if (data == null) {
			data = new Data();
			holder.faithful$setAttachments(data);
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	public static <H, T> T get(H holder, Attachment<H, T> attachment) {
		Data data = ((Holder) holder).faithful$attachments();
		return data == null ? null : (T) data.values.get(attachment);
	}

	public static <H, T> void set(H holder, Attachment<H, T> attachment, T value) {
		store(holder, attachment, value);
		if (holder instanceof Chunk) ((Chunk) holder).dirty = true;
		if (attachment.synced() != Attachment.Sync.NONE && CoreNet.side().runsServer()) send(holder, attachment);
	}

	/** Without telling anyone: a value arriving from the server. */
	public static void store(Object holder, Attachment<?, ?> attachment, Object value) {
		if (value == null) {
			Data data = ((Holder) holder).faithful$attachments();
			if (data != null) data.values.remove(attachment);
		} else {
			data((Holder) holder).values.put(attachment, value);
		}
	}

	private static void read(Data data, NbtCompound attachments) {
		NbtCompound kept = new NbtCompound();
		for (Object o : attachments.values()) {
			NbtElement e = (NbtElement) o;
			Attachment<?, ?> a = ALL.get(e.getKey());
			if (a != null && a.persists() && e instanceof NbtCompound) {
				try {
					Object value = a.codec().read((NbtCompound) e);
					if (value != null) {
						data.values.put(a, value);
						continue;
					}
				} catch (RuntimeException ex) {
					System.out.println("[faithful-core] WARN The attachment " + a.id() + " wouldn't read, and is kept as it was: " + ex);
				}
			}
			kept.put(e.getKey(), e);
		}
		data.kept = kept.values().isEmpty() ? null : kept;
	}

	@SuppressWarnings("unchecked")
	private static NbtCompound write(Data data) {
		NbtCompound out = new NbtCompound();
		if (data.kept != null) {
			for (Object o : data.kept.values()) out.put(((NbtElement) o).getKey(), (NbtElement) o);
		}
		for (Map.Entry<Attachment<?, ?>, Object> e : data.values.entrySet()) {
			Attachment<?, Object> a = (Attachment<?, Object>) e.getKey();
			if (!a.persists()) continue;
			try {
				out.put(a.id(), a.codec().write(e.getValue()));
			} catch (RuntimeException ex) {
				System.out.println("[faithful-core] WARN The attachment " + a.id() + " wouldn't save: " + ex);
			}
		}
		return out.values().isEmpty() ? null : out;
	}

	/** The core's: at the end of {@code Entity.read}. */
	public static void readEntity(Entity entity, NbtCompound nbt) {
		NbtElement found = ChunkData.find(nbt, ChunkData.KEY);
		if (found == null) return;
		Data data = data((Holder) entity);
		if (found instanceof NbtCompound) {
			NbtCompound root = (NbtCompound) found;
			NbtElement attachments = ChunkData.find(root, KEY);
			if (attachments instanceof NbtCompound) {
				ChunkData.remove(root, KEY);
				read(data, (NbtCompound) attachments);
			}
			data.root = root.values().isEmpty() ? null : root;
		} else {
			data.root = found;
		}
	}

	/** The core's: at the end of {@code Entity.write}. Nothing to say writes no key. */
	public static void writeEntity(Entity entity, NbtCompound nbt) {
		Data data = ((Holder) entity).faithful$attachments();
		if (data == null) return;
		if (data.root != null && !(data.root instanceof NbtCompound)) {
			nbt.put(ChunkData.KEY, data.root);
			return;
		}
		NbtCompound root = new NbtCompound();
		if (data.root != null) {
			for (Object o : ((NbtCompound) data.root).values()) root.put(((NbtElement) o).getKey(), (NbtElement) o);
		}
		NbtCompound written = write(data);
		if (written != null) root.put(KEY, written);
		if (!root.values().isEmpty()) nbt.put(ChunkData.KEY, root);
	}

	/** The core's: at a death that isn't keeping the inventory. What falls from {@link Attachment.OnDeath#DROP} values, which go. */
	@SuppressWarnings("unchecked")
	public static List<ItemStack> deathDrops(PlayerEntity player) {
		Data data = ((Holder) player).faithful$attachments();
		if (data == null) return new ArrayList<>();
		List<ItemStack> stacks = new ArrayList<>();
		for (Iterator<Map.Entry<Attachment<?, ?>, Object>> i = data.values.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry<Attachment<?, ?>, Object> e = i.next();
			Attachment<?, Object> a = (Attachment<?, Object>) e.getKey();
			if (a.kind() != Attachment.Kind.PLAYER || a.deathPolicy() != Attachment.OnDeath.DROP) continue;
			try {
				stacks.addAll(a.stacksOf(e.getValue()));
			} catch (RuntimeException ex) {
				System.out.println("[faithful-core] WARN The attachment " + a.id() + " wouldn't say what falls: " + ex);
			}
			i.remove();
		}
		return stacks;
	}

	/** The core's: the new object a respawn makes takes what its policies carry, and whatever was unread. */
	public static void carryOver(PlayerEntity dead, PlayerEntity reborn) {
		Data from = ((Holder) dead).faithful$attachments();
		if (from == null) return;
		Data to = data((Holder) reborn);
		to.kept = from.kept;
		to.root = from.root;
		for (Map.Entry<Attachment<?, ?>, Object> e : from.values.entrySet()) {
			Attachment<?, ?> a = e.getKey();
			if (a.kind() == Attachment.Kind.PLAYER && a.deathPolicy() != Attachment.OnDeath.RESET) to.values.put(a, e.getValue());
		}
	}

	private static byte[] encode(byte target, Attachment<?, Object> a, Object value, int first, int second) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) {
			out.writeByte(target);
			out.writeInt(first);
			out.writeInt(second);
			out.writeUTF(a.id());
			if (value == null) {
				out.writeInt(-1);
			} else {
				ByteArrayOutputStream nbt = new ByteArrayOutputStream();
				try (DataOutputStream nbtOut = new DataOutputStream(nbt)) {
					NbtIo.write(a.codec().write(value), nbtOut);
				}
				if (nbt.size() > MAX_VALUE_BYTES) {
					System.out.println("[faithful-core] WARN The attachment " + a.id() + " is " + nbt.size() + " bytes: too large to send, so clients don't get it");
					return null;
				}
				out.writeInt(nbt.size());
				nbt.writeTo(out);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

	/** Reads one message: the holder is {@code resolve}'s to find. Throws on anything malformed: the channel disconnects for that. */
	public static void receive(DataInputStream in, Resolver resolver) throws IOException {
		byte target = in.readByte();
		int first = in.readInt();
		int second = in.readInt();
		String id = in.readUTF();
		int length = in.readInt();
		Attachment<?, ?> a = ALL.get(id);
		Object value = null;
		if (length >= 0) {
			if (length > MAX_VALUE_BYTES) throw new IOException("An attachment of " + length + " bytes");
			byte[] bytes = new byte[length];
			in.readFully(bytes);
			if (a == null) return;
			NbtBounds.check(bytes);
			value = a.codec().read(NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes))));
		}
		if (a == null) return;
		Object holder = resolver.resolve(target, first, second);
		if (holder != null) store(holder, a, value);
	}

	public interface Resolver {
		Object resolve(byte target, int first, int second);
	}

	@SuppressWarnings("unchecked")
	private static void send(Object holder, Attachment<?, ?> attachment) {
		Attachment<?, Object> a = (Attachment<?, Object>) attachment;
		Object value = get(holder, (Attachment<Object, Object>) attachment);
		if (holder instanceof Chunk) {
			Chunk chunk = (Chunk) holder;
			byte[] message = encode(TO_CHUNK, a, value, chunk.x, chunk.z);
			if (message != null) {
				for (PlayerEntity p : CoreNet.side().watching(chunk)) chunkChannel.toPlayer(p, out -> out.write(message));
			}
			return;
		}
		Entity entity = (Entity) holder;
		if (entity instanceof PlayerEntity) sendSelf((PlayerEntity) entity, a, value);
		if (a.synced() == Attachment.Sync.TRACKING) {
			byte[] message = encode(TO_ENTITY, a, value, entity.id, 0);
			if (message != null) {
				for (PlayerEntity p : CoreNet.side().watching(entity)) channel.toPlayer(p, out -> out.write(message));
			}
		}
	}

	private static void sendSelf(PlayerEntity player, Attachment<?, Object> a, Object value) {
		byte[] message = encode(TO_SELF, a, value, 0, 0);
		if (message != null) channel.toPlayer(player, out -> out.write(message));
	}

	/** The core's, on a server: a player's client has a new player object, or has just joined. Everything it may know of itself. */
	@SuppressWarnings("unchecked")
	public static void sendAllToSelf(PlayerEntity player) {
		Data data = ((Holder) player).faithful$attachments();
		if (data == null) return;
		for (Map.Entry<Attachment<?, ?>, Object> e : data.values.entrySet()) {
			if (e.getKey().synced() != Attachment.Sync.NONE) sendSelf(player, (Attachment<?, Object>) e.getKey(), e.getValue());
		}
	}

	/** The core's, on a server: {@code viewer} has just been told of this entity. */
	@SuppressWarnings("unchecked")
	public static void sendTracked(Entity entity, PlayerEntity viewer) {
		Data data = ((Holder) entity).faithful$attachments();
		if (data == null) return;
		for (Map.Entry<Attachment<?, ?>, Object> e : data.values.entrySet()) {
			if (e.getKey().synced() != Attachment.Sync.TRACKING) continue;
			byte[] message = encode(TO_ENTITY, (Attachment<?, Object>) e.getKey(), e.getValue(), entity.id, 0);
			if (message != null) channel.toPlayer(viewer, out -> out.write(message));
		}
	}

	/** The core's, on a server: {@code viewer} has just been sent this chunk. */
	@SuppressWarnings("unchecked")
	public static void sendChunk(Chunk chunk, PlayerEntity viewer) {
		Data data = ((Holder) chunk).faithful$attachments();
		if (data == null) return;
		for (Map.Entry<Attachment<?, ?>, Object> e : data.values.entrySet()) {
			if (e.getKey().synced() != Attachment.Sync.TRACKING) continue;
			byte[] message = encode(TO_CHUNK, (Attachment<?, Object>) e.getKey(), e.getValue(), chunk.x, chunk.z);
			if (message != null) chunkChannel.toPlayer(viewer, out -> out.write(message));
		}
	}
}
