package org.studioquire.oldfaithful.core.api;

import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;

/**
 * Declaring a mod's content. Names first, for every mod, because vanilla's constructors need numbers and numbers come from
 * every name; the core then numbers them and calls the factories in order. A name is forever once a mod ships.
 */
public interface Content {
	/** What a factory is told: the number to pass to vanilla's constructor, and textures by name. */
	interface Spec {
		/** For a block: its id. For an item: what {@code new Item(...)} takes, 256 below its id, as vanilla's own do. */
		int id();

		/** A tile from {@code assets/<modid>/textures/block/<name>.png} or {@code item/}: the texture index to use. */
		int texture(String name);

		/** A vanilla terrain tile, copied into this block's atlas: a block's tiles all live in one. */
		int vanillaTile(int tile);
	}

	interface BlockFactory<T extends Block> {
		T create(Spec spec);
	}

	interface ItemFactory<T extends Item> {
		T create(Spec spec);
	}

	String modId();

	<T extends Block> BlockEntry<T> block(String name, BlockFactory<T> factory);

	<T extends Item> ItemEntry<T> item(String name, ItemFactory<T> factory);

	/**
	 * A block entity class, saved as {@code <mod id>:<name>}; it needs a public constructor with no arguments. Make the block a
	 * {@code BlockWithEntity} as vanilla's are. For data its clients see, implement {@link SyncedBlockEntity}.
	 */
	void blockEntity(String name, Class<? extends BlockEntity> type);

	/**
	 * An entity class, saved as {@code <mod id>:<name>}; it needs a public constructor taking a {@code World}. It reaches clients by name, living or
	 * not, and a subclass of a vanilla class (a mod's boat) is treated as what it is. {@link EntitySpawnData} sends more with the spawn. On the client,
	 * {@code Renderers.entity} draws it; with no renderer it's a white box, as vanilla's fallback has it.
	 */
	EntityEntry entity(String name, Class<? extends Entity> type);

	/** An inventory screen. {@code name} is short here, like every name a mod declares; {@link Containers#open} and {@code ContainerScreens.register} take the full {@code my-mod:my_screen}. */
	void screen(String name, Containers.Factory factory);

	/**
	 * Blocks that need consecutive ids, in this order (a liquid's flowing and still forms: vanilla's use {@code id + 1}).
	 * The names are the mod's own; declare each with {@link #block} too.
	 */
	void family(String... names);

	/** An old name of one of this mod's things (a block, item, block entity or entity), in any namespace: saves that hold it load as {@code name}. */
	void alias(String oldName, String name);

	/**
	 * A sprite of this mod's for an item that isn't its own, from {@code assets/<modid>/textures/item/<name>.png}: the texture index, for a vanilla item the mod draws differently (through a mixin on
	 * that item's {@code getTextureId}, in the mod's {@code client} list). Whatever draws an item's icon takes it as it takes a mod item's: a slot, the hand, a dropped stack. Ask in {@code content}.
	 */
	int itemTexture(String name);

	/** Another mod's thing, if it's installed: resolves after {@code content}, so hold on to it and ask later. */
	Optional<String> ref(String fullName);

	/** Every block name declared so far, for {@code lateContent}: a slab for every stone. */
	List<String> blockNames();

	/** Every item name declared so far. */
	List<String> itemNames();
}
