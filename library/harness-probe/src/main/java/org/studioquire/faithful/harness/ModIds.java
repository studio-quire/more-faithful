package org.studioquire.faithful.harness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** A thing's name wherever it's from: {@code ns:name}, or vanilla's number. Numbers of mods' things mean nothing between runs. */
public final class ModIds {
	private ModIds() {
	}

	public static String block(int id) {
		String name = ContentRegistry.blockName(id);
		return name != null ? name : String.valueOf(id);
	}

	public static String item(int id) {
		String name = ContentRegistry.itemName(id);
		return name != null ? name : String.valueOf(id);
	}

	/** NBT as text with its keys in order: the same data, the same text. */
	public static String canon(NbtElement element) {
		if (element instanceof NbtCompound) {
			Map<String, String> sorted = new TreeMap<>();
			for (Object o : ((NbtCompound) element).values()) sorted.put(((NbtElement) o).getKey(), canon((NbtElement) o));
			return sorted.toString();
		}
		if (element instanceof NbtList) {
			NbtList list = (NbtList) element;
			List<String> items = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) items.add(canon(list.get(i)));
			return items.toString();
		}
		if (element instanceof NbtByteArray) return "bytes:" + java.util.Arrays.hashCode(((NbtByteArray) element).value);
		return element.toString();
	}
}
