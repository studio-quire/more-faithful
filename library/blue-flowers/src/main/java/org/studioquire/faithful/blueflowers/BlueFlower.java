package org.studioquire.faithful.blueflowers;

import net.minecraft.block.PlantBlock;

/** Beta's own plant block with the rose's and dandelion's settings, so it stands, drops, burns and breaks as they do. */
public final class BlueFlower extends PlantBlock {
	BlueFlower(int id, int texture) {
		super(id, texture);
		setHardness(0.0F);
		setSoundGroup(DIRT_SOUND_GROUP);
	}
}
