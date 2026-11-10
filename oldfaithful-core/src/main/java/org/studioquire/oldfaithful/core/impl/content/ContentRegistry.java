package org.studioquire.oldfaithful.core.impl.content;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.EntityEntry;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.impl.ChunkData;
import org.studioquire.oldfaithful.core.mixin.content.BlockEntityInvoker;

/**
 * Names and numbers. Numbers come from sorted names at each start and are never written to a save: same content, same numbers.
 * Blocks and items share vanilla's one number space, so {@code ITEMS[blockId]} stays a block's item: library blocks from 512
 * (clear of vanilla's items), library items from 4096. A test-only salt shifts everything, to show nothing holds on to a number.
 */
public final class ContentRegistry {
	public static final int FIRST_BLOCK = 512, BLOCK_LIMIT = 4096, FIRST_ITEM = 4096, ITEM_LIMIT = 32000;
	private static final String SALT = System.getProperty("faithful.test.numberSalt", "");

	/** Fast tables for the overlay. */
	public static final boolean[] MOD_BLOCK = new boolean[BLOCK_LIMIT];
	public static final int[] STAND_IN = new int[BLOCK_LIMIT]; // id | meta << 8
	private static final String[] BLOCK_NAME = new String[BLOCK_LIMIT];
	/** The atlas each block's tiles are all in: 0 for vanilla's sheet. */
	public static final int[] BLOCK_ATLAS = new int[BLOCK_LIMIT];
	private static final Map<Integer, String> ITEM_NAME = new HashMap<>();
	private static final Map<String, Integer> BLOCK_IDS = new HashMap<>();
	private static final Map<String, Integer> ITEM_IDS = new HashMap<>();
	private static final Map<String, String> ALIASES = new HashMap<>();
	/** Item stand-ins: id | damage << 16, by item id. */
	private static final Map<Integer, Integer> ITEM_STAND_IN = new HashMap<>();

	private static final Map<String, DeclaredBlock<?>> BLOCKS = new TreeMap<>();
	private static final Map<String, DeclaredItem<?>> ITEMS = new TreeMap<>();
	private static final List<String[]> FAMILIES = new ArrayList<>();
	private static final Map<String, Class<? extends BlockEntity>> BLOCK_ENTITIES = new TreeMap<>();
	private static final Map<String, Containers.Factory> SCREENS = new TreeMap<>();
	private static boolean built;

	private ContentRegistry() {
	}

	private static final class DeclaredBlock<T extends Block> implements BlockEntry<T> {
		final String name;
		final Content.BlockFactory<T> factory;
		T block;
		Block standIn;
		int standInMeta;
		Item itemStandIn;
		boolean noItem;
		Content.ItemFactory<? extends Item> itemFactory;

		DeclaredBlock(String name, Content.BlockFactory<T> factory) {
			this.name = name;
			this.factory = factory;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public T get() {
			if (block == null) throw new IllegalStateException(name + " isn't built yet: ask from init");
			return block;
		}

		@Override
		public BlockEntry<T> standIn(Block b) {
			return standIn(b, 0);
		}

		@Override
		public BlockEntry<T> standIn(Block b, int meta) {
			standIn = b;
			standInMeta = meta;
			return this;
		}

		@Override
		public BlockEntry<T> itemStandIn(Item item) {
			itemStandIn = item;
			return this;
		}

		@Override
		public BlockEntry<T> noItem() {
			noItem = true;
			return this;
		}

		@Override
		public BlockEntry<T> item(Content.ItemFactory<? extends Item> factory) {
			itemFactory = factory;
			return this;
		}
	}

	private static final class DeclaredItem<T extends Item> implements ItemEntry<T> {
		final String name;
		final Content.ItemFactory<T> factory;
		T item;
		Item standIn = Item.STICK;
		int standInDamage;

		DeclaredItem(String name, Content.ItemFactory<T> factory) {
			this.name = name;
			this.factory = factory;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public T get() {
			if (item == null) throw new IllegalStateException(name + " isn't built yet: ask from init");
			return item;
		}

		@Override
		public ItemEntry<T> standIn(Item i) {
			return standIn(i, 0);
		}

		@Override
		public ItemEntry<T> standIn(Item i, int damage) {
			standIn = i;
			standInDamage = damage;
			return this;
		}
	}

	/** One mod's view of the declarations. */
	public static final class ModContent implements Content {
		private final String mod;

		public ModContent(String mod) {
			this.mod = mod;
		}

		@Override
		public String modId() {
			return mod;
		}

		private String full(String name) {
			if (name.indexOf(':') >= 0) throw new IllegalArgumentException(mod + ": a name of your own has no namespace: " + name);
			return mod + ":" + name;
		}

		@Override
		public <T extends Block> BlockEntry<T> block(String name, BlockFactory<T> factory) {
			DeclaredBlock<T> d = new DeclaredBlock<>(full(name), factory);
			if (BLOCKS.putIfAbsent(d.name, d) != null) throw new IllegalStateException("The block " + d.name + " is declared twice");
			return d;
		}

		@Override
		public <T extends Item> ItemEntry<T> item(String name, ItemFactory<T> factory) {
			DeclaredItem<T> d = new DeclaredItem<>(full(name), factory);
			if (ITEMS.putIfAbsent(d.name, d) != null) throw new IllegalStateException("The item " + d.name + " is declared twice");
			return d;
		}

		@Override
		public void blockEntity(String name, Class<? extends BlockEntity> type) {
			if (BLOCK_ENTITIES.putIfAbsent(full(name), type) != null) throw new IllegalStateException("The block entity " + full(name) + " is declared twice");
		}

		@Override
		public EntityEntry entity(String name, Class<? extends Entity> type) {
			return EntityContent.declare(full(name), type);
		}

		@Override
		public void screen(String name, Containers.Factory factory) {
			if (SCREENS.putIfAbsent(full(name), factory) != null) throw new IllegalStateException("The screen " + full(name) + " is declared twice");
		}

		@Override
		public void family(String... names) {
			String[] full = new String[names.length];
			for (int i = 0; i < names.length; i++) full[i] = full(names[i]);
			FAMILIES.add(full);
		}

		@Override
		public void alias(String oldName, String name) {
			if (oldName.indexOf(':') < 0) throw new IllegalArgumentException(mod + ": an alias is a whole name: " + oldName);
			ALIASES.put(oldName, full(name));
		}

		@Override
		public Optional<String> ref(String fullName) {
			return BLOCKS.containsKey(fullName) || ITEMS.containsKey(fullName) ? Optional.of(fullName) : Optional.empty();
		}

		@Override
		public List<String> blockNames() {
			return new ArrayList<>(BLOCKS.keySet());
		}

		@Override
		public List<String> itemNames() {
			return new ArrayList<>(ITEMS.keySet());
		}
	}

	public static boolean any() {
		return !BLOCKS.isEmpty() || !ITEMS.isEmpty() || !BLOCK_ENTITIES.isEmpty() || !SCREENS.isEmpty() || EntityContent.any();
	}

	private static String order(String name) {
		if (SALT.isEmpty()) return name;
		try {
			StringBuilder key = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest((SALT + name).getBytes(StandardCharsets.UTF_8))) key.append(b & 0xFF).append('.');
			return key.toString();
		} catch (NoSuchAlgorithmException e) {
			return name;
		}
	}

	private static int saltShift() {
		return SALT.isEmpty() ? 0 : Math.abs(SALT.hashCode() % 97) + 1;
	}

	/** Blocks in numbering order: by name, a family as one at its first name's place. */
	private static List<String> blockOrder() {
		Map<String, String[]> familyOf = new HashMap<>();
		for (String[] f : FAMILIES) {
			for (String n : f) {
				if (!BLOCKS.containsKey(n)) throw new IllegalStateException("The family member " + n + " isn't declared");
				familyOf.put(n, f);
			}
		}
		List<String> sorted = new ArrayList<>(BLOCKS.keySet());
		sorted.sort(Comparator.comparing(ContentRegistry::order));
		Set<String> out = new LinkedHashSet<>();
		for (String n : sorted) {
			String[] f = familyOf.get(n);
			if (f == null) out.add(n);
			else for (String m : f) out.add(m);
		}
		return new ArrayList<>(out);
	}

	private static boolean taken(int id) {
		return Item.ITEMS[id] != null || (id < Block.BLOCKS.length && Block.BLOCKS[id] != null);
	}

	/** The next free run of {@code length} ids from {@code from}. */
	private static int free(int from, int length, int limit) {
		for (int id = from; id + length <= limit; id++) {
			boolean clear = true;
			// A low byte of 0 reads as "nothing" in beta's block break event.
			for (int i = 0; i < length && clear; i++) clear = !taken(id + i) && (limit == ITEM_LIMIT || (id + i & 0xFF) != 0);
			if (clear) return id;
		}
		throw new IllegalStateException("Out of ids at " + limit);
	}

	/** Numbers everything and builds it: blocks, then their items, then items. Runs once, after every mod has declared. */
	public static void build() {
		if (built) return;
		built = true;
		if (!any()) return;
		if (Block.BLOCKS.length < BLOCK_LIMIT) {
			throw new IllegalStateException("A mod declared content, but none says so in its fabric.mod.json (\"custom\": {\"faithful\": {\"content\": true}})");
		}
		Set<String> inFamily = new java.util.HashSet<>();
		Map<String, String[]> families = new HashMap<>();
		for (String[] f : FAMILIES) {
			families.put(f[0], f);
			for (String n : f) inFamily.add(n);
		}
		int next = FIRST_BLOCK + saltShift();
		List<DeclaredBlock<?>> built = new ArrayList<>();
		List<String> order = blockOrder();
		for (int i = 0; i < order.size(); ) {
			String[] family = families.get(order.get(i));
			int length = family != null ? family.length : 1;
			int id = free(next, length, BLOCK_LIMIT);
			for (int k = 0; k < length; k++) {
				String name = order.get(i + k);
				DeclaredBlock<?> d = BLOCKS.get(name);
				make(d, id + k);
				built.add(d);
			}
			next = id + length;
			i += length;
		}
		for (DeclaredBlock<?> d : built) finishItem(d);
		next = FIRST_ITEM + saltShift();
		List<String> items = new ArrayList<>(ITEMS.keySet());
		items.sort(Comparator.comparing(ContentRegistry::order));
		for (String name : items) {
			DeclaredItem<?> d = ITEMS.get(name);
			int id = free(next, 1, ITEM_LIMIT);
			next = id + 1;
			makeItem(d, id);
		}
		for (Map.Entry<String, Class<? extends BlockEntity>> e : BLOCK_ENTITIES.entrySet()) BlockEntityInvoker.faithful$create(e.getValue(), e.getKey());
		EntityContent.build();
		aliases();
		Atlases.finish();
		ChunkData.section("overlay", new ChunkData.Section() {
			@Override
			public void load(Chunk chunk, NbtCompound faithful) {
				Overlay.load(chunk, faithful);
			}

			@Override
			public void save(Chunk chunk, NbtCompound faithful) {
				Overlay.save(chunk, faithful);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static void aliases() {
		for (Map.Entry<String, String> a : ALIASES.entrySet()) {
			Class<? extends BlockEntity> type = BLOCK_ENTITIES.get(a.getValue());
			if (type != null) BlockEntityInvoker.faithful$idToClass().put(a.getKey(), type);
			EntityContent.alias(a.getKey(), a.getValue());
		}
	}

	private static <T extends Block> void make(DeclaredBlock<T> d, int id) {
		String name = d.name;
		SpecImpl spec = new SpecImpl(id, modOf(name), "block");
		Atlases.beginBlock();
		T block = d.factory.create(spec);
		if (block.id != id) throw new IllegalStateException("The block " + name + " wasn't made with the id it was given");
		d.block = block;
		Block standIn = d.standIn != null ? d.standIn : defaultStandIn(block);
		if (standIn != null && (standIn.id >= 256 || Block.BLOCKS_WITH_ENTITY[standIn.id])) {
			throw new IllegalStateException("The stand-in of " + name + " must be a vanilla block with no block entity");
		}
		MOD_BLOCK[id] = true;
		BLOCK_ATLAS[id] = block.getTexture(0, 0) >> 8;
		STAND_IN[id] = (standIn == null ? 0 : standIn.id) | d.standInMeta << 8;
		BLOCK_NAME[id] = name;
		BLOCK_IDS.put(name, id);
		block.setTranslationKey(name.replace(':', '.'));
	}

	private static Block defaultStandIn(Block block) {
		return block.isOpaque() && block.material != Material.AIR ? Block.STONE : null;
	}

	private static void finishItem(DeclaredBlock<?> d) {
		int id = d.block.id;
		if (!d.noItem) {
			if (d.itemFactory != null) d.itemFactory.create(new SpecImpl(id - 256, modOf(d.name), "item"));
			else new BlockItem(id - 256);
			if (Item.ITEMS[id] == null) throw new IllegalStateException("The block item of " + d.name + " wasn't made with the id it was given");
			int standIn = STAND_IN[id] & 0xFF;
			Item item = d.itemStandIn != null ? d.itemStandIn : standIn != 0 && Item.ITEMS[standIn] != null ? Item.ITEMS[standIn] : Item.STICK;
			if (item.id >= FIRST_BLOCK) throw new IllegalStateException("The item stand-in of " + d.name + " must be a vanilla item");
			// A stand-in stack of id 0 crashes the furnace and the renderers: it has to be a real item.
			ITEM_STAND_IN.put(id, item.id | (d.itemStandIn != null ? 0 : (STAND_IN[id] >> 8)) << 16);
			ITEM_NAME.put(id, d.name);
			ITEM_IDS.put(d.name, id);
		}
		((BlockInit) d.block).faithful$init();
	}

	private static <T extends Item> void makeItem(DeclaredItem<T> d, int id) {
		T item = d.factory.create(new SpecImpl(id - 256, modOf(d.name), "item"));
		if (item.id != id) throw new IllegalStateException("The item " + d.name + " wasn't made with the id it was given");
		if (d.standIn.id >= FIRST_BLOCK) throw new IllegalStateException("The stand-in of " + d.name + " must be a vanilla item");
		d.item = item;
		item.setTranslationKey(d.name.replace(':', '.'));
		ITEM_STAND_IN.put(id, d.standIn.id | d.standInDamage << 16);
		ITEM_NAME.put(id, d.name);
		ITEM_IDS.put(d.name, id);
	}

	/** Implemented by the game's Block, for its protected {@code init}. */
	public interface BlockInit {
		void faithful$init();
	}

	private static String modOf(String name) {
		return name.substring(0, name.indexOf(':'));
	}

	private static final class SpecImpl implements Content.Spec {
		private final int id;
		private final String mod;
		private final String kind;

		SpecImpl(int id, String mod, String kind) {
			this.id = id;
			this.mod = mod;
			this.kind = kind;
		}

		@Override
		public int id() {
			return id;
		}

		@Override
		public int texture(String name) {
			return Atlases.tile(mod, kind, name);
		}

		@Override
		public int vanillaTile(int tile) {
			return Atlases.vanillaTile(tile);
		}

	}

	public static boolean modBlock(int id) {
		return id >= FIRST_BLOCK && id < BLOCK_LIMIT && MOD_BLOCK[id];
	}

	/** Whether an item id is a block's item: vanilla's {@code < 256}, and ours. */
	public static boolean isBlockItem(int id) {
		return id < 256 || modBlock(id);
	}

	public static Containers.Factory screen(String name) {
		return SCREENS.get(name);
	}

	public static String blockName(int id) {
		return id >= 0 && id < BLOCK_LIMIT ? BLOCK_NAME[id] : null;
	}

	/** The block of a name in a save, through aliases; -1 if there's none here. */
	public static int blockId(String name) {
		Integer id = BLOCK_IDS.get(ALIASES.getOrDefault(name, name));
		return id == null ? -1 : id;
	}

	public static String itemName(int id) {
		return id >= FIRST_BLOCK ? ITEM_NAME.get(id) : null;
	}

	public static int itemId(String name) {
		Integer id = ITEM_IDS.get(ALIASES.getOrDefault(name, name));
		return id == null ? -1 : id;
	}

	public static List<String> blockNames() {
		return new ArrayList<>(new TreeMap<>(BLOCK_IDS).keySet());
	}

	public static List<String> itemNames() {
		return new ArrayList<>(new TreeMap<>(ITEM_IDS).keySet());
	}

	public static int itemStandInId(int id) {
		return ITEM_STAND_IN.get(id) & 0xFFFF;
	}

	public static int itemStandInDamage(int id) {
		return ITEM_STAND_IN.get(id) >>> 16;
	}

	/** Lines for the match hash: everything both sides must number alike. */
	public static List<String> hashLines() {
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Integer> e : new TreeMap<>(BLOCK_IDS).entrySet()) lines.add("block:" + e.getKey() + ":" + e.getValue());
		for (Map.Entry<String, Integer> e : new TreeMap<>(ITEM_IDS).entrySet()) lines.add("item:" + e.getKey() + ":" + e.getValue());
		for (String name : BLOCK_ENTITIES.keySet()) lines.add("block_entity:" + name);
		EntityContent.hashLines(lines);
		for (String name : SCREENS.keySet()) lines.add("screen:" + name);
		return lines;
	}
}
