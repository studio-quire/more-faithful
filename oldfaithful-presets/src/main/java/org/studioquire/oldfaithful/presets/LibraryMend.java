package org.studioquire.oldfaithful.presets;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Reads the library's folder at each start, so a list that's gone stale is mended whether or not anyone opens the settings screen, and a pack's list is
 * put back to the install's preset and changes (a new version's comes with its default preset's).
 */
public final class LibraryMend implements PreLaunchEntrypoint {
	@Override
	public void onPreLaunch() {
		new LibrarySelection(FabricLoader.getInstance().getGameDir()).reconcile();
	}
}
