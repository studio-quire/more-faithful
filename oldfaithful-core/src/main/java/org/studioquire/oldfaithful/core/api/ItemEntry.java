package org.studioquire.oldfaithful.core.api;

import net.minecraft.item.Item;

/** A declared item. */
public interface ItemEntry<T extends Item> {
	String name();

	/** The item, from {@code init} on. */
	T get();

	/** What a vanilla reader sees; must be a vanilla item. Default: a stick. */
	ItemEntry<T> standIn(Item item);

	ItemEntry<T> standIn(Item item, int damage);
}
