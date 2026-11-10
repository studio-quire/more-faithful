package org.studioquire.oldfaithful.core.impl.content;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.MathHelper;
import org.studioquire.oldfaithful.core.api.EntityEntry;
import org.studioquire.oldfaithful.core.api.EntitySpawnData;
import org.studioquire.oldfaithful.core.impl.ChannelPacket;
import org.studioquire.oldfaithful.core.mixin.content.EntityRegistryInvoker;

/**
 * Mods' entities: {@code ns:name} in vanilla's own maps, tracked and spawned by name. A registered class skips vanilla's spawn packets, which
 * know a fixed list of classes and would send a mod's boat subclass as a boat.
 */
public final class EntityContent {
	/** Vanilla's raw ids stop at 95. Only its own spawn packet reads one, and that's never used for these. */
	private static final int FIRST_RAW_ID = 1000;

	public static final class Entry implements EntityEntry {
		final String name;
		final Class<? extends Entity> type;
		public int range = 160;
		public int rate = 3;
		public boolean velocity;

		Entry(String name, Class<? extends Entity> type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public EntityEntry tracking(int range, int rate) {
			if (range <= 0 || rate <= 0) throw new IllegalArgumentException("A tracking range and rate must be above 0, for " + name);
			this.range = range;
			this.rate = rate;
			return this;
		}

		@Override
		public EntityEntry sendVelocity() {
			velocity = true;
			return this;
		}
	}

	private static final Map<String, Entry> BY_NAME = new TreeMap<>();
	/** What each class seen so far is, nulls included. */
	private static final Map<Class<?>, Entry> BY_CLASS = new HashMap<>();

	private EntityContent() {
	}

	static Entry declare(String name, Class<? extends Entity> type) {
		Entry e = new Entry(name, type);
		if (BY_NAME.putIfAbsent(name, e) != null) throw new IllegalStateException("The entity " + name + " is declared twice");
		if (BY_CLASS.putIfAbsent(type, e) != null) throw new IllegalStateException("The entity class " + type.getName() + " is declared twice");
		return e;
	}

	static boolean any() {
		return !BY_NAME.isEmpty();
	}

	static void build() {
		int raw = FIRST_RAW_ID;
		for (Entry e : BY_NAME.values()) EntityRegistryInvoker.faithful$register(e.type, e.name, raw++);
	}

	/** A name an entity used to have: saves that hold it load as what it is now. Vanilla's map says which class; the class says the new name. */
	@SuppressWarnings("unchecked")
	static void alias(String oldName, String name) {
		Entry e = BY_NAME.get(name);
		if (e != null) EntityRegistryInvoker.faithful$idToClass().put(oldName, e.type);
	}

	static void hashLines(List<String> lines) {
		for (Entry e : BY_NAME.values()) lines.add("entity:" + e.name);
	}

	public static boolean declared(String name) {
		return BY_NAME.containsKey(name);
	}

	/** The registered class this entity is, or is a subclass of; null for anything else. */
	public static Entry of(Entity entity) {
		Class<?> type = entity.getClass();
		if (BY_CLASS.containsKey(type)) return BY_CLASS.get(type);
		Class<?> walk = type.getSuperclass();
		while (walk != null && !BY_CLASS.containsKey(walk)) walk = walk.getSuperclass();
		Entry found = walk == null ? null : BY_CLASS.get(walk);
		BY_CLASS.put(type, found);
		return found;
	}

	private static int velocity(double v) {
		return (int) (Math.max(-3.9, Math.min(3.9, v)) * 8000.0);
	}

	/** The spawn message for one client: position in 1/32 blocks, as vanilla's packets have it. */
	public static Packet spawnPacket(Entity entity, Entry entry) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeInt(entity.id);
			out.writeUTF(entry.name);
			out.writeInt(MathHelper.floor(entity.x * 32.0));
			out.writeInt(MathHelper.floor(entity.y * 32.0));
			out.writeInt(MathHelper.floor(entity.z * 32.0));
			out.writeByte((int) (entity.yaw * 256.0F / 360.0F));
			out.writeByte((int) (entity.pitch * 256.0F / 360.0F));
			out.writeShort(velocity(entity.velocityX));
			out.writeShort(velocity(entity.velocityY));
			out.writeShort(velocity(entity.velocityZ));
			entity.getDataTracker().writeAllEntries(out);
			byte[] data = new byte[0];
			if (entity instanceof EntitySpawnData) {
				NbtCompound nbt = new NbtCompound();
				try {
					((EntitySpawnData) entity).writeSpawnData(nbt);
				} catch (RuntimeException e) {
					System.out.println("[faithful-core] WARN " + entry.name + " couldn't write its spawn data: " + e);
				}
				data = StackData.bytes(nbt);
				if (data.length > ContentNet.MAX_SYNC_BYTES) {
					System.out.println("[faithful-core] WARN " + data.length + " bytes of spawn data from " + entry.name + ": left out");
					data = new byte[0];
				}
			}
			out.writeShort(data.length);
			out.write(data);
			return new ChannelPacket(ContentNet.spawn.number, bytes.toByteArray(), false);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
