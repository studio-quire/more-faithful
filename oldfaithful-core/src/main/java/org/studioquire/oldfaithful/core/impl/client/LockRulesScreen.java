package org.studioquire.oldfaithful.core.impl.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * Asks once before locking a world's rules, on beta's own screen for deleting a world. Its second line is too long for one line of a
 * small window, so it's left empty and drawn here, wrapped, where beta draws it.
 */
final class LockRulesScreen extends ConfirmScreen {
	private static final int WRAP = 300;

	private final Screen page;

	LockRulesScreen(Screen page) {
		super(page, RuleSettings.CONFIRM, "", RuleSettings.CONFIRM_YES, RuleSettings.CONFIRM_NO, 0);
		this.page = page;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == 0 && !Rules.lock()) System.out.println("[faithful-core] WARN This world's rules couldn't be locked; the log above says why");
		minecraft.setScreen(page);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		super.render(mouseX, mouseY, delta);
		int y = 90;
		for (String line : wrap(RuleSettings.CONFIRM_WARNING)) {
			drawCenteredTextWithShadow(textRenderer, line, width / 2, y, 0xFFFFFF);
			y += 10;
		}
	}

	private List<String> wrap(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (line.length() > 0 && textRenderer.getWidth(line + " " + word) > WRAP) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0) line.append(' ');
			line.append(word);
		}
		if (line.length() > 0) lines.add(line.toString());
		return lines;
	}
}
