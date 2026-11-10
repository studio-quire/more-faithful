package org.studioquire.oldfaithful.core.api;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.impl.Attachments;
import org.studioquire.oldfaithful.core.impl.SavedChunks;

/**
 * A mod's own data on a player, an entity or a chunk, declared once in {@code init} through {@link Mod#attachment}:
 * <pre>score = mod.attachment("score", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).onDeath(Attachment.OnDeath.KEEP);</pre>
 * Saved under {@code Faithful} in vanilla's own NBT, by the attachment's name, so a world keeps its data when a mod is removed and
 * has it again when the mod returns. A holder with no values writes nothing at all. On a player or a vanilla entity, don't use {@code DataTracker}: it has 32 ids
 * and throws on a duplicate, and every mod shares them. A mod's own entity class is its own to use, from id 16.
 * <p>
 * The side that runs the world owns the values. {@link #set} there is what's saved and sent; on a client on a server, a value is
 * what the server last sent. {@link #get} returns the object itself, and changing that object is seen by nobody until {@link #set}.
 *
 * @param <H> what it's attached to: {@link PlayerEntity}, {@link Entity} or {@link Chunk}
 * @param <T> the value
 */
public final class Attachment<H, T> {
	public enum Kind {
		PLAYER, ENTITY, CHUNK
	}

	/** Who hears of a value, on a server. In single player nobody needs to: both ends are one object. */
	public enum Sync {
		/** Only the side that runs the world knows it. */
		NONE,
		/** The player it's on. Players only. */
		SELF,
		/** Everyone who can see the entity or has the chunk loaded, and the player it's on. */
		TRACKING
	}

	/** What a player's attachment does when they die and respawn as a new object. Entities and chunks don't have this. */
	public enum OnDeath {
		/** The new player has the value. */
		KEEP,
		/** The new player has none: the default, if there is one. The default policy. */
		RESET,
		/**
		 * The value's stacks fall with the player's things (through {@link Players#onDrops}) and the new player has none, unless the
		 * inventory is being kept, when the value is kept with it.
		 */
		DROP
	}

	/** What {@link Mod#attachment} takes. */
	public static final class Spec<H, T> {
		public final Kind kind;
		public final Codec<T> codec;

		private Spec(Kind kind, Codec<T> codec) {
			this.kind = kind;
			this.codec = codec;
		}
	}

	public static <T> Spec<PlayerEntity, T> player(Codec<T> codec) {
		return new Spec<>(Kind.PLAYER, codec);
	}

	/** On any entity, players included, and on that object only: a respawned player is a new object. For a player's own, use {@link #player}. */
	public static <T> Spec<Entity, T> entity(Codec<T> codec) {
		return new Spec<>(Kind.ENTITY, codec);
	}

	public static <T> Spec<Chunk, T> chunk(Codec<T> codec) {
		return new Spec<>(Kind.CHUNK, codec);
	}

	private final String id;
	private final Kind kind;
	private final Codec<T> codec;
	private boolean persist;
	private Sync sync = Sync.NONE;
	private OnDeath onDeath = OnDeath.RESET;
	private Supplier<T> defaults;
	private Function<T, List<ItemStack>> stacks;

	/** The core's. */
	public Attachment(String id, Spec<H, T> spec) {
		this.id = id;
		this.kind = spec.kind;
		this.codec = spec.codec;
	}

	public String id() {
		return id;
	}

	public Kind kind() {
		return kind;
	}

	public Codec<T> codec() {
		return codec;
	}

	public boolean persists() {
		return persist;
	}

	public Sync synced() {
		return sync;
	}

	public OnDeath deathPolicy() {
		return onDeath;
	}

	/** Saved with its holder. Without this, it's gone when the holder is. */
	public Attachment<H, T> persist() {
		persist = true;
		return this;
	}

	public Attachment<H, T> sync(Sync sync) {
		if (sync == Sync.SELF && kind == Kind.CHUNK) throw new IllegalArgumentException(id + ": a chunk has no self to sync to");
		this.sync = sync;
		return this;
	}

	public Attachment<H, T> onDeath(OnDeath policy) {
		if (kind != Kind.PLAYER) throw new IllegalArgumentException(id + ": only a player's attachment has a policy for death");
		onDeath = policy;
		return this;
	}

	/** What {@link #get} gives where there's no value. It isn't stored until someone calls {@link #set}. */
	public Attachment<H, T> defaultValue(Supplier<T> defaults) {
		this.defaults = defaults;
		return this;
	}

	/** For {@link OnDeath#DROP}: what falls. */
	public Attachment<H, T> stacks(Function<T, List<ItemStack>> stacks) {
		this.stacks = stacks;
		return this;
	}

	/** The core's. */
	public List<ItemStack> stacksOf(T value) {
		return stacks == null ? java.util.Collections.emptyList() : stacks.apply(value);
	}

	/** The value, the default if there's none, or null. */
	public T get(H holder) {
		T value = Attachments.get(holder, this);
		return value != null || defaults == null ? value : defaults.get();
	}

	/** Whether a value is set, as opposed to a default. */
	public boolean has(H holder) {
		return Attachments.get(holder, this) != null;
	}

	/** Null takes the value away. */
	public void set(H holder, T value) {
		Attachments.set(holder, this, value);
	}

	/** A chunk's value as last saved, read from the disk with nothing loaded, or null: no such chunk saved, or no value in it. A chunk in memory may have a newer one, so ask there first. */
	public T saved(World world, int chunkX, int chunkZ) {
		if (kind != Kind.CHUNK) throw new IllegalArgumentException(id + ": only a chunk's attachment lies on disk by itself");
		NbtCompound value = SavedChunks.attachment(world, chunkX, chunkZ, id);
		return value == null ? null : codec.read(value);
	}
}
