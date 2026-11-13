package org.studioquire.oldfaithful.presets;

import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.oldfaithful.options.settings.ModList;

/** The pack's list as Faithful Core's refusal screen asks for it (Match This Server's Mods): the folder read afresh at each ask, as a page is. */
public final class PackModList implements ModList {
	private static LibrarySelection library() {
		return new LibrarySelection(FabricLoader.getInstance().getGameDir());
	}

	@Override
	public boolean any() {
		return !library().entries().isEmpty();
	}

	@Override
	public String name(String id) {
		for (LibrarySelection.Entry e : library().entries()) {
			if (e.id.equals(id)) return e.name;
		}
		return null;
	}

	@Override
	public Matched match(Iterable<String> mustMatch) {
		LibrarySelection.Change change = library().useServerSet(mustMatch);
		return new Matched(change.saved, change.on, change.off, change.absent);
	}
}
