package org.studioquire.oldfaithful.core.api;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/** A declared block: settle its details here, then {@link #get} once {@code content} is over. */
public interface BlockEntry<T extends Block> {
	String name();

	/** The block, from {@code init} on, and in an item's factory: blocks are built before items. */
	T get();

	/** What a vanilla reader, or this game without the mod, sees here. A vanilla block with no block entity. */
	BlockEntry<T> standIn(Block block);

	BlockEntry<T> standIn(Block block, int meta);

	/** What its item is in a vanilla reader's eyes; defaults to the block's own stand-in, or a stick. */
	BlockEntry<T> itemStandIn(Item item);

	/** No block item: lit variants, crops, anything only placed by something else. */
	BlockEntry<T> noItem();

	/** A block item of its own in place of a plain one. */
	BlockEntry<T> item(Content.ItemFactory<? extends Item> factory);
}
