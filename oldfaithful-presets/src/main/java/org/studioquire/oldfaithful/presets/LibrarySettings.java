package org.studioquire.oldfaithful.presets;

import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.oldfaithful.options.settings.PageButton;
import org.studioquire.oldfaithful.options.settings.Setting;
import org.studioquire.oldfaithful.options.settings.SettingsProvider;
import org.studioquire.oldfaithful.options.settings.SettingsRegistry;

/**
 * A library pack's mods on Faithful Settings' screen: each mod's page has its on and off, for the next launch, and a pack with presets has a Preset button
 * at the top of the Mods menu, stepping through them a click at a time. Nothing shows without the folder.
 */
public final class LibrarySettings implements SettingsProvider {
	@Override
	public void register(SettingsRegistry settings) {
		LibrarySelection library = new LibrarySelection(FabricLoader.getInstance().getGameDir());
		if (library.entries().isEmpty()) return;
		settings.onReload(library::forget);
		if (!library.presets().isEmpty()) settings.addButtonToModsMenu(presetButton(library, settings), "Choose a preset, turn mods on or off, and change their settings.");
		for (LibrarySelection.Entry e : library.entries()) {
			settings.mod(e.id, e.name);
			settings.modSwitch(e.id, Setting.stored("library." + e.id, "Enabled", e.description, false, new Setting.Store() {
				private String also;

				@Override
				public String read() {
					return String.valueOf(library.isOn(e));
				}

				@Override
				public boolean write(String value) {
					LibrarySelection.Change change = library.set(e, Boolean.parseBoolean(value));
					also = change.also();
					return change.saved;
				}

				@Override
				public String said() {
					return also;
				}
			}).running(() -> String.valueOf(FabricLoader.getInstance().isModLoaded(e.id))).lockedWhen(() -> library.heldBy(e, settings::heldReason)));
		}
	}

	/**
	 * "Preset: Simple", or "Preset: Custom" once a mod is turned on or off beyond it, with the asterisk of a change waiting for a restart. A click chooses
	 * the next, or from Custom the install's preset as it comes, for the next launch.
	 */
	private static PageButton presetButton(LibrarySelection library, SettingsRegistry settings) {
		return new PageButton(() -> {
			boolean waiting = library.waiting(id -> FabricLoader.getInstance().isModLoaded(id));
			return "Preset: " + (library.custom() ? "Custom" : library.current().name) + (waiting ? "*" : "");
		}, () -> {
			LibrarySelection.Preset at = library.current();
			return library.custom() ? at.name + " with mods you've turned on or off yourself. A click goes back to " + at.name + " as it comes." : at.hint;
		}, () -> {
			String[] why = {null};
			return library.next(settings::heldReason, why) != null ? null : why[0] != null ? why[0] : "There's no other preset.";
		}, page -> {
			LibrarySelection.Preset next = library.next(settings::heldReason, new String[1]);
			if (next != null) library.choose(next);
			return null;
		});
	}
}
