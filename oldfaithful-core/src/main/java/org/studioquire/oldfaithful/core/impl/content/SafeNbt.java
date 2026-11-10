package org.studioquire.oldfaithful.core.impl.content;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Walks NBT from the network before the game's parser sees it: every length bounded by what's left, depth bounded, tag types known.
 * b1.7.3's parser takes lengths on trust, so one bad array is an OutOfMemoryError.
 */
public final class SafeNbt {
	private static final int MAX_DEPTH = 32;

	private SafeNbt() {
	}

	public static boolean check(byte[] bytes) {
		try {
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
			int type = in.readUnsignedByte();
			if (type != 10) return false;
			skipName(in);
			walk(in, 10, 0);
			return true;
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}

	/** Bytes from the network as a compound, or an exception: the game's parser never sees what didn't pass {@link #check}. */
	public static net.minecraft.nbt.NbtCompound parse(byte[] bytes) throws IOException {
		if (!check(bytes)) throw new IOException("NBT that isn't sound");
		return StackData.read(bytes);
	}

	private static void skipName(DataInputStream in) throws IOException {
		skip(in, in.readUnsignedShort());
	}

	private static void skip(DataInputStream in, long n) throws IOException {
		if (n < 0 || n > in.available()) throw new IOException("past the end");
		in.skipBytes((int) n);
	}

	private static void walk(DataInputStream in, int type, int depth) throws IOException {
		if (depth > MAX_DEPTH) throw new IOException("too deep");
		switch (type) {
			case 1: skip(in, 1); break;
			case 2: skip(in, 2); break;
			case 3: case 5: skip(in, 4); break;
			case 4: case 6: skip(in, 8); break;
			case 7: skip(in, in.readInt()); break;
			case 8: skip(in, in.readUnsignedShort()); break;
			case 9: {
				int inner = in.readUnsignedByte();
				int n = in.readInt();
				if (n < 0 || n > in.available()) throw new IOException("a list longer than what's left");
				if (n > 0 && (inner < 1 || inner > 10)) throw new IOException("unknown list type");
				for (int i = 0; i < n; i++) walk(in, inner, depth + 1);
				break;
			}
			case 10:
				while (true) {
					int t = in.readUnsignedByte();
					if (t == 0) break;
					if (t > 10) throw new IOException("unknown tag");
					skipName(in);
					walk(in, t, depth + 1);
				}
				break;
			default:
				throw new IOException("unknown tag");
		}
	}
}
