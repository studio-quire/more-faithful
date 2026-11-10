package org.studioquire.oldfaithful.core.impl;

import java.util.Random;

import net.minecraft.item.ItemStack;

/** Implemented by the game's DungeonFeature, for {@code Worldgen}'s dungeon lists. */
public interface DungeonFeatureInvoker {
	ItemStack faithful$chestItem(Random random);

	String faithful$monster(Random random);
}
