package org.studioquire.oldfaithful.core.mixin.content.server;

import net.minecraft.server.command.ServerCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** {@code give} takes a number, which now shifts with the mods installed, so a name like {@code my-mod:my_item} works too. */
@Mixin(ServerCommandHandler.class)
abstract class GiveByNameMixin {
	@ModifyVariable(method = "executeCommand", at = @At("STORE"), ordinal = 0)
	private String faithful$giveByName(String command) {
		String[] words = command.split(" ");
		if (words.length < 3 || !words[0].equalsIgnoreCase("give") || words[2].indexOf(':') < 0) return command;
		int id = ContentRegistry.itemId(words[2]);
		if (id < 0) return command;
		words[2] = String.valueOf(id);
		return String.join(" ", words);
	}
}
