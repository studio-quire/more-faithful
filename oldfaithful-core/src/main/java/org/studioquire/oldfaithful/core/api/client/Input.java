package org.studioquire.oldfaithful.core.api.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.studioquire.oldfaithful.core.api.Cancellable;
import org.studioquire.oldfaithful.core.api.Listeners;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;

/**
 * Raw keyboard and mouse events, on a client, in or out of a screen, as the game takes them from LWJGL. Vetoes: a cancelled event
 * never reaches the game or the screen, and every listener still hears it. For a bindable key, {@link Keys} is the kinder way.
 */
public final class Input {
	public static final class KeyEvent extends Cancellable {
		public final int code;
		public final char character;
		public final boolean pressed;
		/** The screen it would go to, or null in the game. */
		public final Screen screen;

		KeyEvent(int code, char character, boolean pressed, Screen screen) {
			this.code = code;
			this.character = character;
			this.pressed = pressed;
			this.screen = screen;
		}
	}

	public static final class MouseEvent extends Cancellable {
		/** -1 for a move or a wheel turn. */
		public final int button;
		public final boolean pressed;
		public final int dx, dy, wheel;
		public final Screen screen;

		MouseEvent(int button, boolean pressed, int dx, int dy, int wheel, Screen screen) {
			this.button = button;
			this.pressed = pressed;
			this.dx = dx;
			this.dy = dy;
			this.wheel = wheel;
			this.screen = screen;
		}
	}

	private static final Listeners<Consumer<KeyEvent>> KEY = new Listeners<>("Input.onKey");
	private static final Listeners<Consumer<MouseEvent>> MOUSE = new Listeners<>("Input.onMouse");

	private Input() {
	}

	public static void onKey(Consumer<KeyEvent> listener) {
		KEY.add(listener);
	}

	public static void onMouse(Consumer<MouseEvent> listener) {
		MOUSE.add(listener);
	}

	private static Screen screen() {
		Minecraft minecraft = ClientNet.minecraft;
		return minecraft == null ? null : minecraft.currentScreen;
	}

	/** The core's, around {@code Keyboard.next}: the next event the game may have. */
	public static boolean nextKey(BooleanSupplier next) {
		if (KEY.isEmpty() && !Keys.hasPressListeners()) return next.getAsBoolean();
		while (next.getAsBoolean()) {
			KeyEvent event = new KeyEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), Keyboard.getEventKeyState(), screen());
			KEY.each(l -> l.accept(event));
			if (event.cancelled()) continue;
			if (event.pressed && event.screen == null) Keys.pressed(event.code);
			return true;
		}
		return false;
	}

	/** The core's, around {@code Mouse.next}. */
	public static boolean nextMouse(BooleanSupplier next) {
		if (MOUSE.isEmpty()) return next.getAsBoolean();
		while (next.getAsBoolean()) {
			MouseEvent event = new MouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState(), Mouse.getEventDX(), Mouse.getEventDY(), Mouse.getEventDWheel(), screen());
			MOUSE.each(l -> l.accept(event));
			if (!event.cancelled()) return true;
		}
		return false;
	}
}
