package org.studioquire.oldfaithful.core.impl.client;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.studioquire.oldfaithful.core.impl.Hello;
import org.studioquire.oldfaithful.settings.LibrarySelection;

/**
 * Why a server was refused, in beta's look: what must match and doesn't, by the mods' names (ids and versions go to the log). With the library pack,
 * {@code Match This Server's Mods} writes the list for the next
 * launch: only ids come from the server, and only jars already in the folder are switched.
 */
final class MismatchScreen extends Screen {
	private static final int BACK = 0, USE_SET = 1;
	private static final int MAX_LINES = 12;
	private List<String> lines;
	/** What must match there; null when it can't be used, or has been. */
	private List<String> theirSet;
	private String title = "Your mods don't match this server's";

	MismatchScreen(List<String> lines, List<String> theirSet) {
		this.lines = lines;
		this.theirSet = theirSet;
	}

	@Override
	public void init() {
		buttons.clear();
		buttons.add(new ButtonWidget(BACK, width / 2 - 100, height / 6 + 168, "Back to title screen"));
		if (theirSet != null && !new LibrarySelection(FabricLoader.getInstance().getGameDir()).entries().isEmpty()) {
			buttons.add(new ButtonWidget(USE_SET, width / 2 - 100, height / 6 + 144, "Match This Server's Mods"));
		}
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK) minecraft.setScreen(new TitleScreen());
		if (button.id == USE_SET) useTheirSet();
	}

	private void useTheirSet() {
		LibrarySelection.Change change = new LibrarySelection(FabricLoader.getInstance().getGameDir()).useServerSet(theirSet);
		theirSet = null;
		lines = new ArrayList<>();
		title = change.saved ? "Saved: restart the game and join again" : "Couldn't save that: " + log() + " says why";
		for (String name : change.on) lines.add("On: " + name);
		for (String name : change.off) lines.add("Off: " + name);
		for (String id : change.absent) lines.add("You don't have " + modName(id) + ", so it still won't match");
		init();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
		int y = 48;
		for (int i = 0; i < lines.size() && i < MAX_LINES; i++, y += 11) drawCenteredTextWithShadow(textRenderer, lines.get(i), width / 2, y, 0xA0A0A0);
		if (lines.size() > MAX_LINES) drawCenteredTextWithShadow(textRenderer, "and " + (lines.size() - MAX_LINES) + " more, all in " + log(), width / 2, y, 0xA0A0A0);
		super.render(mouseX, mouseY, delta);
	}

	/** A mod's name: the loaded mod's, or a jar's in the library folder, or its id made into words (as for a mod that has none). */
	static String modName(String id) {
		FabricLoader loader = FabricLoader.getInstance();
		String loaded = loader.getModContainer(id).map(c -> c.getMetadata().getName()).orElse(id);
		if (!loaded.equals(id)) return loaded;
		for (LibrarySelection.Entry e : new LibrarySelection(loader.getGameDir()).entries()) {
			if (e.id.equals(id)) return e.name;
		}
		return Hello.titled(id);
	}

	/** Where a player finds what's printed: Pure Faithful's hardening keeps the game's output in a file of its own. */
	private static String log() {
		return FabricLoader.getInstance().isModLoaded("purefaithful-hardening") ? "logs/purefaithful/latest.log" : "the launcher's log";
	}
}
