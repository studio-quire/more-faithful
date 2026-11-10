package org.studioquire.oldfaithful.core.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * NBT that came over the wire, walked before the game's reader sees it: that reader allocates what a length says and recurses as
 * deep as the data goes. Every length here is checked against the bytes actually there, and depth is capped.
 */
public final class NbtBounds {
	private static final int MAX_DEPTH = 32;

	private NbtBounds() {
	}

	public static void check(byte[] bytes) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		if (in.readByte() != 10) throw new IOException("NBT that isn't a compound");
		skip(in, in.readUnsignedShort());
		payload(in, 10, 0);
		if (in.available() != 0) throw new IOException("Bytes after the end of the NBT");
	}

	private static void skip(DataInputStream in, long n) throws IOException {
		if (n < 0 || n > in.available()) throw new IOException("NBT that says it's longer than it is");
		in.skipBytes((int) n);
	}

	private static void payload(DataInputStream in, int type, int depth) throws IOException {
		if (depth > MAX_DEPTH) throw new IOException("NBT nested more than " + MAX_DEPTH + " deep");
		switch (type) {
			case 1: skip(in, 1); break;
			case 2: skip(in, 2); break;
			case 3: case 5: skip(in, 4); break;
			case 4: case 6: skip(in, 8); break;
			case 7: skip(in, in.readInt()); break;
			case 8: skip(in, in.readUnsignedShort()); break;
			case 9: {
				int elements = in.readByte();
				int count = in.readInt();
				if (count < 0 || count > in.available() || elements <= 0 && count > 0 || elements > 10) throw new IOException("A bad NBT list");
				for (int i = 0; i < count; i++) payload(in, elements, depth + 1);
				break;
			}
			case 10: {
				for (int tag = in.readByte(); tag != 0; tag = in.readByte()) {
					if (tag < 0 || tag > 10) throw new IOException("An unknown NBT tag " + tag);
					skip(in, in.readUnsignedShort());
					payload(in, tag, depth + 1);
				}
				break;
			}
			default:
				throw new IOException("An unknown NBT tag " + type);
		}
	}
}
