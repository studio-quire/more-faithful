package org.studioquire.oldfaithful.core.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class NbtBoundsTest {
	private interface Body {
		void write(DataOutputStream out) throws IOException;
	}

	private static byte[] nbt(Body body) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeByte(10);
		out.writeUTF("");
		body.write(out);
		out.writeByte(0);
		return bytes.toByteArray();
	}

	private static void tag(DataOutputStream out, int type, String name) throws IOException {
		out.writeByte(type);
		out.writeUTF(name);
	}

	@Test
	void everyTagTypeReadsThrough() throws IOException {
		byte[] good = nbt(out -> {
			tag(out, 1, "b");
			out.writeByte(1);
			tag(out, 2, "s");
			out.writeShort(1);
			tag(out, 3, "i");
			out.writeInt(1);
			tag(out, 4, "l");
			out.writeLong(1);
			tag(out, 5, "f");
			out.writeFloat(1);
			tag(out, 6, "d");
			out.writeDouble(1);
			tag(out, 7, "a");
			out.writeInt(2);
			out.write(new byte[2]);
			tag(out, 8, "t");
			out.writeUTF("text");
			tag(out, 9, "list");
			out.writeByte(3);
			out.writeInt(2);
			out.writeInt(1);
			out.writeInt(2);
			tag(out, 10, "c");
			out.writeByte(0);
		});
		assertDoesNotThrow(() -> NbtBounds.check(good));
	}

	@Test
	void anArrayLongerThanTheMessageIsRefusedBeforeAnythingAllocates() throws IOException {
		byte[] bad = nbt(out -> {
			tag(out, 7, "a");
			out.writeInt(Integer.MAX_VALUE);
		});
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}

	@Test
	void aNegativeLengthIsRefused() throws IOException {
		byte[] bad = nbt(out -> {
			tag(out, 7, "a");
			out.writeInt(-1);
		});
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}

	@Test
	void aListThatClaimsMoreThanThereIsIsRefused() throws IOException {
		byte[] bad = nbt(out -> {
			tag(out, 9, "list");
			out.writeByte(1);
			out.writeInt(1 << 30);
		});
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}

	@Test
	void deepNestingIsRefused() throws IOException {
		byte[] bad = nbt(out -> {
			for (int i = 0; i < 40; i++) tag(out, 10, "c");
			for (int i = 0; i < 40; i++) out.writeByte(0);
		});
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}

	@Test
	void anUnknownTagIsRefused() throws IOException {
		byte[] bad = nbt(out -> tag(out, 11, "int array"));
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}

	@Test
	void trailingBytesAreRefused() throws IOException {
		byte[] good = nbt(out -> {
		});
		byte[] bad = java.util.Arrays.copyOf(good, good.length + 1);
		assertThrows(IOException.class, () -> NbtBounds.check(bad));
	}
}
