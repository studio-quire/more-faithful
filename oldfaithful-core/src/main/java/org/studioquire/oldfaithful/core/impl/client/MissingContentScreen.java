package org.studioquire.oldfaithful.core.impl.client;

import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.studioquire.oldfaithful.core.impl.SeenMods;

/** A world that had a mod's content, opened without the mod: what's missing, what happens, and the way on. */
public final class MissingContentScreen extends Screen {
	private final Screen parent;
	private final Path world;
	private final List<String> gone;
	private final Runnable play;

	public MissingContentScreen(Screen parent, Path world, List<String> gone, Runnable play) {
		this.parent = parent;
		this.world = world;
		this.gone = gone;
		this.play = play;
	}

	@Override
	public void init() {
		buttons.add(new ButtonWidget(0, width / 2 - 100, height / 6 + 96, "Make a Backup and Play"));
		buttons.add(new ButtonWidget(1, width / 2 - 100, height / 6 + 120, "Play"));
		buttons.add(new ButtonWidget(2, width / 2 - 100, height / 6 + 144, "Cancel"));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == 2) {
			minecraft.setScreen(parent);
			return;
		}
		if (button.id == 0 && SeenMods.backup(world) == null) return;
		SeenMods.accept(world, gone);
		play.run();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		drawCenteredTextWithShadow(textRenderer, "This world used mods that aren't here", width / 2, 20, 0xFFFFFF);
		drawCenteredTextWithShadow(textRenderer, String.join(", ", gone), width / 2, 40, 0xFFFF80);
		drawCenteredTextWithShadow(textRenderer, "Their blocks show as stand-ins and come back with the mods.", width / 2, 60, 0xA0A0A0);
		drawCenteredTextWithShadow(textRenderer, "Their items, creatures and machines are lost as the world saves.", width / 2, 72, 0xA0A0A0);
		super.render(mouseX, mouseY, delta);
	}
}
