package org.studioquire.oldfaithful.core.impl.client;

import java.nio.file.Path;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.studioquire.oldfaithful.core.impl.SeenMods;

/**
 * Make a Backup and Play at work. The zip is made on a thread of its own, as a big world takes seconds and the window would stop answering
 * meanwhile; once it's made the world opens, as it did straight from the button. Cancel (or Esc) stops it between two files and takes the
 * unfinished zip away, back to the warning; closing the game does the same, so no half zip is left to pass for a backup. If it couldn't be
 * made, it's back to the warning, as before, with why in the log.
 */
final class BackupScreen extends Screen implements SeenMods.Progress {
	private final Screen back;
	private final Runnable play;
	private final Thread worker;
	private final Thread atExit;
	private volatile boolean stop;
	private volatile long bytes;
	private volatile boolean finished;
	private volatile String made;
	private boolean answered;
	private ButtonWidget cancel;

	BackupScreen(Screen back, Path world, Runnable play) {
		this.back = back;
		this.play = play;
		worker = new Thread(() -> {
			try {
				made = SeenMods.backup(world, this);
			} finally {
				// A throw (the folder walk's own) is printed by the thread's end, and it's back to the warning, never a screen left waiting.
				finished = true;
			}
		}, "Faithful Core backup");
		worker.setDaemon(true);
		atExit = new Thread(() -> {
			stop = true;
			try {
				worker.join(10_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "Faithful Core backup, at exit");
		Runtime.getRuntime().addShutdownHook(atExit);
		worker.start();
	}

	@Override
	public void done(long bytes) {
		this.bytes = bytes;
	}

	@Override
	public boolean stopped() {
		return stop;
	}

	@Override
	public void init() {
		// Not where Make a Backup and Play was, so a double click there doesn't cancel it.
		cancel = new ButtonWidget(0, width / 2 - 100, height / 6 + 144, "Cancel");
		cancel.active = !stop;
		buttons.add(cancel);
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == 0) cancel();
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == 1) cancel();
	}

	private void cancel() {
		stop = true;
		if (cancel != null) cancel.active = false;
	}

	@Override
	public void tick() {
		if (!finished || answered) return;
		answered = true;
		try {
			Runtime.getRuntime().removeShutdownHook(atExit);
		} catch (IllegalStateException e) {
			// The game is closing.
		}
		// Cancel after the backup's last check still goes back; the zip it finished is a whole backup and stays.
		if (made != null && !stop) play.run();
		else minecraft.setScreen(back);
	}

	/** Put aside by anything else: given up, as for Cancel. */
	@Override
	public void removed() {
		if (!finished) stop = true;
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		drawCenteredTextWithShadow(textRenderer, stop ? "Stopping the backup" : "Making a backup of the world", width / 2, height / 2 - 30, 0xFFFFFF);
		drawCenteredTextWithShadow(textRenderer, String.format(java.util.Locale.ROOT, "%.1f MB so far", bytes / 1048576.0), width / 2, height / 2 - 14, 0xA0A0A0);
		super.render(mouseX, mouseY, delta);
	}
}
