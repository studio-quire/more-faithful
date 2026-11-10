package org.studioquire.faithful.staticbad;

import net.minecraft.block.FarmlandBlock;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** A common mixin on a client class, and an {@code @Overwrite}. */
@Mixin({FarmlandBlock.class, InGameHud.class})
abstract class OverwritingMixin {
	@Overwrite
	public boolean isOpaque() {
		return true;
	}
}
