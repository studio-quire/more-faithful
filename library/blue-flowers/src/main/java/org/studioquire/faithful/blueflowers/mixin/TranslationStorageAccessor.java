package org.studioquire.faithful.blueflowers.mixin;

import java.util.Properties;

import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The game's names, to rename beta's yellow flower while a switch says so. The core merges each mod's lang file once, at start-up, and has no way to change a name later,
 * which a switch that takes effect at once needs. An accessor changes no method of the game's, so nothing else reaching here is in its way.
 */
@Mixin(TranslationStorage.class)
public interface TranslationStorageAccessor {
	@Accessor("translations")
	Properties blueflowers$translations();
}
