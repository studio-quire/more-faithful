package org.studioquire.faithful.golems.client;

import org.studioquire.oldfaithful.core.api.client.Input;

/** The press of the right button that made an exchange with a golem, until it's let go: none of beta's repeats of it, and nothing used from the hand (the mixin on {@code Minecraft}). */
public final class HeldClick {
	public static boolean latched;

	private HeldClick() {
	}

	static void init() {
		// Heard before the game takes the event: a release ends it, and so does a fresh press, which is a click of its own.
		Input.onMouse(e -> {
			if (e.button == 1) latched = false;
		});
	}
}
