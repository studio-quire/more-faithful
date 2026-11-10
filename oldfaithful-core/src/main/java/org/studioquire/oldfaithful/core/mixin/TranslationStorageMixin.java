package org.studioquire.oldfaithful.core.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each mod's {@code assets/<modid>/lang/en_US.lang} joins the game's own: beta has one language. */
@Mixin(TranslationStorage.class)
abstract class TranslationStorageMixin {
	@Shadow
	private Properties translations;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void faithful$modLang(CallbackInfo ci) {
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			Optional<Path> lang = mod.findPath("assets/" + mod.getMetadata().getId() + "/lang/en_US.lang");
			if (!lang.isPresent() || !Files.isRegularFile(lang.get())) continue;
			try (InputStream in = Files.newInputStream(lang.get())) {
				translations.load(in);
			} catch (IOException e) {
				System.out.println("[faithful-core] WARN Could not read " + mod.getMetadata().getId() + "'s lang file: " + e);
			}
		}
	}
}
