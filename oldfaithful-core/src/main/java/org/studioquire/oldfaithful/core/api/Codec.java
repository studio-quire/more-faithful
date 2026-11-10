package org.studioquire.oldfaithful.core.api;

import java.util.function.Function;

import net.minecraft.nbt.NbtCompound;

/**
 * How a value goes to and from NBT, for saves and the wire. Beta's NBT has no int or long arrays: an unknown tag fails vanilla's
 * parse of the whole chunk, so a compound's contents stay to beta's types, which NbtCompound's own calls keep to.
 */
public interface Codec<T> {
	NbtCompound write(T value);

	/** A throw leaves the data as it was on disk, unread, and is reported. */
	T read(NbtCompound nbt);

	Codec<Integer> INT = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putInt("v", v);
		return n;
	}, n -> n.getInt("v"));

	Codec<Long> LONG = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putLong("v", v);
		return n;
	}, n -> n.getLong("v"));

	Codec<Double> DOUBLE = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putDouble("v", v);
		return n;
	}, n -> n.getDouble("v"));

	Codec<Boolean> BOOL = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putBoolean("v", v);
		return n;
	}, n -> n.getBoolean("v"));

	Codec<String> STRING = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putString("v", v);
		return n;
	}, n -> n.getString("v"));

	Codec<byte[]> BYTES = of(v -> {
		NbtCompound n = new NbtCompound();
		n.putByteArray("v", v);
		return n;
	}, n -> n.getByteArray("v"));

	/** The compound itself, for a mod that wants its own layout. */
	Codec<NbtCompound> COMPOUND = of(v -> v, n -> n);

	static <T> Codec<T> of(Function<T, NbtCompound> write, Function<NbtCompound, T> read) {
		return new Codec<T>() {
			@Override
			public NbtCompound write(T value) {
				return write.apply(value);
			}

			@Override
			public T read(NbtCompound nbt) {
				return read.apply(nbt);
			}
		};
	}
}
