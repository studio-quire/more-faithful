package org.studioquire.oldfaithful.core.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ArmorItem;

/**
 * Armor:
 * <pre>{@code
 * c.item("my_helmet", Armor.piece("my-mod:my_set", Armor.HEAD, 9, "my_helmet")).standIn(Item.IRON_HELMET);
 * }</pre>
 * {@code my-mod:my_set} is drawn from {@code assets/my-mod/textures/armor/my_set_1.png} and {@code my_set_2.png}, laid out as vanilla's two sheets are. Beta has one protection per slot, whatever it's
 * made of, so a set differs by its look and its durability. {@code ArmorItem} keeps a number for its look, an index into a table of five names, which {@link #texture} extends; the numbers aren't
 * saved or sent.
 */
public final class Armor {
	public static final int HEAD = 0, CHEST = 1, LEGS = 2, FEET = 3;
	/** {@link #piece}'s durability, in vanilla's sets. */
	public static final int LEATHER = 3, CHAIN = 6, IRON = 12, DIAMOND = 24;
	private static final int[] SLOT_DURABILITY = {11, 16, 15, 13};
	private static final int FIRST = 5;
	private static final List<String> NAMES = new ArrayList<>();

	private Armor() {
	}

	private static final class Piece extends ArmorItem {
		Piece(int id, int look, int slot, int durability) {
			super(id, 0, look, slot);
			setMaxDamage(SLOT_DURABILITY[slot] * durability);
		}
	}

	/**
	 * A piece of a set.
	 * @param set a full name, for {@link #texture}
	 * @param slot {@link #HEAD} to {@link #FEET}
	 * @param durability a multiple of the slot's own 11, 16, 15 or 13, any value: {@link #LEATHER} 3, {@link #CHAIN} 6, {@link #IRON} 12, {@link #DIAMOND} 24, or 9 for halfway from chain to iron
	 * @param itemTexture the item's texture
	 */
	public static Content.ItemFactory<ArmorItem> piece(String set, int slot, int durability, String itemTexture) {
		int look = texture(set);
		return s -> {
			ArmorItem item = new Piece(s.id(), look, slot, durability);
			item.setTextureId(s.texture(itemTexture));
			return item;
		};
	}

	/** The {@code textureIndex} of a set, by a full name. In common code. */
	public static synchronized int texture(String name) {
		if (name.indexOf(':') <= 0) throw new IllegalArgumentException("An armor texture's name is a full one: " + name);
		int at = NAMES.indexOf(name);
		if (at < 0) {
			at = NAMES.size();
			NAMES.add(name);
		}
		return FIRST + at;
	}

	/** The path of a sheet ({@code 1} or {@code 2}), or null for one of vanilla's. */
	public static synchronized String path(int textureIndex, int sheet) {
		if (textureIndex < FIRST || textureIndex - FIRST >= NAMES.size()) return null;
		String name = NAMES.get(textureIndex - FIRST);
		int colon = name.indexOf(':');
		return "/assets/" + name.substring(0, colon) + "/textures/armor/" + name.substring(colon + 1) + "_" + sheet + ".png";
	}
}
