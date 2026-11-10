package org.studioquire.oldfaithful.core.mixin.client;

import java.util.List;
import java.util.Map;

import net.minecraft.client.sound.SoundEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEntry.class)
public interface SoundEntryAccessor {
	@Accessor("weightedSoundSet")
	Map faithful$sets();

	@Accessor("loadedSounds")
	List faithful$all();
}
